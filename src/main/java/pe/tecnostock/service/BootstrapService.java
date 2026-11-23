
package pe.tecnostock.service;

import pe.tecnostock.config.Database;
import pe.tecnostock.util.PasswordUtil;

import java.sql.*;
import java.util.Arrays;

/**
 * Gestiona la configuración inicial de TecnoStock.
 *
 * Permite registrar al primer administrador y su
 * empleado asociado cuando no existen usuarios.
 */
public class BootstrapService {

    /**
     * Comprueba si existe al menos una cuenta registrada.
     *
     * @return true si ya existe algún usuario.
     */
    public boolean hasUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            return result.next() && result.getInt(1) > 0;
        }
    }

    /**
     * Registra al primer administrador y su empleado.
     * Ambas operaciones se guardan en una transacción
     * para evitar registros incompletos.
     *
     * @param firstName Nombres del administrador.
     * @param lastName Apellidos del administrador.
     * @param document Documento de identidad.
     * @param username Nombre de acceso.
     * @param password Contraseña de acceso.
     */
    public void createInitialAdmin(
            String firstName,
            String lastName,
            String document,
            String username,
            char[] password) throws SQLException {

        try {
            if (firstName == null || firstName.isBlank()
                    || lastName == null || lastName.isBlank()
                    || document == null
                    || !document.matches("\\d{8}")
                    || username == null
                    || !username.matches("[a-zA-Z0-9_]{4,30}")
                    || password == null
                    || password.length < 10) {

                throw new IllegalArgumentException(
                        "Verifica los datos. El DNI debe tener 8 "
                        + "dígitos, el usuario entre 4 y 30 "
                        + "caracteres válidos y la contraseña "
                        + "al menos 10 caracteres."
                );
            }

            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(
                    password, salt
            );

            try (Connection connection =
                         Database.getConnection()) {

                connection.setAutoCommit(false);

                try {
                    // Impide crear otro administrador inicial.
                    try (PreparedStatement check =
                            connection.prepareStatement(
                                "SELECT COUNT(*) FROM usuarios"
                            );
                         ResultSet result =
                                 check.executeQuery()) {

                        if (result.next()
                                && result.getInt(1) > 0) {
                            throw new IllegalStateException(
                                    "La configuración inicial "
                                    + "ya fue realizada."
                            );
                        }
                    }

                    int employeeId;

                    String employeeSql = """
                        INSERT INTO empleados
                        (nombres, apellidos, documento, cargo)
                        VALUES (?, ?, ?, 'ADMINISTRADOR')
                        """;

                    try (PreparedStatement statement =
                            connection.prepareStatement(
                                    employeeSql,
                                    Statement.RETURN_GENERATED_KEYS
                            )) {

                        statement.setString(
                                1, firstName.trim());
                        statement.setString(
                                2, lastName.trim());
                        statement.setString(3, document);

                        statement.executeUpdate();

                        try (ResultSet keys =
                                statement.getGeneratedKeys()) {

                            if (!keys.next()) {
                                throw new SQLException(
                                        "No se obtuvo el ID "
                                        + "del empleado."
                                );
                            }

                            employeeId = keys.getInt(1);
                        }
                    }

                    String userSql = """
                        INSERT INTO usuarios
                        (empleado_id, username,
                         password_hash, password_salt,
                         rol, activo)
                        VALUES (?, ?, ?, ?, 'ADMIN', 1)
                        """;

                    try (PreparedStatement statement =
                            connection.prepareStatement(userSql)) {

                        statement.setInt(1, employeeId);
                        statement.setString(2, username);
                        statement.setString(3, hash);
                        statement.setString(4, salt);

                        statement.executeUpdate();
                    }

                    connection.commit();

                } catch (SQLException | RuntimeException e) {
                    // Revierte ambos registros si algo falla.
                    connection.rollback();
                    throw e;
                }
            }

        } finally {
            // Elimina la contraseña recibida del arreglo.
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
