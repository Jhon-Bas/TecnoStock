
package pe.tecnostock.modules.admin.repository;

import pe.tecnostock.config.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta el historial de acciones administrativas.
 *
 * Utiliza paginación desde SQLite y no proporciona
 * operaciones para borrar registros de auditoría.
 */
public class AuditRepository {

    /**
     * Representa un evento administrativo.
     */
    public record AuditRow(
            int id,
            String date,
            String actor,
            String target,
            String action,
            String details
    ) {}

    /**
     * Representa el resultado de una página.
     */
    public record AuditPage(
            List<AuditRow> events,
            int total
    ) {}

    /**
     * Consulta eventos paginados con búsqueda.
     *
     * @param search Texto de búsqueda.
     * @param page Página desde cero.
     * @param pageSize Cantidad de registros.
     * @return Eventos y cantidad total encontrada.
     */
    public AuditPage findPage(
            String search,
            int page,
            int pageSize
    ) throws SQLException {

        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException(
                    "Parámetros de paginación inválidos."
            );
        }

        String filter = "%" + search.toLowerCase() + "%";

        String joins = """
            FROM audit_events a
            LEFT JOIN usuarios actor
                ON actor.id = a.actor_user_id
            LEFT JOIN usuarios target
                ON target.id = a.target_user_id
            """;

        String where = """
            WHERE LOWER(COALESCE(actor.username, '')) LIKE ?
               OR LOWER(COALESCE(target.username, '')) LIKE ?
               OR LOWER(a.action) LIKE ?
               OR LOWER(a.details) LIKE ?
            """;

        String countSql =
                "SELECT COUNT(*) " + joins + where;

        String listSql = """
            SELECT
                a.id,
                a.created_at,
                COALESCE(
                    actor.username, 'Configuración inicial'
                ) AS actor_name,
                COALESCE(
                    target.username, 'Cuenta desconocida'
                ) AS target_name,
                a.action,
                a.details
            """ + joins + where + """
            ORDER BY a.created_at DESC, a.id DESC
            LIMIT ? OFFSET ?
            """;

        try (Connection connection = Database.getConnection()) {

            int total;

            // Obtiene el total de registros coincidentes.
            try (PreparedStatement statement =
                    connection.prepareStatement(countSql)) {

                setFilter(statement, filter);

                try (ResultSet result =
                        statement.executeQuery()) {

                    total = result.next()
                            ? result.getInt(1) : 0;
                }
            }

            List<AuditRow> events = new ArrayList<>();

            // Recupera únicamente la página solicitada.
            try (PreparedStatement statement =
                    connection.prepareStatement(listSql)) {

                setFilter(statement, filter);

                statement.setInt(5, pageSize);
                statement.setInt(6, page * pageSize);

                try (ResultSet result =
                        statement.executeQuery()) {

                    while (result.next()) {

                        events.add(new AuditRow(
                                result.getInt("id"),
                                result.getString("created_at"),
                                result.getString("actor_name"),
                                result.getString("target_name"),
                                result.getString("action"),
                                result.getString("details")
                        ));
                    }
                }
            }

            return new AuditPage(events, total);
        }
    }

    /**
     * Asigna el filtro a las consultas parametrizadas.
     */
    private void setFilter(
            PreparedStatement statement,
            String filter
    ) throws SQLException {

        for (int i = 1; i <= 4; i++) {
            statement.setString(i, filter);
        }
    }
}
