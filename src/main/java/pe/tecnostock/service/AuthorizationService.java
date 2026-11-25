package pe.tecnostock.service;

import pe.tecnostock.security.Permission;
import pe.tecnostock.security.PermissionPolicy;
import pe.tecnostock.model.Role;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Autoriza operaciones según permisos y sesión vigente.
 *
 * Comprueba dos cosas:
 * 1. Que el rol del usuario tenga el permiso (PermissionPolicy).
 * 2. Que la sesión siga vigente en SQLite: la cuenta existe,
 *    está activa, conserva su rol y versión de sesión, y no
 *    tiene un cambio de contraseña pendiente.
 *
 * Los servicios la llaman antes de operar, de modo que la
 * seguridad no depende de qué botones muestre la interfaz.
 */
public class AuthorizationService {

    /** Comprueba el acceso administrativo con una conexión propia. */
    public void requireAdmin(User currentUser) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            requireAdmin(connection, currentUser);
        }
    }

    /** Comprueba el acceso administrativo dentro de una transacción. */
    public void requireAdmin(Connection connection, User currentUser)
            throws SQLException {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw new SecurityException("No tienes permisos de administrador.");
        }
        requirePermission(connection, currentUser, Permission.MANAGE_USERS);
    }

    /**
     * @throws SecurityException si no hay sesión, falta el permiso
     *                           o la sesión ya no es vigente
     */
    public void requirePermission(User currentUser, Permission permission)
            throws SQLException {

        try (Connection connection = Database.getConnection()) {
            requirePermission(connection, currentUser, permission);
        }
    }

    /**
     * Variante para ejecutar la autorización dentro de una
     * transacción que ya está abierta.
     */
    public void requirePermission(
            Connection connection,
            User currentUser,
            Permission permission
    ) throws SQLException {

        Objects.requireNonNull(permission, "El permiso es obligatorio.");

        if (currentUser == null) {
            throw new SecurityException("Debes iniciar sesión.");
        }

        if (!PermissionPolicy.has(currentUser.getRole(), permission)) {
            throw new SecurityException(
                    "No tienes permiso para realizar esta operación."
            );
        }

        verifyCurrentSession(connection, currentUser);
    }

    /** Comprueba identidad, estado, rol, versión y cambio de contraseña. */
    private void verifyCurrentSession(Connection connection, User currentUser)
            throws SQLException {

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

                boolean valid =
                        currentUser.getEmployeeId()
                                == result.getInt("empleado_id")
                        && currentUser.getUsername().equals(
                                result.getString("username"))
                        && currentUser.getRole().name().equals(
                                result.getString("rol"))
                        && result.getInt("activo") == 1
                        && currentUser.getSessionVersion()
                                == result.getInt("session_version")
                        && result.getInt("must_change_password") == 0;

                if (!valid) {
                    throw new SecurityException(
                            "Tu sesión ha cambiado o expirado. "
                            + "Inicia sesión nuevamente."
                    );
                }
            }
        }
    }
}
