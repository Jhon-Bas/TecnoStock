package pe.tecnostock.config.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Un cambio versionado del esquema de SQLite.
 *
 * Cada migración tiene un número único y creciente.
 * El migrador aplica solo las que la base aún no tiene,
 * dentro de una transacción que él abre y cierra: la acción
 * no debe hacer commit ni rollback por su cuenta.
 */
public record Migration(int version, String description, Action action) {

    /** Cambio de esquema que se ejecuta con la conexión del migrador. */
    @FunctionalInterface
    public interface Action {
        void execute(Connection connection) throws SQLException;
    }

    public Migration {
        Objects.requireNonNull(description, "La descripción es obligatoria.");
        Objects.requireNonNull(action, "La acción es obligatoria.");
    }
}
