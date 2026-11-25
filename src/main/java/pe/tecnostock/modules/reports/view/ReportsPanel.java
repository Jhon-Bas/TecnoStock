package pe.tecnostock.modules.reports.view;

import pe.tecnostock.model.User;
import pe.tecnostock.modules.reports.export.ReportExporter;
import pe.tecnostock.modules.reports.model.SalesReport;
import pe.tecnostock.modules.reports.repository.ReportRepository;
import pe.tecnostock.modules.reports.service.ReportService;
import pe.tecnostock.service.AuthorizationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutionException;

/** Interfaz de consulta y exportación; delega reglas y persistencia a sus capas. */
public final class ReportsPanel extends JPanel {
    private final User currentUser;
    private final ReportService service = new ReportService(new ReportRepository(), new AuthorizationService());
    private final ReportExporter exporter = new ReportExporter();
    private final JTextField fromDate = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
    private final JTextField toDate = new JTextField(LocalDate.now().toString(), 10);
    private final JLabel summary = new JLabel("Selecciona un periodo y genera el reporte.");
    private final DefaultTableModel daily = tableModel("Fecha", "Ventas", "Total");
    private final DefaultTableModel items = tableModel("Producto / servicio", "Unidades", "Importe");
    private SalesReport latestReport;

    /** Inicializa el componente y sus dependencias. */
    public ReportsPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(12, 12)); setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Desde (AAAA-MM-DD):")); filters.add(fromDate);
        filters.add(new JLabel("Hasta:")); filters.add(toDate);
        JButton generate = new JButton("Generar reporte"); filters.add(generate);
        JButton pdf = new JButton("Exportar PDF"); filters.add(pdf);
        JButton csv = new JButton("Exportar CSV"); filters.add(csv);
        add(filters, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.add(tablePanel("Ventas por día", new JTable(daily)));
        center.add(tablePanel("Productos y servicios más vendidos", new JTable(items)));
        add(center, BorderLayout.CENTER);
        summary.setBorder(new EmptyBorder(8, 4, 8, 4)); add(summary, BorderLayout.SOUTH);
        generate.addActionListener(e -> generate());
        pdf.addActionListener(e -> export(true)); csv.addActionListener(e -> export(false));
        pdf.setEnabled(true); csv.setEnabled(true);
    }

    /** Construye un modelo de tabla no editable. */
    private static DefaultTableModel tableModel(String... cols) {
        return new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }
    /** Construye el panel de presentación de una tabla. */
    private JPanel tablePanel(String title, JTable table) {
        JPanel p = new JPanel(new BorderLayout(6, 6)); p.setBorder(BorderFactory.createTitledBorder(title));
        table.setFillsViewportHeight(true); p.add(new JScrollPane(table)); return p;
    }
    /** Solicita el reporte fuera del hilo de interfaz. */
    private void generate() {
        latestReport = null;
        final LocalDate start, end;
        try { start = LocalDate.parse(fromDate.getText().trim()); end = LocalDate.parse(toDate.getText().trim()); }
        catch (DateTimeParseException ex) { JOptionPane.showMessageDialog(this, "Usa el formato AAAA-MM-DD.", "Fecha inválida", JOptionPane.WARNING_MESSAGE); return; }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<SalesReport, Void>() {
            @Override protected SalesReport doInBackground() throws Exception { return service.generateSales(currentUser, start, end); }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try { display(get()); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                catch (ExecutionException ex) { JOptionPane.showMessageDialog(ReportsPanel.this, ex.getCause().getMessage(), "No se pudo generar", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }
    /** Actualiza los indicadores y tablas del reporte. */
    private void display(SalesReport r) {
        latestReport = r; daily.setRowCount(0); items.setRowCount(0);
        r.dailySales().forEach(d -> daily.addRow(new Object[]{d.date(), d.quantity(), d.total()}));
        r.topItems().forEach(i -> items.addRow(new Object[]{i.name(), i.quantity(), i.amount()}));
        summary.setText("Periodo: " + r.fromDate() + " al " + r.toDate() + " | Ventas: " + r.saleCount()
                + " | Subtotal: S/ " + r.subtotal() + " | Descuentos: S/ " + r.discounts() + " | Total: S/ " + r.total());
    }
    /** Exporta el reporte seleccionado. */
    private void export(boolean pdf) {
        if (latestReport == null) { JOptionPane.showMessageDialog(this, "Primero genera un reporte."); return; }
        JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new java.io.File(pdf ? "reporte-ventas.pdf" : "reporte-ventas.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        String extension = pdf ? ".pdf" : ".csv";
        if (!path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(extension)) {
            path = path.resolveSibling(path.getFileName() + extension);
        }
        try {
            new AuthorizationService().requirePermission(currentUser, pe.tecnostock.security.Permission.VIEW_REPORTS);
            Path generated = pdf ? exporter.exportPdf(latestReport, path) : exporter.exportCsv(latestReport, path);
            JOptionPane.showMessageDialog(this, "Reporte guardado en:\n" + generated);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "No se pudo exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
    }
}