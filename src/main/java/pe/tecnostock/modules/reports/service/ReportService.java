package pe.tecnostock.modules.reports.service;

import pe.tecnostock.model.User;
import pe.tecnostock.modules.reports.model.SalesReport;
import pe.tecnostock.modules.reports.repository.ReportQuery;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.security.Permission;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

/** Orquesta validaciones, autorización y consulta del reporte. */
public final class ReportService {
    private final ReportQuery repository;
    private final AuthorizationService authorization;

    /** Inicializa el componente y sus dependencias. */
    public ReportService(ReportQuery repository, AuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository);
        this.authorization = Objects.requireNonNull(authorization);
    }

    /** Valida el intervalo, comprueba los permisos y consulta las ventas. */
    public SalesReport generateSales(User currentUser, LocalDate fromDate, LocalDate toDate)
            throws SQLException {
        Objects.requireNonNull(fromDate, "La fecha inicial es obligatoria.");
        Objects.requireNonNull(toDate, "La fecha final es obligatoria.");
        if (toDate.isBefore(fromDate)) throw new IllegalArgumentException(
                "La fecha final no puede ser anterior a la fecha inicial.");
        if (toDate.isAfter(LocalDate.now())) throw new IllegalArgumentException(
                "La fecha final no puede estar en el futuro.");
        if (fromDate.isBefore(LocalDate.of(2000, 1, 1))) throw new IllegalArgumentException(
                "El rango de fechas no es válido.");
        authorization.requirePermission(currentUser, Permission.VIEW_REPORTS);
        return repository.getSales(fromDate, toDate);
    }
}