package pe.tecnostock.modules.reports.proxy;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import pe.tecnostock.config.TemporaryDatabase;
import pe.tecnostock.security.TestUsers;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.modules.reports.model.SalesReport;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Comprueba que el proxy protege el servicio antes de delegar. */
@ResourceLock("tecnostock.db.path")
class ReportAccessProxyTest {
    @TempDir Path directory;
    private String previous;
    private TestUsers users;
    private ReportAccessProxy proxy;
    private final AtomicInteger calls = new AtomicInteger();
    private final LocalDate date = LocalDate.now();
    private final SalesReport expected = new SalesReport(date, date, 0,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());

    /** Prepara una base temporal y un servicio que registra las delegaciones. */
    @BeforeEach void setUp() throws SQLException {
        previous = TemporaryDatabase.use(directory);
        users = TestUsers.create();
        proxy = new ReportAccessProxy((user, from, to) -> {
            calls.incrementAndGet();
            return expected;
        }, new AuthorizationService());
    }

    /** Restaura la configuración de la base de datos. */
    @AfterEach void restore() { TemporaryDatabase.restore(previous); }

    /** Permite la consulta al gerente y conserva el resultado. */
    @Test void shouldDelegateForManager() throws SQLException {
        assertSame(expected, proxy.generateSales(users.manager, date, date));
        assertEquals(1, calls.get());
    }

    /** Impide que un empleado alcance el servicio real. */
    @Test void shouldRejectEmployee() {
        assertThrows(SecurityException.class, () -> proxy.generateSales(users.employee, date, date));
        assertEquals(0, calls.get());
    }

    /** Impide consultas sin una sesión autenticada. */
    @Test void shouldRejectMissingSession() {
        assertThrows(SecurityException.class, () -> proxy.generateSales(null, date, date));
        assertEquals(0, calls.get());
    }
}