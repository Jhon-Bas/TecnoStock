package pe.tecnostock.config.migration;

import pe.tecnostock.config.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Lleva el esquema de SQLite a la última versión.
 *
 * Guarda las versiones aplicadas en la tabla schema_version.
 * Cada migración pendiente corre en su propia transacción:
 * si falla, se revierte y la versión no se registra.
 * Ejecutarlo varias veces no repite ni destruye datos.
 */
public final class SchemaMigrator {

    private final List<Migration> migrations;

    public SchemaMigrator() {
        this(Migrations.all());
    }

    public SchemaMigrator(List<Migration> migrations) {
        validate(migrations);
        this.migrations = List.copyOf(migrations);
    }

    /**
     * Aplica las migraciones pendientes en orden.
     *
     * @return cantidad de migraciones aplicadas en esta llamada
     */
    public int migrate() throws SQLException {

        try (Connection connection = Database.getConnection()) {

            createVersionTable(connection);

            int currentVersion = readVersion(connection);
            int applied = 0;

            for (Migration migration : migrations) {
                if (migration.version() > currentVersion) {
                    apply(connection, migration);
                    applied++;
                }
            }

            return applied;
        }
    }

    /** Última versión registrada, o 0 si la base es nueva. */
    public int currentVersion() throws SQLException {

        try (Connection connection = Database.getConnection()) {
            createVersionTable(connection);
            return readVersion(connection);
        }
    }

    private static void validate(List<Migration> migrations) {

        int previous = 0;

        for (Migration migration : migrations) {
            if (migration.version() <= previous) {
                throw new IllegalArgumentException(
                        "Las migraciones deben tener versiones "
                                + "positivas, únicas y crecientes. "
                                + "Problema en la versión "
                                + migration.version() + "."
                );
            }
            previous = migration.version();
        }
    }

    private static void createVersionTable(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY,
                    descripcion TEXT NOT NULL,
                    aplicada_en TEXT NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                )
                """);
        }
    }

    private static int readVersion(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COALESCE(MAX(version), 0) "
                             + "FROM schema_version"
             )) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static void apply(Connection connection, Migration migration)
            throws SQLException {

        connection.setAutoCommit(false);

        try {
            migration.action().execute(connection);
            recordVersion(connection, migration);
            connection.commit();

        } catch (SQLException | RuntimeException failure) {
            rollback(connection, failure);
            throw failure;

        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void recordVersion(Connection connection, Migration migration)
            throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO schema_version (version, descripcion) "
                        + "VALUES (?, ?)"
        )) {
            statement.setInt(1, migration.version());
            statement.setString(2, migration.description());
            statement.executeUpdate();
        }
    }

    private static void rollback(Connection connection, Exception cause) {

        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }
}
