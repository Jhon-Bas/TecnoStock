package pe.tecnostock.modules.reports.repository;

import pe.tecnostock.config.Database;
import pe.tecnostock.modules.reports.model.SalesReport;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas de lectura dedicadas a reportes. No contiene reglas de negocio. */
public final class ReportRepository implements ReportQuery {

    @Override
    public SalesReport getSales(LocalDate fromDate, LocalDate toDate) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            Summary summary = querySummary(connection, fromDate, toDate);
            return new SalesReport(fromDate, toDate, summary.quantity(), summary.subtotal(),
                    summary.discounts(), summary.total(), queryByDay(connection, fromDate, toDate),
                    queryItems(connection, fromDate, toDate));
        }
    }

    /** Consulta los totales de las ventas emitidas. */
    private Summary querySummary(Connection connection, LocalDate fromDate, LocalDate toDate)
            throws SQLException {
        String sql = """
                SELECT COUNT(*) cantidad,
                       COALESCE(SUM(subtotal_centimos), 0) subtotal,
                       COALESCE(SUM(descuento_centimos), 0) descuentos,
                       COALESCE(SUM(total_centimos), 0) total
                FROM ventas
                WHERE date(fecha) BETWEEN date(?) AND date(?) AND estado = 'EMITIDA'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fromDate.toString()); ps.setString(2, toDate.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Summary(rs.getInt("cantidad"), toSoles(rs.getLong("subtotal")),
                        toSoles(rs.getLong("descuentos")), toSoles(rs.getLong("total")));
            }
        }
    }

    /** Agrupa las ventas por fecha. */
    private List<SalesReport.DailySale> queryByDay(
            Connection connection, LocalDate fromDate, LocalDate toDate) throws SQLException {
        String sql = """
                SELECT date(fecha) dia, COUNT(*) cantidad,
                       COALESCE(SUM(total_centimos), 0) total
                FROM ventas
                WHERE date(fecha) BETWEEN date(?) AND date(?) AND estado = 'EMITIDA'
                GROUP BY date(fecha) ORDER BY date(fecha)
                """;
        List<SalesReport.DailySale> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fromDate.toString()); ps.setString(2, toDate.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new SalesReport.DailySale(
                        LocalDate.parse(rs.getString("dia")), rs.getInt("cantidad"),
                        toSoles(rs.getLong("total"))));
            }
        }
        return result;
    }

    /** Consulta los artículos más vendidos. */
    private List<SalesReport.TopItem> queryItems(
            Connection connection, LocalDate fromDate, LocalDate toDate) throws SQLException {
        String sql = """
                SELECT d.tipo_item || ' - ' || d.nombre_item nombre, SUM(d.cantidad) cantidad,
                       SUM(d.subtotal_centimos) importe
                FROM detalle_venta d JOIN ventas v ON v.id = d.venta_id
                WHERE date(v.fecha) BETWEEN date(?) AND date(?) AND v.estado = 'EMITIDA'
                GROUP BY d.tipo_item, d.codigo_item, d.nombre_item ORDER BY cantidad DESC, importe DESC LIMIT 10
                """;
        List<SalesReport.TopItem> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fromDate.toString()); ps.setString(2, toDate.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new SalesReport.TopItem(
                        rs.getString("nombre"), rs.getInt("cantidad"),
                        toSoles(rs.getLong("importe"))));
            }
        }
        return result;
    }

    /** Convierte céntimos a soles. */
    private static BigDecimal toSoles(long cents) { return BigDecimal.valueOf(cents, 2); }
    /** Inicializa el componente y sus dependencias. */
    private record Summary(int quantity, BigDecimal subtotal, BigDecimal discounts, BigDecimal total) {}
}