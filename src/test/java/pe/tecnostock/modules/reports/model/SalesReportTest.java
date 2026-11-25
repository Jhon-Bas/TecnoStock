package pe.tecnostock.modules.reports.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SalesReportTest {
    @Test
    void shouldKeepImmutableBreakdowns() {
        List<SalesReport.DailySale> days = new ArrayList<>();
        days.add(new SalesReport.DailySale(LocalDate.of(2026, 11, 24), 2, new BigDecimal("80.00")));
        SalesReport report = new SalesReport(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 2, new BigDecimal("100.00"),
                new BigDecimal("20.00"), new BigDecimal("80.00"), days, List.of());

        days.clear();
        assertEquals(1, report.dailySales().size());
        assertThrows(UnsupportedOperationException.class,
                () -> report.dailySales().clear());
    }
}