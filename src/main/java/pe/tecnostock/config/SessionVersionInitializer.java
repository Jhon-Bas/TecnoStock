
package pe.tecnostock.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Prepara el control de versiones de sesión.
 *
 * Cada cambio sensible de una cuenta incrementa
 * su versión y permite invalidar sesiones anteriores.
 */
public final class SessionVersionInitializer {

    private SessionVersionInitializer() {
        // Impide crear objetos de esta clase utilitaria.
    }

    /** Inicializa la estructura usando una conexión propia. */
    public static void initialize() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            initialize(connection);
        }
    }

    /**
     * Agrega la columna de sesión e instala el trigger.
     *
     * Conserva todas las cuentas y contraseñas existentes.
     *
     * Se ejecuta dentro de una migración: el migrador abre
     * la transacción y la confirma o la revierte.
     */
    public static void initialize(Connection connection)
            throws SQLException {

        boolean columnExists = false;

        // Comprueba si esta instalación ya fue actualizada.
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "PRAGMA table_info(usuarios)"
             )) {

            while (result.next()) {
                if ("session_version".equals(
                        result.getString("name"))) {
                    columnExists = true;
                    break;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {

            if (!columnExists) {
                statement.executeUpdate("""
                    ALTER TABLE usuarios
                    ADD COLUMN session_version
                    INTEGER NOT NULL DEFAULT 0
                    """);
            }

            // Invalida sesiones anteriores si cambian
            // las credenciales, el rol o el estado.
            statement.executeUpdate("""
                CREATE TRIGGER IF NOT EXISTS
                trg_user_session_version

                AFTER UPDATE OF
                    password_hash,
                    password_salt,
                    rol,
                    activo
                ON usuarios

                WHEN OLD.password_hash <> NEW.password_hash
                  OR OLD.password_salt <> NEW.password_salt
                  OR OLD.rol <> NEW.rol
                  OR OLD.activo <> NEW.activo

                BEGIN
                    UPDATE usuarios
                    SET session_version = session_version + 1
                    WHERE id = NEW.id;
                END
                """);
        }
    }
}
