
package pe.tecnostock.repository;

import pe.tecnostock.config.Database;
import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona las consultas de cuentas de usuario.
 *
 * Separa el acceso a SQLite de la autenticación,
 * los permisos y las reglas de negocio.
 */
public class UserRepository {

    /**
     * Contiene los datos requeridos para autenticar
     * a un usuario sin almacenar su contraseña original.
     */
    public record Credentials(
            User user,
            String passwordHash,
            String passwordSalt
    ) {}

    /**
     * Representa una cuenta que puede seleccionarse
     * desde el buscador de contraseñas.
     */
    public record ResetCandidate(
            int userId,
            String firstName,
            String lastName,
            String document,
            String username,
            Role role,
            boolean active
    ) {}

    /**
     * Consulta una cuenta por su nombre de usuario.
     *
     * Incluye la versión actual de sesión para
     * detectar modificaciones posteriores.
     */
    public Optional<Credentials> findByUsername(
            String username
    ) throws SQLException {

        String sql = """
            SELECT
                id,
                empleado_id,
                username,
                password_hash,
                password_salt,
                rol,
                activo,
                session_version
            FROM usuarios
            WHERE username = ?
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return Optional.empty();
                }

                User user = new User(
                        result.getInt("id"),
                        result.getInt("empleado_id"),
                        result.getString("username"),
                        Role.valueOf(result.getString("rol")),
                        result.getInt("activo") == 1,
                        result.getInt("session_version")
                );

                return Optional.of(new Credentials(
                        user,
                        result.getString("password_hash"),
                        result.getString("password_salt")
                ));
            }
        }
    }

    /**
     * Busca gerentes y empleados por nombre, apellido,
     * DNI o usuario para restablecer contraseñas.
     */
    public List<ResetCandidate> searchResetCandidates(
            String search
    ) throws SQLException {

        List<ResetCandidate> candidates =
                new ArrayList<>();

        String sql = """
            SELECT
                u.id AS user_id,
                e.nombres,
                e.apellidos,
                e.documento,
                u.username,
                u.rol,
                u.activo
            FROM usuarios u
            INNER JOIN empleados e
                ON e.id = u.empleado_id
            WHERE u.rol IN ('GERENTE', 'EMPLEADO')
              AND (
                  LOWER(e.nombres) LIKE ?
                  OR LOWER(e.apellidos) LIKE ?
                  OR e.documento LIKE ?
                  OR LOWER(u.username) LIKE ?
              )
            ORDER BY e.apellidos, e.nombres, u.id
            LIMIT 50
            """;

        String filter = "%"
                + (search == null
                    ? "" : search.trim().toLowerCase())
                + "%";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            // Usa parámetros para evitar inyección SQL.
            for (int i = 1; i <= 4; i++) {
                statement.setString(i, filter);
            }

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    candidates.add(new ResetCandidate(
                            result.getInt("user_id"),
                            result.getString("nombres"),
                            result.getString("apellidos"),
                            result.getString("documento"),
                            result.getString("username"),
                            Role.valueOf(result.getString("rol")),
                            result.getInt("activo") == 1
                    ));
                }
            }
        }

        return candidates;
    }
}
