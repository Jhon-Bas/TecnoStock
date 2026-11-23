
package pe.tecnostock.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Prepara el sistema de auditoría de TecnoStock.
 *
 * Crea la tabla de eventos y los triggers de SQLite
 * que registran automáticamente las modificaciones
 * de las cuentas de usuario.
 */
public final class AuditInitializer {

    private AuditInitializer() {
        // Evita crear instancias de esta clase.
    }

    /**
     * Inicializa la auditoría sin borrar registros.
     *
     * @throws SQLException Si ocurre un error en SQLite.
     */
    public static void initialize() throws SQLException {

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            // Conserva el historial de operaciones.
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS audit_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    actor_user_id INTEGER,
                    target_user_id INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    details TEXT NOT NULL,
                    created_at TEXT NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (actor_user_id)
                        REFERENCES usuarios(id),
                    FOREIGN KEY (target_user_id)
                        REFERENCES usuarios(id)
                )
                """);

            // Registra la creación de una nueva cuenta.
            statement.executeUpdate("""
                CREATE TRIGGER IF NOT EXISTS
                trg_audit_user_created

                AFTER INSERT ON usuarios

                BEGIN
                    INSERT INTO audit_events (
                        actor_user_id,
                        target_user_id,
                        action,
                        details
                    )
                    VALUES (
                        NEW.created_by,
                        NEW.id,
                        'CREACION',
                        'Cuenta creada con rol ' || NEW.rol
                    );
                END
                """);

            // Registra modificaciones de cuentas, roles,
            // estados y restablecimientos de contraseñas.
            statement.executeUpdate("""
                CREATE TRIGGER IF NOT EXISTS
                trg_audit_user_updated

                AFTER UPDATE OF
                    username,
                    rol,
                    activo,
                    password_hash,
                    updated_at
                ON usuarios

                BEGIN
                    INSERT INTO audit_events (
                        actor_user_id,
                        target_user_id,
                        action,
                        details
                    )
                    VALUES (
                        NEW.updated_by,
                        NEW.id,

                        CASE
                            WHEN OLD.password_hash
                                 <> NEW.password_hash
                                THEN 'PASSWORD_RESET'

                            WHEN OLD.activo <> NEW.activo
                                 AND NEW.activo = 0
                                THEN 'DESACTIVACION'

                            WHEN OLD.activo <> NEW.activo
                                 AND NEW.activo = 1
                                THEN 'REACTIVACION'

                            WHEN OLD.rol <> NEW.rol
                                THEN 'CAMBIO_ROL'

                            ELSE 'ACTUALIZACION'
                        END,

                        CASE
                            WHEN OLD.password_hash
                                 <> NEW.password_hash
                                THEN 'Contraseña restablecida'

                            WHEN OLD.activo <> NEW.activo
                                THEN 'Estado: '
                                     || OLD.activo
                                     || ' -> '
                                     || NEW.activo

                            WHEN OLD.rol <> NEW.rol
                                THEN 'Rol: '
                                     || OLD.rol
                                     || ' -> '
                                     || NEW.rol

                            ELSE 'Datos de cuenta actualizados'
                        END
                    );
                END
                """);

            // Mejora las consultas del historial.
            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS
                idx_audit_created
                ON audit_events(created_at DESC, id DESC)
                """);
        }

        System.out.println(
                "Sistema de auditoría inicializado correctamente."
        );
    }
}
