package pe.tecnostock.config;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Apoyo para pruebas: apunta Database a un archivo SQLite
 * temporal y nunca toca la base de datos real del proyecto.
 */
public final class TemporaryDatabase {

    private static final String PROPERTY = "tecnostock.db.path";

    private TemporaryDatabase() {
    }

    /** Usa una base nueva en el directorio dado. Devuelve la ruta anterior. */
    public static String use(Path directory) {

        String previous = System.getProperty(PROPERTY);

        System.setProperty(
                PROPERTY,
                directory.resolve("test-" + System.nanoTime() + ".db")
                        .toString()
        );

        return previous;
    }

    public static void restore(String previous) {

        if (previous == null) {
            System.clearProperty(PROPERTY);
        } else {
            System.setProperty(PROPERTY, previous);
        }
    }

    public static boolean tableExists(String name) throws SQLException {

        try (Connection connection = Database.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT 1 FROM sqlite_master "
                             + "WHERE type = 'table' AND name = ?"
             )) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public static void execute(Connection connection, String sql)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    public static int count(String table) throws SQLException {

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + table
             )) {
            return result.getInt(1);
        }
    }
}
