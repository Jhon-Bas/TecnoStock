package pe.tecnostock.modules.inventory.repository;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.InventoryMovement;
import pe.tecnostock.model.MovementType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Acceso a las existencias y al historial de movimientos en SQLite.
 *
 * Las operaciones que cambian datos reciben la Connection: asÃ­ el
 * llamador puede ejecutarlas dentro de su propia transacciÃ³n, por
 * ejemplo la que registra una venta. Solo contiene SQL; las reglas de
 * stock las aplica InventarioService.
 */
public class InventoryRepository {

    /**
     * Stock de un producto activo.
     *
     * @return vacÃ­o si el producto no existe o estÃ¡ inactivo
     */
    public OptionalInt readActiveStock(Connection connection, int productId)
            throws SQLException {

        String sql = """
            SELECT stock_actual FROM productos
            WHERE id = ? AND activo = 1
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, productId);

            try (ResultSet row = statement.executeQuery()) {
                return row.next()
                        ? OptionalInt.of(row.getInt("stock_actual"))
                        : OptionalInt.empty();
            }
        }
    }

    /**
     * Cambia el stock solo si todavÃ­a vale lo que se leyÃ³, para no
     * pisar un cambio hecho por otra operaciÃ³n en el medio.
     *
     * @return false si el stock ya habÃ­a cambiado
     */
    public boolean updateStock(
            Connection connection,
            int productId,
            int previousStock,
            int newStock
    ) throws SQLException {

        String sql = """
            UPDATE productos
            SET stock_actual = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND stock_actual = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, newStock);
            statement.setInt(2, productId);
            statement.setInt(3, previousStock);

            return statement.executeUpdate() > 0;
        }
    }

    /** Registra el movimiento dentro de la transacciÃ³n recibida. */
    public void insertMovement(
            Connection connection,
            int productId,
            MovementType type,
            int quantity,
            int previousStock,
            int resultingStock,
            int userId,
            String reason
    ) throws SQLException {

        String sql = """
            INSERT INTO movimientos_inventario (
                producto_id, tipo, cantidad, stock_anterior,
                stock_resultante, usuario_id, motivo
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, productId);
            statement.setString(2, type.databaseValue());
            statement.setInt(3, quantity);
            statement.setInt(4, previousStock);
            statement.setInt(5, resultingStock);
            statement.setInt(6, userId);
            statement.setString(7, reason);
            statement.executeUpdate();
        }
    }

    /**
     * Ãšltimos movimientos, del mÃ¡s reciente al mÃ¡s antiguo.
     *
     * @param productoId 0 para todos los productos
     */
    public List<InventoryMovement> listMovements(
            int productId,
            int limit
    ) throws SQLException {

        String sql = """
            SELECT m.id, p.codigo, p.nombre, m.tipo, m.cantidad,
                   m.stock_anterior, m.stock_resultante, m.fecha,
                   u.username, m.motivo
            FROM movimientos_inventario m
            JOIN productos p ON p.id = m.producto_id
            JOIN usuarios u ON u.id = m.usuario_id
            WHERE (? = 0 OR m.producto_id = ?)
            ORDER BY m.id DESC
            LIMIT ?
            """;

        List<InventoryMovement> movements = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, productId);
            statement.setInt(2, productId);
            statement.setInt(3, limit);

            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    movements.add(new InventoryMovement(
                            row.getInt("id"),
                            row.getString("codigo"),
                            row.getString("nombre"),
                            MovementType.fromDatabase(row.getString("tipo")),
                            row.getInt("cantidad"),
                            row.getInt("stock_anterior"),
                            row.getInt("stock_resultante"),
                            LocalDateTime.parse(
                                    row.getString("fecha").replace(' ', 'T')),
                            row.getString("username"),
                            row.getString("motivo")
                    ));
                }
            }
        }

        return movements;
    }
}