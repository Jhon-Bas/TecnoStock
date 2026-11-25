package pe.tecnostock.modules.customers.repository;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.Customer;
import pe.tecnostock.util.SqlUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de clientes. */
public class CustomerRepository {

    /** Inserta el registro mediante una sentencia parametrizada. */
    public void insert(Customer customer) throws SQLException {
        String sql = """
            INSERT INTO clientes
                (documento, nombre, telefono, correo, direccion, activo)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.getDocument());
            statement.setString(2, customer.getName());
            statement.setString(3, customer.getPhone());
            statement.setString(4, customer.getEmail());
            statement.setString(5, customer.getAddress());
            statement.setInt(6, customer.isActive() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    /** Actualiza los datos del cliente seleccionado. */
    public boolean update(Customer customer) throws SQLException {
        String sql = """
            UPDATE clientes SET documento=?, nombre=?, telefono=?,
                correo=?, direccion=?, updated_at=CURRENT_TIMESTAMP
            WHERE id=?
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.getDocument());
            statement.setString(2, customer.getName());
            statement.setString(3, customer.getPhone());
            statement.setString(4, customer.getEmail());
            statement.setString(5, customer.getAddress());
            statement.setInt(6, customer.getId());
            return statement.executeUpdate() > 0;
        }
    }

    /** Actualiza el estado sin eliminar el registro. */
    public boolean changeStatus(int id, boolean active) throws SQLException {
        String sql = """
            UPDATE clientes SET activo=?, updated_at=CURRENT_TIMESTAMP WHERE id=?
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, active ? 1 : 0);
            statement.setInt(2, id);
            return statement.executeUpdate() > 0;
        }
    }

    /** Comprueba si el documento pertenece a otro cliente. */
    public boolean documentExists(String document, int excludedId) throws SQLException {
        String sql = "SELECT 1 FROM clientes WHERE documento=? AND id<>?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, document);
            statement.setInt(2, excludedId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    /** Busca registros según el texto y el estado solicitado. */
    public List<Customer> search(String text, boolean includeInactive, int limit)
            throws SQLException {
        String sql = """
            SELECT id, documento, nombre, telefono, correo, direccion, activo
            FROM clientes
            WHERE (?=1 OR activo=1)
              AND (documento LIKE ? ESCAPE '\\' OR nombre LIKE ? ESCAPE '\\')
            ORDER BY nombre COLLATE NOCASE
            LIMIT ?
            """;
        String pattern = "%" + SqlUtil.escapeLike(text == null ? "" : text.trim()) + "%";
        List<Customer> list = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, includeInactive ? 1 : 0);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setInt(4, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) list.add(mapRow(result));
            }
        }
        return list;
    }

    /** Consulta un cliente por su identificador. */
    public Optional<Customer> findById(int id) throws SQLException {
        String sql = """
            SELECT id, documento, nombre, telefono, correo, direccion, activo
            FROM clientes WHERE id=?
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return Optional.of(mapRow(result));
            }
        }
        return Optional.empty();
    }

    /** Convierte una fila SQL en una entidad del dominio. */
    private Customer mapRow(ResultSet result) throws SQLException {
        return new Customer(result.getInt("id"), result.getString("documento"),
                result.getString("nombre"), result.getString("telefono"),
                result.getString("correo"), result.getString("direccion"),
                result.getInt("activo") == 1);
    }
}