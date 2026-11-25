package pe.tecnostock.modules.reports.service;

import pe.tecnostock.model.User;
import pe.tecnostock.modules.reports.model.SalesReport;
import java.sql.SQLException;
import java.time.LocalDate;

/** Contrato común para el servicio real y su proxy de protección. */
public interface ReportAccess {
    /** Solicita un reporte de ventas para el periodo indicado. */
    SalesReport generateSales(User currentUser, LocalDate fromDate, LocalDate toDate)
            throws SQLException;
}