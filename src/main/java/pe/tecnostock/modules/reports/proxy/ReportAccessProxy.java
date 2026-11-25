package pe.tecnostock.modules.reports.proxy;

import pe.tecnostock.model.User;
import pe.tecnostock.modules.reports.model.SalesReport;
import pe.tecnostock.modules.reports.service.ReportAccess;
import pe.tecnostock.security.Permission;
import pe.tecnostock.service.AuthorizationService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

/** Protege el acceso al servicio de reportes antes de delegar la consulta. */
public final class ReportAccessProxy implements ReportAccess {
    private final ReportAccess target;
    private final AuthorizationService authorization;

    /** Recibe el servicio real y el componente de autorización. */
    public ReportAccessProxy(ReportAccess target, AuthorizationService authorization) {
        this.target = Objects.requireNonNull(target);
        this.authorization = Objects.requireNonNull(authorization);
    }

    /** Comprueba la sesión y el permiso antes de acceder al servicio real. */
    @Override
    public SalesReport generateSales(User currentUser, LocalDate fromDate, LocalDate toDate)
            throws SQLException {
        authorization.requirePermission(currentUser, Permission.VIEW_REPORTS);
        return target.generateSales(currentUser, fromDate, toDate);
    }
}