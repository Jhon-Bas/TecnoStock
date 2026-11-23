
package pe.tecnostock.modules.admin.service;

import pe.tecnostock.service.AuthorizationService;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.User;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Gestiona el restablecimiento administrativo
 * de contraseñas de gerentes y empleados.
 *
 * Comprueba la vigencia de la sesión ADMIN,
 * utiliza el ID de la cuenta y conserva
 * la trazabilidad de la operación.
 */
public class PasswordResetService {

    private final UserRepository userRepository =
            new UserRepository();

    private final AuthorizationService authorizationService =
            new AuthorizationService();

    /**
     * Busca cuentas disponibles para restablecimiento.
     *
     * Solo permite consultar los resultados desde
     * una sesión administrativa válida.
     */
    public List<UserRepository.ResetCandidate> searchUsers(
            User administrator,
            String search
    ) throws SQLException {

        authorizationService.requireAdmin(administrator);

        return userRepository.searchResetCandidates(search);
    }

    /**
     * Restablece la contraseña de una cuenta
     * GERENTE o EMPLEADO seleccionada por su ID.
     *
     * Los datos se actualizan en una transacción.
     */
    public void resetPassword(
            User administrator,
            int targetUserId,
            char[] password,
            char[] confirmation
    ) throws SQLException {

        try {
            if (targetUserId <= 0) {
                throw new IllegalArgumentException(
                        "Selecciona una cuenta válida."
                );
            }

            if (password == null || password.length < 10) {
                throw new IllegalArgumentException(
                        "La contraseña debe tener al menos "
                        + "10 caracteres."
                );
            }

            if (confirmation == null
                    || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException(
                        "Las contraseñas no coinciden."
                );
            }

            try (Connection connection =
                         Database.getConnection()) {

                // Inicia una transacción de escritura antes
                // de comprobar los permisos y el objetivo.
                connection.setAutoCommit(false);

                try {
                    authorizationService.requireAdmin(
                            connection, administrator
                    );

                    String targetSql = """
                        SELECT rol
                        FROM usuarios
                        WHERE id = ?
                        """;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(targetSql)) {

                        statement.setInt(1, targetUserId);

                        try (ResultSet result =
                                     statement.executeQuery()) {

                            if (!result.next()) {
                                throw new IllegalArgumentException(
                                        "La cuenta seleccionada "
                                        + "no existe."
                                );
                            }

                            String role =
                                    result.getString("rol");

                            if (!"GERENTE".equals(role)
                                    && !"EMPLEADO".equals(role)) {

                                throw new SecurityException(
                                        "No se puede restablecer "
                                        + "una cuenta ADMIN."
                                );
                            }
                        }
                    }

                    String salt =
                            PasswordUtil.generateSalt();

                    String hash =
                            PasswordUtil.hashPassword(
                                    password, salt
                            );

                    String updateSql = """
                        UPDATE usuarios
                        SET password_hash = ?,
                            password_salt = ?,
                            updated_at = CURRENT_TIMESTAMP,
                            updated_by = ?
                        WHERE id = ?
                          AND rol IN ('GERENTE', 'EMPLEADO')
                        """;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(updateSql)) {

                        statement.setString(1, hash);
                        statement.setString(2, salt);
                        statement.setInt(
                                3, administrator.getId()
                        );
                        statement.setInt(4, targetUserId);

                        if (statement.executeUpdate() != 1) {
                            throw new SQLException(
                                    "No se pudo actualizar la cuenta."
                            );
                        }
                    }

                    // Los triggers marcarán la contraseña
                    // como temporal, invalidarán sesiones
                    // anteriores y registrarán la auditoría.
                    connection.commit();

                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                }
            }

        } finally {
            // Evita conservar contraseñas en los arreglos.
            if (password != null) {
                Arrays.fill(password, '\0');
            }

            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
    }
}
