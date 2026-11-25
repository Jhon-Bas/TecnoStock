package pe.tecnostock.modules.sales.view;

import pe.tecnostock.model.Sale;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.customers.repository.CustomerRepository;
import pe.tecnostock.modules.inventory.repository.InventoryRepository;
import pe.tecnostock.modules.inventory.service.InventoryService;
import pe.tecnostock.modules.products.repository.ProductRepository;
import pe.tecnostock.modules.products.repository.ServiceItemRepository;
import pe.tecnostock.modules.sales.repository.SaleRepository;
import pe.tecnostock.modules.sales.service.SaleService;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.view.Styles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Consulta de boletas emitidas. Reutiliza el servicio de ventas y sus permisos. */
public final class ReceiptsPanel extends JPanel {
    private final User currentUser;
    private final SaleService service;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"N.º", "Boleta", "Cliente", "Documento", "Fecha", "Total", "Estado"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<Sale> sales = List.of();

    public ReceiptsPanel(User currentUser) {
        this.currentUser = currentUser;
        AuthorizationService authorization = new AuthorizationService();
        ProductRepository products = new ProductRepository();
        this.service = new SaleService(new SaleRepository(), new CustomerRepository(),
                products, new ServiceItemRepository(),
                new InventoryService(new InventoryRepository(), products, authorization), authorization);
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = Styles.actionButton("Actualizar");
        JButton view = Styles.actionButton("Ver / imprimir / guardar PDF");
        actions.add(refresh); actions.add(view);
        table.setRowHeight(26);
        if (currentUser.getRole() != pe.tecnostock.model.Role.ADMIN) table.removeColumn(table.getColumnModel().getColumn(0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(actions, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh.addActionListener(e -> load());
        view.addActionListener(e -> showSelected());
        load();
    }

    private void load() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<Sale>, Void>() {
            @Override protected List<Sale> doInBackground() throws SQLException {
                return service.listSales(currentUser);
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    sales = get(); model.setRowCount(0);
                    for (Sale sale : sales) model.addRow(new Object[]{sale.getId(),
                            sale.getReceiptNumber(), sale.getCustomerName(), sale.getCustomerDocument(),
                            sale.getDate(), sale.getTotal(), sale.getStatus()});
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    JOptionPane.showMessageDialog(ReceiptsPanel.this,
                            causa == null ? "No se pudieron cargar las boletas." : causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void showSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona una boleta."); return; }
        int index = table.convertRowIndexToModel(row);
        new ReceiptDialog(SwingUtilities.getWindowAncestor(this), sales.get(index)).setVisible(true);
    }
}