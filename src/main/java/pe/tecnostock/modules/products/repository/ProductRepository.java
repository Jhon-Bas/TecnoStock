package pe.tecnostock.modules.products.repository;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.Category;
import pe.tecnostock.model.Product;
import pe.tecnostock.util.SqlUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a las tablas productos y categorias en SQLite.
 *
 * Solo contiene SQL. Los precios se guardan en céntimos (enteros)
 * y se convierten a BigDecimal al leerlos. Actualizar un producto
 * no modifica su stock: las existencias cambian solo mediante
 * movimientos de inventario.
 */
public class ProductRepository {

    public void insert(Product product) throws SQLException {

        String sql = """
            INSERT INTO productos (
                codigo, nombre, descripcion, categoria_id,
                precio_compra_centimos, precio_venta_centimos,
                stock_actual, stock_minimo, activo
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, product.getCode());
            statement.setString(2, product.getName());
            statement.setString(3, product.getDescription());
            statement.setInt(4, product.getCategory().id());
            statement.setLong(5, toCents(product.getPurchasePrice()));
            statement.setLong(6, toCents(product.getSalePrice()));
            statement.setInt(7, product.getCurrentStock());
            statement.setInt(8, product.getMinimumStock());
            statement.setInt(9, product.isActive() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    /** @return true si existía un producto con ese id */
    public boolean update(Product product) throws SQLException {

        String sql = """
            UPDATE productos
            SET codigo = ?, nombre = ?, descripcion = ?,
                categoria_id = ?, precio_compra_centimos = ?,
                precio_venta_centimos = ?, stock_minimo = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, product.getCode());
            statement.setString(2, product.getName());
            statement.setString(3, product.getDescription());
            statement.setInt(4, product.getCategory().id());
            statement.setLong(5, toCents(product.getPurchasePrice()));
            statement.setLong(6, toCents(product.getSalePrice()));
            statement.setInt(7, product.getMinimumStock());
            statement.setInt(8, product.getId());

            return statement.executeUpdate() > 0;
        }
    }

    /** @return true si existía un producto con ese id */
    public boolean changeStatus(int id, boolean active)
            throws SQLException {

        String sql = """
            UPDATE productos
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
     * @param excluirId id de un producto que se ignora (el que se está
     *                  editando), o 0 para no excluir ninguno
     */
    public boolean codeExists(String code, int excludedId)
            throws SQLException {

        String sql = "SELECT 1 FROM productos WHERE codigo = ? AND id <> ?";

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
     *
     * @param soloStockBajo true para devolver solo los productos cuyo
     *                      stock es igual o menor al stock mínimo
     */
    public List<Product> search(
            String text,
            boolean includeInactive,
            boolean lowStockOnly,
            int limit
    ) throws SQLException {

        String sql = """
            SELECT p.id, p.codigo, p.nombre, p.descripcion,
                   p.precio_compra_centimos, p.precio_venta_centimos,
                   p.stock_actual, p.stock_minimo, p.activo,
                   c.id AS categoria_id, c.nombre AS categoria_nombre
            FROM productos p
            JOIN categorias c ON c.id = p.categoria_id
            WHERE (? = 1 OR p.activo = 1)
              AND (? = 0 OR p.stock_actual <= p.stock_minimo)
              AND (p.codigo LIKE ? ESCAPE '\\'
                   OR p.nombre LIKE ? ESCAPE '\\')
            ORDER BY p.nombre COLLATE NOCASE
            LIMIT ?
            """;

        String pattern = "%" + SqlUtil.escapeLike(text) + "%";
        List<Product> products = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, includeInactive ? 1 : 0);
            statement.setInt(2, lowStockOnly ? 1 : 0);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            statement.setInt(5, limit);

            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    products.add(readProduct(row));
                }
            }
        }

        return products;
    }

    private static Product readProduct(ResultSet row)
            throws SQLException {

        return new Product(
                row.getInt("id"),
                row.getString("codigo"),
                row.getString("nombre"),
                row.getString("descripcion"),
                toSoles(row.getLong("precio_venta_centimos")),
                toSoles(row.getLong("precio_compra_centimos")),
                row.getInt("stock_actual"),
                row.getInt("stock_minimo"),
                new Category(
                        row.getInt("categoria_id"),
                        row.getString("categoria_nombre")),
                row.getInt("activo") == 1
        );
    }

    public List<Category> listCategories() throws SQLException {

        List<Category> categories = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery(
                     "SELECT id, nombre FROM categorias "
                             + "ORDER BY nombre COLLATE NOCASE")) {

            while (row.next()) {
                categories.add(new Category(
                        row.getInt("id"), row.getString("nombre")));
            }
        }

        return categories;
    }

    private static long toCents(BigDecimal soles) {
        return soles.movePointRight(2).longValueExact();
    }

    private static BigDecimal toSoles(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}