package pe.tecnostock.config;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ejecuta varias operaciones de base de datos como una unidad.
 *
 * Si todas terminan bien se confirma la transacción.
 * Si alguna falla se revierte todo. Es la base de la venta,
 * donde el registro y el descuento de stock no pueden separarse.
 *
 * Los repositorios que participen deben tener métodos que reciban
 * la Connection en lugar de abrir una propia.
 */
public final class Transaction {

    private Transaction() {
        // Impide crear instancias de esta clase.
    }

    /** Operación que devuelve un resultado. */
    @FunctionalInterface
    public interface Operation<T> {
        T execute(Connection connection) throws SQLException;
    }

    /** Operación que no devuelve nada. */
    @FunctionalInterface
    public interface VoidOperation {
        void execute(Connection connection) throws SQLException;
    }

    public static <T> T execute(Operation<T> operation)
            throws SQLException {

        try (Connection connection = Database.getConnection()) {

            connection.setAutoCommit(false);

            try {
                T result = operation.execute(connection);
                connection.commit();
                return result;

            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        }
    }

    public static void executeWithoutResult(
            VoidOperation operation
    ) throws SQLException {

        execute(connection -> {
            operation.execute(connection);
            return null;
        });
    }

    private static void rollback(Connection connection, Exception cause) {

        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }
}
