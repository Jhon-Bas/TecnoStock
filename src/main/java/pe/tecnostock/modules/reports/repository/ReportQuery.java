package pe.tecnostock.modules.reports.repository;

import pe.tecnostock.modules.reports.model.SalesReport;
import java.sql.SQLException;
import java.time.LocalDate;

/** Puerto de consulta que permite sustituir la persistencia en pruebas. */
@FunctionalInterface
public interface ReportQuery {
    SalesReport getSales(LocalDate fromDate, LocalDate toDate) throws SQLException;
}