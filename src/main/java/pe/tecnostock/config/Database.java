
package pe.tecnostock.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Administra las conexiones SQLite de TecnoStock.
 *
 * Utiliza la base de datos principal normalmente.
 * Durante las pruebas permite usar otra ubicación
 * para no modificar la información real del negocio.
 */
public final class Database {

    private static final Path DEFAULT_DB_PATH =
            Paths.get("data", "tecnostock.db");

    private static final String DB_PATH_PROPERTY =
            "tecnostock.db.path";

    private Database() {
        // Evita crear objetos de esta clase utilitaria.
    }

    /**
     * Obtiene la ruta configurada para SQLite.
     *
     * Si no existe una ruta personalizada, utiliza
     * la base de datos principal de TecnoStock.
     */
    private static Path getDatabasePath() {

        String configuredPath =
                System.getProperty(DB_PATH_PROPERTY);

        if (configuredPath == null
                || configuredPath.isBlank()) {

            return DEFAULT_DB_PATH.toAbsolutePath();
        }

        return Paths.get(configuredPath).toAbsolutePath();
    }

    /**
     * Abre una conexión y activa las claves foráneas.
     *
     * @return Conexión lista para ejecutar consultas.
     * @throws SQLException Si no puede abrirse SQLite.
     */
    public static Connection getConnection()
            throws SQLException {

        Path databasePath = getDatabasePath();

        try {
            // Crea la carpeta donde se guardará SQLite.
            Files.createDirectories(
                    databasePath.getParent()
            );

        } catch (IOException e) {
            throw new SQLException(
                    "No se pudo crear el directorio de datos.",
                    e
            );
        }

        String url = "jdbc:sqlite:" + databasePath;

        Connection connection =
                DriverManager.getConnection(url);

        try {
            // Activa la integridad referencial
            // para cada conexión utilizada.
            try (Statement statement =
                         connection.createStatement()) {

                statement.execute(
                        "PRAGMA foreign_keys = ON"
                );
            }

            return connection;

        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }
}
