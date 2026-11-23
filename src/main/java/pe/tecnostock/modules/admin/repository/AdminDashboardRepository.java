
package pe.tecnostock.modules.admin.repository;

import pe.tecnostock.config.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Consulta los indicadores del panel administrativo.
 *
 * Obtiene datos reales de SQLite sin modificar
 * empleados ni registros de auditoría.
 */
public class AdminDashboardRepository {

    /**
     * Agrupa los indicadores que muestra el dashboard.
     */
    public record DashboardStats(
            int totalEmployees,
            int activeEmployees,
            int managers,
            int auditEvents
    ) {}

    /**
     * Recupera los indicadores administrativos.
     *
     * @return Estadísticas actuales del sistema.
     */
    public DashboardStats getStatistics()
            throws SQLException {

        String sql = """
            SELECT
                (SELECT COUNT(*)
                 FROM empleados) AS total_employees,

                (SELECT COUNT(*)
                 FROM usuarios
                 WHERE activo = 1) AS active_employees,

                (SELECT COUNT(*)
                 FROM usuarios
                 WHERE rol = 'GERENTE'
                   AND activo = 1) AS managers,

                (SELECT COUNT(*)
                 FROM audit_events) AS audit_events
            """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            if (!result.next()) {
                throw new SQLException(
                        "No se pudieron obtener las estadísticas."
                );
            }

            return new DashboardStats(
                    result.getInt("total_employees"),
                    result.getInt("active_employees"),
                    result.getInt("managers"),
                    result.getInt("audit_events")
            );
        }
    }
}
