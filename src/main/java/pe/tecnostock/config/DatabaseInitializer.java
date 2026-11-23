
package pe.tecnostock.config;

import java.sql.*;

/**
 * Inicializa y actualiza la estructura de SQLite.
 *
 * Conserva los datos existentes y agrega las columnas
 * necesarias cuando se actualiza la aplicación.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
        // Impide instanciar esta clase utilitaria.
    }

    /**
     * Crea las tablas y ejecuta las migraciones pendientes.
     */
    public static void initialize() throws SQLException {

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            // Almacena la información personal del empleado.
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS empleados (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombres TEXT NOT NULL,
                    apellidos TEXT NOT NULL,
                    documento TEXT NOT NULL UNIQUE,
                    telefono TEXT,
                    cargo TEXT NOT NULL,
                    fecha_registro TEXT NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Almacena las credenciales y el rol de acceso.
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    empleado_id INTEGER NOT NULL UNIQUE,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    password_salt TEXT NOT NULL,
                    rol TEXT NOT NULL CHECK (
                        rol IN ('ADMIN', 'GERENTE', 'EMPLEADO')
                    ),
                    activo INTEGER NOT NULL DEFAULT 1
                        CHECK (activo IN (0, 1)),
                    fecha_creacion TEXT NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    created_by INTEGER,
                    updated_at TEXT,
                    updated_by INTEGER,
                    FOREIGN KEY (empleado_id)
                        REFERENCES empleados(id),
                    FOREIGN KEY (created_by)
                        REFERENCES usuarios(id),
                    FOREIGN KEY (updated_by)
                        REFERENCES usuarios(id)
                )
                """);

            // Migra las instalaciones anteriores.
            addColumnIfMissing(
                    connection,
                    "usuarios",
                    "created_by",
                    "INTEGER REFERENCES usuarios(id)"
            );

            addColumnIfMissing(
                    connection,
                    "usuarios",
                    "updated_at",
                    "TEXT"
            );

            addColumnIfMissing(
                    connection,
                    "usuarios",
                    "updated_by",
                    "INTEGER REFERENCES usuarios(id)"
            );

            // Facilita las búsquedas por empleado.
            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS
                idx_empleados_nombres
                ON empleados(nombres, apellidos)
                """);
        }

        System.out.println(
                "Base de datos inicializada correctamente."
        );
    }

    /**
     * Agrega una columna únicamente cuando no existe.
     *
     * Permite actualizar la base de datos sin eliminar
     * la información registrada anteriormente.
     */
    private static void addColumnIfMissing(
            Connection connection,
            String table,
            String column,
            String definition
    ) throws SQLException {

        boolean exists = false;

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "PRAGMA table_info(" + table + ")"
             )) {

            while (result.next()) {
                if (column.equalsIgnoreCase(
                        result.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {
            try (Statement statement =
                         connection.createStatement()) {

                statement.executeUpdate(
                        "ALTER TABLE " + table
                        + " ADD COLUMN " + column
                        + " " + definition
                );
            }
        }
    }
}
