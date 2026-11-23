
package pe.tecnostock.config;

import java.sql.*;

/**
 * Prepara las reglas de contraseñas temporales.
 *
 * Actualiza SQLite sin eliminar usuarios existentes
 * y marca las nuevas contraseñas administrativas
 * para solicitar su cambio al iniciar sesión.
 */
public final class PasswordPolicyInitializer {

    private PasswordPolicyInitializer() {
        // Impide crear instancias de esta clase.
    }

    /**
     * Agrega la columna y los triggers necesarios.
     */
    public static void initialize() throws SQLException {

        try (Connection connection = Database.getConnection()) {

            boolean exists = false;

            // Comprueba si la columna ya está creada.
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "PRAGMA table_info(usuarios)"
                 )) {

                while (result.next()) {
                    if ("must_change_password".equals(
                            result.getString("name"))) {
                        exists = true;
                        break;
                    }
                }
            }

            try (Statement statement = connection.createStatement()) {

                if (!exists) {
                    // Las cuentas anteriores conservan su acceso
                    // hasta que se restablezca su contraseña.
                    statement.executeUpdate("""
                        ALTER TABLE usuarios
                        ADD COLUMN must_change_password INTEGER
                        NOT NULL DEFAULT 0
                        CHECK (must_change_password IN (0, 1))
                        """);
                }

                // Marca como temporal la contraseña de
                // cualquier gerente o empleado recién creado.
                statement.executeUpdate("""
                    CREATE TRIGGER IF NOT EXISTS
                    trg_temporary_password_new_user

                    AFTER INSERT ON usuarios
                    WHEN NEW.rol IN ('GERENTE', 'EMPLEADO')

                    BEGIN
                        UPDATE usuarios
                        SET must_change_password = 1
                        WHERE id = NEW.id;
                    END
                    """);

                // Marca como temporal una contraseña
                // restablecida por otra cuenta administrativa.
                statement.executeUpdate("""
                    CREATE TRIGGER IF NOT EXISTS
                    trg_temporary_password_reset

                    AFTER UPDATE OF password_hash ON usuarios

                    WHEN NEW.rol IN ('GERENTE', 'EMPLEADO')
                         AND NEW.updated_by IS NOT NULL
                         AND NEW.updated_by <> NEW.id
                         AND NEW.password_hash <> OLD.password_hash

                    BEGIN
                        UPDATE usuarios
                        SET must_change_password = 1
                        WHERE id = NEW.id;
                    END
                    """);
            }
        }

        System.out.println(
                "Política de contraseñas inicializada."
        );
    }
}
