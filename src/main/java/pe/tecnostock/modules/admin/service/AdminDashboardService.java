
package pe.tecnostock.modules.admin.service;

import pe.tecnostock.service.AuthorizationService;

import pe.tecnostock.model.User;
import pe.tecnostock.modules.admin.repository.AdminDashboardRepository;

import java.sql.SQLException;

/**
 * Gestiona los indicadores del administrador.
 *
 * Comprueba la autorización antes de consultar
 * información administrativa del sistema.
 */
public class AdminDashboardService {

    private final AuthorizationService authorization =
            new AuthorizationService();

    private final AdminDashboardRepository repository =
            new AdminDashboardRepository();

    /**
     * Devuelve los indicadores únicamente cuando
     * la sesión administrativa sigue siendo válida.
     */
    public AdminDashboardRepository.DashboardStats
            getStatistics(User currentUser)
            throws SQLException {

        authorization.requireAdmin(currentUser);

        return repository.getStatistics();
    }
}
