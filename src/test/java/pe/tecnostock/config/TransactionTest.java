package pe.tecnostock.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba que Transaccion confirme o revierta en conjunto.
 */
@ResourceLock("tecnostock.db.path")
public class TransactionTest {

    @TempDir
    Path tempDirectory;

    private String previousPath;

    @BeforeEach
    void setUp() throws SQLException {
        previousPath = TemporaryDatabase.use(tempDirectory);

        Transaction.executeWithoutResult(connection ->
                TemporaryDatabase.execute(connection,
                        "CREATE TABLE registro (id INTEGER PRIMARY KEY, "
                                + "dato TEXT NOT NULL)")
        );
    }

    @AfterEach
    void restore() {
        TemporaryDatabase.restore(previousPath);
    }

    @Test
    void shouldCommitAllOperations() throws SQLException {

        Transaction.executeWithoutResult(connection -> {
            TemporaryDatabase.execute(connection,
                    "INSERT INTO registro (dato) VALUES ('uno')");
            TemporaryDatabase.execute(connection,
                    "INSERT INTO registro (dato) VALUES ('dos')");
        });

        assertEquals(2, TemporaryDatabase.count("registro"));
    }

    @Test
    void shouldRollbackWhenOperationFails() {

        assertThrows(IllegalStateException.class, () ->
                Transaction.executeWithoutResult(connection -> {
                    TemporaryDatabase.execute(connection,
                            "INSERT INTO registro (dato) VALUES ('uno')");
                    throw new IllegalStateException("Fallo simulado");
                })
        );

        assertDoesNotThrow(() ->
                assertEquals(0, TemporaryDatabase.count("registro"))
        );
    }

    @Test
    void shouldRollbackOnConstraintFailure() {

        assertThrows(SQLException.class, () ->
                Transaction.executeWithoutResult(connection -> {
                    TemporaryDatabase.execute(connection,
                            "INSERT INTO registro (dato) VALUES ('uno')");
                    // dato es NOT NULL: esta sentencia falla.
                    TemporaryDatabase.execute(connection,
                            "INSERT INTO registro (dato) VALUES (NULL)");
                })
        );

        assertDoesNotThrow(() ->
                assertEquals(0, TemporaryDatabase.count("registro"))
        );
    }

    @Test
    void shouldReturnOperationResult() throws SQLException {

        int result = Transaction.execute(connection -> 42);

        assertEquals(42, result);
    }
}
