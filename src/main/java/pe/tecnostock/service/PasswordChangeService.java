
package pe.tecnostock.service;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.User;
import pe.tecnostock.util.PasswordUtil;

import java.sql.*;
import java.util.Arrays;

/**
 * Gestiona el cambio obligatorio de contraseña.
 *
 * Verifica la contraseña anterior, almacena
 * el nuevo hash y elimina la obligación de cambio.
 */
public class PasswordChangeService {

    /**
     * Consulta si el usuario debe cambiar su contraseña.
     */
    public boolean mustChangePassword(User user)
            throws SQLException {

        if (user == null) {
            return false;
        }

        String sql = """
            SELECT activo, must_change_password
            FROM usuarios
            WHERE id = ?
              AND username = ?
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, user.getId());
            statement.setString(2, user.getUsername());

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()
                        || result.getInt("activo") != 1) {
                    throw new SecurityException(
                            "La cuenta ya no está disponible."
                    );
                }

                return result.getInt(
                        "must_change_password") == 1;
            }
        }
    }

    /**
     * Sustituye la contraseña temporal por una propia.
     *
     * Los arreglos de contraseñas se limpian al terminar.
     */
    public void changePassword(
            User user,
            char[] oldPassword,
            char[] newPassword,
            char[] confirmation
    ) throws SQLException {

        try {
            if (user == null) {
                throw new SecurityException(
                        "Debes iniciar sesión."
                );
            }

            if (oldPassword == null || oldPassword.length == 0) {
                throw new IllegalArgumentException(
                        "Ingresa tu contraseña temporal."
                );
            }

            if (newPassword == null
                    || newPassword.length < 10) {
                throw new IllegalArgumentException(
                        "La nueva contraseña debe tener "
                        + "al menos 10 caracteres."
                );
            }

            if (confirmation == null
                    || !Arrays.equals(newPassword, confirmation)) {
                throw new IllegalArgumentException(
                        "Las contraseñas nuevas no coinciden."
                );
            }

            try (Connection connection =
                         Database.getConnection()) {

                connection.setAutoCommit(false);

                try {
                    String sql = """
                        SELECT password_hash,
                               password_salt,
                               activo,
                               must_change_password
                        FROM usuarios
                        WHERE id = ?
                          AND username = ?
                        """;

                    String storedHash;
                    String storedSalt;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(sql)) {

                        statement.setInt(1, user.getId());
                        statement.setString(2, user.getUsername());

                        try (ResultSet result =
                                     statement.executeQuery()) {

                            if (!result.next()
                                    || result.getInt("activo") != 1
                                    || result.getInt(
                                        "must_change_password") != 1) {

                                throw new SecurityException(
                                        "No existe un cambio "
                                        + "de contraseña pendiente."
                                );
                            }

                            storedHash =
                                    result.getString("password_hash");

                            storedSalt =
                                    result.getString("password_salt");
                        }
                    }

                    // Comprueba que conoce la contraseña
                    // temporal asignada a su cuenta.
                    if (!PasswordUtil.verifyPassword(
                            oldPassword, storedHash, storedSalt)) {

                        throw new IllegalArgumentException(
                                "La contraseña temporal es incorrecta."
                        );
                    }

                    // Impide conservar la misma contraseña.
                    if (Arrays.equals(oldPassword, newPassword)) {
                        throw new IllegalArgumentException(
                                "La nueva contraseña debe ser diferente."
                        );
                    }

                    String newSalt =
                            PasswordUtil.generateSalt();

                    String newHash =
                            PasswordUtil.hashPassword(
                                    newPassword, newSalt
                            );

                    String updateSql = """
                        UPDATE usuarios
                        SET password_hash = ?,
                            password_salt = ?,
                            must_change_password = 0,
                            updated_at = CURRENT_TIMESTAMP,
                            updated_by = ?
                        WHERE id = ?
                          AND activo = 1
                          AND must_change_password = 1
                        """;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(updateSql)) {

                        statement.setString(1, newHash);
                        statement.setString(2, newSalt);

                        // Registra que el propio usuario
                        // realizó este cambio de contraseña.
                        statement.setInt(3, user.getId());
                        statement.setInt(4, user.getId());

                        if (statement.executeUpdate() != 1) {
                            throw new SQLException(
                                    "No se pudo guardar el cambio."
                            );
                        }
                    }

                    connection.commit();

                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                }
            }

        } finally {
            // Limpia todas las contraseñas utilizadas.
            if (oldPassword != null) {
                Arrays.fill(oldPassword, '\0');
            }
            if (newPassword != null) {
                Arrays.fill(newPassword, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
    }
}
