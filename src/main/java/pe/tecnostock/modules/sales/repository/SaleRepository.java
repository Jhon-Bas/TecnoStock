package pe.tecnostock.modules.sales.repository;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.SaleDetail;
import pe.tecnostock.model.ItemType;
import pe.tecnostock.model.Sale;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de ventas y sus detalles.
 * Los importes se almacenan como céntimos enteros y se exponen como soles.
 */
public class SaleRepository {

    public int insertSale(Connection connection, String receiptNumber,
            int customerId, int userId, long subtotalCents,
            long discountCents, long totalCents) throws SQLException {

        String sql = """
                INSERT INTO ventas (numero_boleta, cliente_id, usuario_id,
                    subtotal_centimos, descuento_centimos, total_centimos)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, receiptNumber);
            ps.setInt(2, customerId);
            ps.setInt(3, userId);
            ps.setLong(4, subtotalCents);
            ps.setLong(5, discountCents);
            ps.setLong(6, totalCents);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        // Compatibilidad con controladores SQLite que no devuelven generated keys.
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("No se pudo obtener el ID de la venta registrada.");
    }

    public void insertDetail(Connection connection, int saleId, ItemType itemType,
            int itemId, String itemCode, String itemName, int quantity,
            long unitPriceCents, long subtotalCents) throws SQLException {

        String sql = """
                INSERT INTO detalle_venta (venta_id, tipo_item, item_id,
                    codigo_item, nombre_item, cantidad,
                    precio_unitario_centimos, subtotal_centimos)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            ps.setString(2, itemType == ItemType.PRODUCT ? "PRODUCTO" : "SERVICIO");
            ps.setInt(3, itemId);
            ps.setString(4, itemCode);
            ps.setString(5, itemName);
            ps.setInt(6, quantity);
            ps.setLong(7, unitPriceCents);
            ps.setLong(8, subtotalCents);
            ps.executeUpdate();
        }
    }

    /** Debe llamarse dentro de la transacción de registro. */
    public int nextSequence(Connection connection) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM ventas";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    public List<Sale> list(Integer userIdFilter, int limit) throws SQLException {
        String base = """
                SELECT v.id, v.numero_boleta, v.cliente_id,
                       c.nombre AS cliente_nombre, c.documento AS cliente_documento,
                       v.usuario_id, v.subtotal_centimos, v.descuento_centimos,
                       v.total_centimos, v.estado, v.fecha
                FROM ventas v
                JOIN clientes c ON c.id = v.cliente_id
                """;
        String sql = userIdFilter == null
                ? base + " ORDER BY v.id DESC LIMIT ?"
                : base + " WHERE v.usuario_id = ? ORDER BY v.id DESC LIMIT ?";

        List<Sale> sales = new ArrayList<>();
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (userIdFilter == null) ps.setInt(1, limit);
            else {
                ps.setInt(1, userIdFilter);
                ps.setInt(2, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    sales.add(readSale(rs, listDetails(cn, id)));
                }
            }
        }
        return sales;
    }

    public Optional<Sale> findById(int saleId) throws SQLException {
        try (Connection cn = Database.getConnection()) {
            return findById(cn, saleId);
        }
    }

    /** Consulta con la misma conexión para poder leer datos no confirmados aún. */
    public Optional<Sale> findById(Connection cn, int saleId) throws SQLException {
        String sql = """
                SELECT v.id, v.numero_boleta, v.cliente_id,
                       c.nombre AS cliente_nombre, c.documento AS cliente_documento,
                       v.usuario_id, v.subtotal_centimos, v.descuento_centimos,
                       v.total_centimos, v.estado, v.fecha
                FROM ventas v
                JOIN clientes c ON c.id = v.cliente_id
                WHERE v.id = ?
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(
                        readSale(rs, listDetails(cn, saleId)));
            }
        }
        return Optional.empty();
    }

    private List<SaleDetail> listDetails(Connection cn, int saleId)
            throws SQLException {
        String sql = """
                SELECT id, venta_id, tipo_item, item_id, codigo_item, nombre_item,
                       cantidad, precio_unitario_centimos, subtotal_centimos
                FROM detalle_venta WHERE venta_id = ? ORDER BY id ASC
                """;
        List<SaleDetail> details = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    details.add(new SaleDetail(
                            rs.getInt("id"), rs.getInt("venta_id"),
                            "PRODUCTO".equals(rs.getString("tipo_item")) ? ItemType.PRODUCT : ItemType.SERVICE,
                            rs.getInt("item_id"), rs.getString("codigo_item"),
                            rs.getString("nombre_item"), rs.getInt("cantidad"),
                            toSoles(rs.getLong("precio_unitario_centimos")),
                            toSoles(rs.getLong("subtotal_centimos"))
                    ));
                }
            }
        }
        return details;
    }

    private Sale readSale(ResultSet rs, List<SaleDetail> details)
            throws SQLException {
        return new Sale(
                rs.getInt("id"), rs.getString("numero_boleta"),
                rs.getInt("cliente_id"), rs.getString("cliente_nombre"),
                rs.getString("cliente_documento"), rs.getInt("usuario_id"),
                toSoles(rs.getLong("subtotal_centimos")),
                toSoles(rs.getLong("descuento_centimos")),
                toSoles(rs.getLong("total_centimos")),
                rs.getString("estado"), rs.getString("fecha"), details
        );
    }

    private static BigDecimal toSoles(long toCents) {
        return BigDecimal.valueOf(toCents, 2);
    }
}