
package pe.tecnostock.service;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Centraliza la autorización de operaciones sensibles.
 *
 * Verifica la identidad, el estado, el rol y la
 * versión de sesión contra los datos actuales
 * almacenados en SQLite.
 */
public class AuthorizationService {

    /**
     * Comprueba que el usuario tiene permiso ADMIN.
     *
     * @param currentUser Usuario de la sesión.
     * @throws SQLException Si falla la consulta.
     * @throws SecurityException Si no tiene autorización.
     */
    public void requireAdmin(User currentUser)
            throws SQLException {

        try (Connection connection = Database.getConnection()) {
            requireAdmin(connection, currentUser);
        }
    }

    /**
     * Comprueba los permisos usando una conexión existente.
     *
     * Esta variante permite realizar la autorización
     * dentro de una transacción administrativa.
     */
    public void requireAdmin(
            Connection connection,
            User currentUser
    ) throws SQLException {

        if (currentUser == null
                || currentUser.getRole() != Role.ADMIN) {

            throw new SecurityException(
                    "No tienes permisos de administrador."
            );
        }

        String sql = """
            SELECT
                empleado_id,
                username,
                rol,
                activo,
                session_version,
                must_change_password
            FROM usuarios
            WHERE id = ?
            """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, currentUser.getId());

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    throw new SecurityException(
                            "La sesión ya no existe."
                    );
                }

                boolean authorized =
                        currentUser.getEmployeeId()
                                == result.getInt("empleado_id")
                        && currentUser.getUsername().equals(
                                result.getString("username"))
                        && "ADMIN".equals(
                                result.getString("rol"))
                        && result.getInt("activo") == 1
                        && currentUser.getSessionVersion()
                                == result.getInt("session_version")
                        && result.getInt(
                                "must_change_password") == 0;

                if (!authorized) {
                    throw new SecurityException(
                            "Tu sesión ha cambiado o expirado. "
                            + "Inicia sesión nuevamente."
                    );
                }
            }
        }
    }
}
