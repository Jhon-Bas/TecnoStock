
package pe.tecnostock.modules.admin.service;

import pe.tecnostock.service.AuthorizationService;

import pe.tecnostock.model.User;
import pe.tecnostock.modules.admin.repository.AuditRepository;
import pe.tecnostock.repository.UserRepository;

import java.sql.SQLException;

/**
 * Controla el acceso al historial administrativo.
 *
 * Verifica la sesión y delega las búsquedas
 * paginadas al repositorio de auditoría.
 */
public class AuditService {

    private final AuditRepository auditRepository;

    private final AuthorizationService authorizationService =
            new AuthorizationService();

    /**
     * Recibe el repositorio de auditoría.
     *
     * UserRepository se conserva como parámetro
     * para no alterar los formularios existentes.
     */
    public AuditService(
            AuditRepository auditRepository,
            UserRepository userRepository
    ) {
        this.auditRepository = auditRepository;
    }

    /**
     * Consulta eventos solamente cuando el usuario
     * conserva una sesión administrativa válida.
     */
    public AuditRepository.AuditPage listEvents(
            User currentUser,
            String search,
            int page,
            int pageSize
    ) throws SQLException {

        authorizationService.requireAdmin(currentUser);

        return auditRepository.findPage(
                search == null ? "" : search.trim(),
                page,
                pageSize
        );
    }
}
