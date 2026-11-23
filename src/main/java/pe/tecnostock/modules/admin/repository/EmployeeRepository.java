
package pe.tecnostock.modules.admin.repository;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la persistencia de empleados en SQLite.
 *
 * Utiliza consultas paginadas, transacciones y
 * desactivación lógica para conservar los registros.
 */
public class EmployeeRepository {

    /**
     * Datos que se muestran en el listado administrativo.
     */
    public record EmployeeRow(
            int id,
            String firstName,
            String lastName,
            String document,
            String username,
            String role,
            boolean active,
            String createdAt,
            String createdBy,
            String updatedAt
    ) {}

    /**
     * Resultado paginado de una búsqueda.
     */
    public record EmployeePage(
            List<EmployeeRow> employees,
            int total
    ) {}

    /**
     * Registra empleado y cuenta en una transacción.
     */
    public void create(
            String firstName,
            String lastName,
            String document,
            String username,
            Role role,
            String hash,
            String salt,
            int creatorId
    ) throws SQLException {

        try (Connection connection = Database.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int employeeId;

                String employeeSql = """
                    INSERT INTO empleados
                    (nombres, apellidos, documento, cargo)
                    VALUES (?, ?, ?, ?)
                    """;

                try (PreparedStatement statement =
                        connection.prepareStatement(
                                employeeSql,
                                Statement.RETURN_GENERATED_KEYS
                        )) {

                    statement.setString(1, firstName);
                    statement.setString(2, lastName);
                    statement.setString(3, document);
                    statement.setString(4, role.name());
                    statement.executeUpdate();

                    try (ResultSet keys =
                            statement.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "No se obtuvo el ID del empleado."
                            );
                        }

                        employeeId = keys.getInt(1);
                    }
                }

                String userSql = """
                    INSERT INTO usuarios (
                        empleado_id, username,
                        password_hash, password_salt,
                        rol, activo, created_by
                    )
                    VALUES (?, ?, ?, ?, ?, 1, ?)
                    """;

                try (PreparedStatement statement =
                        connection.prepareStatement(userSql)) {

                    statement.setInt(1, employeeId);
                    statement.setString(2, username);
                    statement.setString(3, hash);
                    statement.setString(4, salt);
                    statement.setString(5, role.name());
                    statement.setInt(6, creatorId);
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Busca empleados usando paginación SQL.
     *
     * @param search Texto de búsqueda.
     * @param page Número de página desde cero.
     * @param pageSize Registros por página.
     */
    public EmployeePage findPage(
            String search,
            int page,
            int pageSize
    ) throws SQLException {

        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException(
                    "Parámetros de paginación inválidos."
            );
        }

        String filter = "%" + search.toLowerCase() + "%";

        String where = """
            WHERE (
                LOWER(e.nombres) LIKE ?
                OR LOWER(e.apellidos) LIKE ?
                OR e.documento LIKE ?
                OR LOWER(u.username) LIKE ?
            )
            """;

        String countSql = """
            SELECT COUNT(*)
            FROM empleados e
            JOIN usuarios u ON u.empleado_id = e.id
            """ + where;

        String listSql = """
            SELECT
                e.id, e.nombres, e.apellidos, e.documento,
                u.username, u.rol, u.activo,
                u.fecha_creacion, u.updated_at,
                creator.username AS creator_name
            FROM empleados e
            JOIN usuarios u ON u.empleado_id = e.id
            LEFT JOIN usuarios creator
                ON creator.id = u.created_by
            """ + where + """
            ORDER BY e.id DESC
            LIMIT ? OFFSET ?
            """;

        try (Connection connection = Database.getConnection()) {

            int total;

            try (PreparedStatement statement =
                    connection.prepareStatement(countSql)) {

                setFilters(statement, filter);

                try (ResultSet result = statement.executeQuery()) {
                    total = result.getInt(1);
                }
            }

            List<EmployeeRow> employees = new ArrayList<>();

            try (PreparedStatement statement =
                    connection.prepareStatement(listSql)) {

                setFilters(statement, filter);
                statement.setInt(5, pageSize);
                statement.setInt(6, page * pageSize);

                try (ResultSet result = statement.executeQuery()) {

                    while (result.next()) {

                        String creator = result.getString(
                                "creator_name"
                        );

                        employees.add(new EmployeeRow(
                                result.getInt("id"),
                                result.getString("nombres"),
                                result.getString("apellidos"),
                                result.getString("documento"),
                                result.getString("username"),
                                result.getString("rol"),
                                result.getInt("activo") == 1,
                                result.getString("fecha_creacion"),
                                creator == null
                                    ? "No registrado" : creator,
                                result.getString("updated_at")
                        ));
                    }
                }
            }

            return new EmployeePage(employees, total);
        }
    }

    /**
     * Asigna los parámetros de búsqueda de manera segura.
     */
    private void setFilters(
            PreparedStatement statement,
            String filter
    ) throws SQLException {

        for (int i = 1; i <= 4; i++) {
            statement.setString(i, filter);
        }
    }

    /**
     * Modifica los datos personales y el rol.
     *
     * No permite modificar cuentas administradoras
     * desde este procedimiento.
     */
    public void update(
            int employeeId,
            String firstName,
            String lastName,
            String document,
            String username,
            Role role,
            int editorId
    ) throws SQLException {

        try (Connection connection = Database.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Evita modificar administradores por esta vía.
                requireManagedAccount(connection, employeeId);

                try (PreparedStatement statement =
                        connection.prepareStatement("""
                            UPDATE empleados
                            SET nombres = ?,
                                apellidos = ?,
                                documento = ?,
                                cargo = ?
                            WHERE id = ?
                            """)) {

                    statement.setString(1, firstName);
                    statement.setString(2, lastName);
                    statement.setString(3, document);
                    statement.setString(4, role.name());
                    statement.setInt(5, employeeId);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement =
                        connection.prepareStatement("""
                            UPDATE usuarios
                            SET username = ?,
                                rol = ?,
                                updated_at = CURRENT_TIMESTAMP,
                                updated_by = ?
                            WHERE empleado_id = ?
                            """)) {

                    statement.setString(1, username);
                    statement.setString(2, role.name());
                    statement.setInt(3, editorId);
                    statement.setInt(4, employeeId);
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Activa o desactiva una cuenta sin eliminarla.
     */
    public void setActive(
            int employeeId,
            boolean active,
            int editorId
    ) throws SQLException {

        try (Connection connection = Database.getConnection()) {

            connection.setAutoCommit(false);

            try {
                requireManagedAccount(connection, employeeId);

                try (PreparedStatement statement =
                        connection.prepareStatement("""
                            UPDATE usuarios
                            SET activo = ?,
                                updated_at = CURRENT_TIMESTAMP,
                                updated_by = ?
                            WHERE empleado_id = ?
                            """)) {

                    statement.setInt(1, active ? 1 : 0);
                    statement.setInt(2, editorId);
                    statement.setInt(3, employeeId);
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Comprueba que el empleado exista y que su cuenta
     * no sea administradora antes de modificarla.
     */
    private void requireManagedAccount(
            Connection connection,
            int employeeId
    ) throws SQLException {

        try (PreparedStatement statement =
                connection.prepareStatement("""
                    SELECT rol FROM usuarios
                    WHERE empleado_id = ?
                    """)) {

            statement.setInt(1, employeeId);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "Empleado no encontrado."
                    );
                }

                if ("ADMIN".equals(result.getString("rol"))) {
                    throw new SecurityException(
                            "No se permite modificar una cuenta ADMIN."
                    );
                }
            }
        }
    }
}
