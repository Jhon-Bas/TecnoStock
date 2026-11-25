package pe.tecnostock.modules.reports.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Resultado inmutable de un reporte de ventas para un rango de fechas. */
public record SalesReport(
        LocalDate fromDate,
        LocalDate toDate,
        int saleCount,
        BigDecimal subtotal,
        BigDecimal discounts,
        BigDecimal total,
        List<DailySale> dailySales,
        List<TopItem> topItems) {

    public SalesReport {
        dailySales = List.copyOf(dailySales);
        topItems = List.copyOf(topItems);
    }

    /** Inicializa el componente y sus dependencias. */
    public record DailySale(LocalDate date, int quantity, BigDecimal total) {}
    /** Inicializa el componente y sus dependencias. */
    public record TopItem(String name, int quantity, BigDecimal amount) {}
}