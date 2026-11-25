package pe.tecnostock.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import pe.tecnostock.config.migration.Migration;
import pe.tecnostock.config.migration.SchemaMigrator;
import pe.tecnostock.config.migration.Migrations;
import pe.tecnostock.service.BootstrapService;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba las migraciones versionadas del esquema.
 */
@ResourceLock("tecnostock.db.path")
public class SchemaMigratorTest {

    @TempDir
    Path tempDirectory;

    private String previousPath;

    @BeforeEach
    void setUp() {
        previousPath = TemporaryDatabase.use(tempDirectory);
    }

    @AfterEach
    void restore() {
        TemporaryDatabase.restore(previousPath);
    }

    @Test
    void shouldApplyAllMigrationsOnNewDatabase()
            throws SQLException {

        SchemaMigrator migrator = new SchemaMigrator();
        int expected = Migrations.all().size();

        assertEquals(expected, migrator.migrate());

        int latest = Migrations.all()
                .get(expected - 1).version();

        assertEquals(latest, migrator.currentVersion());
        assertTrue(TemporaryDatabase.tableExists("schema_version"));
        assertTrue(TemporaryDatabase.tableExists("usuarios"));
        assertTrue(TemporaryDatabase.tableExists("empleados"));
        assertTrue(TemporaryDatabase.tableExists("clientes"));
        assertTrue(TemporaryDatabase.tableExists("categorias"));
        assertTrue(TemporaryDatabase.tableExists("productos"));
        assertTrue(TemporaryDatabase.tableExists("servicios"));
        assertEquals(4, TemporaryDatabase.count("categorias"));
        assertTrue(TemporaryDatabase.tableExists("movimientos_inventario"));
    }

    @Test
    void shouldBeIdempotent() throws SQLException {

        SchemaMigrator migrator = new SchemaMigrator();
        migrator.migrate();

        assertEquals(0, migrator.migrate());
    }

    @Test
    void shouldMigrateExistingDatabaseWithoutDataLoss()
            throws SQLException {

        // Reproduce una instalación hecha antes de las migraciones.
        try (Connection connection = Database.getConnection()) {
            DatabaseInitializer.initialize(connection);
            AuditInitializer.initialize(connection);
            PasswordPolicyInitializer.initialize(connection);
            SessionVersionInitializer.initialize(connection);
        }

        BootstrapService bootstrap = new BootstrapService();
        bootstrap.createInitialAdmin(
                "Administrador",
                "Prueba",
                "12345678",
                "admin_test",
                "AdminPrueba2026!".toCharArray()
        );

        new SchemaMigrator().migrate();

        assertTrue(bootstrap.hasUsers());
        assertEquals(1, TemporaryDatabase.count("usuarios"));
    }

    @Test
    void shouldRollbackFailedMigration() throws SQLException {

        Migration valid = new Migration(
                1, "Crea tabla A",
                connection -> TemporaryDatabase.execute(
                        connection, "CREATE TABLE prueba_a (id INTEGER)")
        );

        Migration invalid = new Migration(
                2, "Crea tabla B y falla",
                connection -> {
                    TemporaryDatabase.execute(
                            connection, "CREATE TABLE prueba_b (id INTEGER)");
                    throw new SQLException("Fallo simulado");
                }
        );

        SchemaMigrator migrator =
                new SchemaMigrator(List.of(valid, invalid));

        assertThrows(SQLException.class, migrator::migrate);

        assertEquals(1, migrator.currentVersion());
        assertTrue(TemporaryDatabase.tableExists("prueba_a"));
        assertFalse(TemporaryDatabase.tableExists("prueba_b"));
    }

    @Test
    void shouldRejectDuplicateVersions() {

        Migration migration = new Migration(
                1, "Cambio", connection -> { }
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SchemaMigrator(List.of(migration, migration))
        );
    }
}
