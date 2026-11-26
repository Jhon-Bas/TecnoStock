package pe.tecnostock.modules.reports.service;

import org.junit.jupiter.api.Test;
import pe.tecnostock.modules.reports.repository.ReportQuery;
import pe.tecnostock.service.AuthorizationService;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {
    private final ReportQuery query = (fromDate, toDate) -> {
        fail("No debe consultar el repositorio si el rango no es válido.");
        return null;
    };
    private final ReportService service = new ReportService(query, new AuthorizationService());

    @Test
    void shouldRejectReversedRange() {
        assertThrows(IllegalArgumentException.class, () -> service.generateSales(null,
                LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 24)));
    }

    @Test
    void shouldRejectFutureDate() {
        assertThrows(IllegalArgumentException.class, () -> service.generateSales(null,
                LocalDate.now(), LocalDate.now().plusDays(1)));
    }
}