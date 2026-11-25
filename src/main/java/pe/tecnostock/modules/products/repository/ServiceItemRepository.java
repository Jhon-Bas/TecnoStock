package pe.tecnostock.modules.products.repository;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.ServiceItem;
import pe.tecnostock.util.SqlUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla servicios en SQLite.
 *
 * Solo contiene SQL. Los precios se guardan en céntimos (enteros).
 */
public class ServiceItemRepository {

    public void insert(ServiceItem service) throws SQLException {

        String sql = """
            INSERT INTO servicios
                (codigo, nombre, descripcion,
                 precio_venta_centimos, activo)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, service.getCode());
            statement.setString(2, service.getName());
            statement.setString(3, service.getDescription());
            statement.setLong(4, toCents(service.getSalePrice()));
            statement.setInt(5, service.isActive() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    /** @return true si existía un servicio con ese id */
    public boolean update(ServiceItem service) throws SQLException {

        String sql = """
            UPDATE servicios
            SET codigo = ?, nombre = ?, descripcion = ?,
                precio_venta_centimos = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, service.getCode());
            statement.setString(2, service.getName());
            statement.setString(3, service.getDescription());
            statement.setLong(4, toCents(service.getSalePrice()));
            statement.setInt(5, service.getId());

            return statement.executeUpdate() > 0;
        }
    }

    /** @return true si existía un servicio con ese id */
    public boolean changeStatus(int id, boolean active)
            throws SQLException {

        String sql = """
            UPDATE servicios
            SET activo = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, active ? 1 : 0);
            statement.setInt(2, id);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * @param excluirId id de un servicio que se ignora (el que se está
     *                  editando), o 0 para no excluir ninguno
     */
    public boolean codeExists(String code, int excludedId)
            throws SQLException {

        String sql = "SELECT 1 FROM servicios WHERE codigo = ? AND id <> ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, code);
            statement.setInt(2, excludedId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /**
     * Busca por parte del código o del nombre, sin distinguir
     * mayúsculas. Un texto vacío devuelve todos, hasta el límite.
     */
    public List<ServiceItem> search(
            String text,
            boolean includeInactive,
            int limit
    ) throws SQLException {

        String sql = """
            SELECT id, codigo, nombre, descripcion,
                   precio_venta_centimos, activo
            FROM servicios
            WHERE (? = 1 OR activo = 1)
              AND (codigo LIKE ? ESCAPE '\\'
                   OR nombre LIKE ? ESCAPE '\\')
            ORDER BY nombre COLLATE NOCASE
            LIMIT ?
            """;

        String pattern = "%" + SqlUtil.escapeLike(text) + "%";
        List<ServiceItem> services = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, includeInactive ? 1 : 0);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setInt(4, limit);

            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    services.add(new ServiceItem(
                            row.getInt("id"),
                            row.getString("codigo"),
                            row.getString("nombre"),
                            row.getString("descripcion"),
                            toSoles(row.getLong("precio_venta_centimos")),
                            row.getInt("activo") == 1
                    ));
                }
            }
        }

        return services;
    }

    private static long toCents(BigDecimal soles) {
        return soles.movePointRight(2).longValueExact();
    }

    private static BigDecimal toSoles(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}