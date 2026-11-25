package pe.tecnostock.modules.sales.facade;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import pe.tecnostock.config.TemporaryDatabase;
import pe.tecnostock.model.Customer;
import pe.tecnostock.modules.customers.repository.CustomerRepository;
import pe.tecnostock.security.TestUsers;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica la coordinación de los servicios desde la fachada comercial. */
@ResourceLock("tecnostock.db.path")
class SalesFacadeTest {
    @TempDir Path directory;
    private String previous;
    private TestUsers users;
    private SalesFacade facade;

    /** Prepara una base independiente para cada prueba. */
    @BeforeEach void setUp() throws SQLException {
        previous = TemporaryDatabase.use(directory);
        users = TestUsers.create();
        facade = new SalesFacade();
    }

    /** Restablece la configuración de la base original. */
    @AfterEach void restore() {
        TemporaryDatabase.restore(previous);
    }

    /** Comprueba que la preparación reúna clientes y catálogo. */
    @Test void shouldPrepareSale() throws SQLException {
        new CustomerRepository().insert(Customer.create(
                "87654321", "Cliente de prueba", null, null, null));
        var data = facade.prepareSale(users.employee);
        assertTrue(data.customers().stream().anyMatch(
                customer -> customer.getDocument().equals("87654321")));
        assertNotNull(data.catalog());
    }

    /** Verifica que la fachada no permita eludir la autorización. */
    @Test void shouldRejectMissingSession() {
        assertThrows(SecurityException.class, () -> facade.prepareSale(null));
    }

    /** Conserva la validación de ventas sin artículos. */
    @Test void shouldRejectEmptySale() {
        assertThrows(IllegalArgumentException.class, () -> facade.registerSale(
                users.employee, 1, List.of(), BigDecimal.ZERO));
    }
}