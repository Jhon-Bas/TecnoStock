package pe.tecnostock.modules.sales.view;

import pe.tecnostock.modules.sales.facade.SalesFacade;

import pe.tecnostock.model.*;
import pe.tecnostock.modules.sales.service.SaleService;
import pe.tecnostock.security.Permission;
import pe.tecnostock.security.PermissionPolicy;
import pe.tecnostock.view.Styles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pantalla de ventas integrada al área central de MainFrame.
 * Permite armar un carrito, aplicar descuento autorizado y consultar ventas.
 */
public class SalesPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(SalesPanel.class.getName());
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-PE"));

    private record CustomerOption(Customer customer) {
        @Override public String toString() {
            return customer.getName() + " - " + customer.getDocument();
        }
    }

    private record CartLine(CommercialItem item, int quantity) {
        BigDecimal subtotal() {
            return item.getSalePrice().multiply(BigDecimal.valueOf(quantity));
        }
    }

    private final User currentUser;
    private final SalesFacade facade = new SalesFacade();

    private final JComboBox<CustomerOption> cmbCliente = new JComboBox<>();
    private final JComboBox<CommercialItem> cmbItem = new JComboBox<>();
    private final JSpinner spCantidad = new JSpinner(
            new SpinnerNumberModel(1, 1, 9999, 1));
    private final JTextField txtDescuento = new JTextField("0.00", 9);
    private final JTextField txtBuscar = new JTextField(16);
    private final JLabel lblSubtotal = new JLabel("S/ 0.00");
    private final JLabel lblDescuento = new JLabel("S/ 0.00");
    private final JLabel lblTotal = new JLabel("S/ 0.00");

    private final DefaultTableModel modeloCarrito = model(
            "Tipo", "Código", "Descripción", "Cant.", "P. unitario", "Importe");
    private final DefaultTableModel modeloHistorial = model(
            "N.º", "Boleta", "Cliente", "Fecha", "Subtotal", "Descuento", "Total", "Estado");

    private final JTable tablaCarrito = new JTable(modeloCarrito);
    private final JTable tablaHistorial = new JTable(modeloHistorial);
    private final List<CartLine> cart = new ArrayList<>();
    private List<Sale> loadedSales = List.of();

    private final JButton btnAgregar = Styles.actionButton("Agregar ítem");
    private final JButton btnQuitar = Styles.actionButton("Quitar seleccionado");
    private final JButton btnRegistrar = Styles.actionButton("Registrar venta");
    private final JButton btnActualizar = Styles.actionButton("Actualizar historial");
    private final JButton btnVerBoleta = Styles.actionButton("Ver / imprimir boleta");

    public SalesPanel(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser, "El usuario es obligatorio.");
        boolean puedeRegistrar = PermissionPolicy.has(
                currentUser.getRole(), Permission.REGISTER_SALES);
        boolean puedeConsultar = PermissionPolicy.has(currentUser.getRole(),
                Permission.VIEW_OWN_SALES)
                || PermissionPolicy.has(currentUser.getRole(), Permission.VIEW_ALL_SALES);
        if (!puedeRegistrar && !puedeConsultar) {
            throw new SecurityException("No tienes permiso para acceder a ventas.");
        }
        buildInterface();
        btnRegistrar.setEnabled(puedeRegistrar);
        txtDescuento.setEnabled(PermissionPolicy.has(
                currentUser.getRole(), Permission.APPLY_DISCOUNT));
        if (!puedeRegistrar) {
            btnAgregar.setEnabled(false);
            btnQuitar.setEnabled(false);
            cmbCliente.setEnabled(false);
            cmbItem.setEnabled(false);
            spCantidad.setEnabled(false);
            txtBuscar.setEnabled(false);
        }
        loadInitialData();
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void buildInterface() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Styles.BACKGROUND);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel superior = new JPanel(new BorderLayout(10, 10));
        superior.setOpaque(false);
        superior.add(buildForm(), BorderLayout.NORTH);
        superior.add(buildCart(), BorderLayout.CENTER);

        add(superior, BorderLayout.CENTER);
        add(buildHistory(), BorderLayout.SOUTH);

        btnAgregar.addActionListener(e -> addItem());
        btnQuitar.addActionListener(e -> removeItem());
        btnRegistrar.setBackground(new Color(22, 163, 74));
        btnRegistrar.setForeground(Color.WHITE);
        btnRegistrar.setFont(
                btnRegistrar.getFont().deriveFont(Font.BOLD)
        );

        btnRegistrar.addActionListener(e -> registerSale());
        btnActualizar.addActionListener(e -> loadHistory());
        txtBuscar.addActionListener(e -> loadCatalog());
        txtDescuento.addActionListener(e -> updateTotals());

        cmbItem.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,
                    Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof CommercialItem item) {
                    setText(item.getType() + " | " + item.getCode() + " | "
                            + item.getName() + " — " + money(item.getSalePrice()));
                }
                return this;
            }
        });
        tablaCarrito.setRowHeight(25);
        tablaHistorial.setRowHeight(24);
        tablaHistorial.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        btnVerBoleta.addActionListener(e -> showSelectedReceipt());
    }

    private JPanel buildForm() {
        JPanel panel = card("Datos de la venta");
        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0; c.gridx = 0;
        campos.add(new JLabel("Cliente:"), c);
        c.gridx = 1; c.weightx = 1;
        campos.add(cmbCliente, c);
        c.gridx = 2; c.weightx = 0;
        campos.add(new JLabel("Buscar producto/servicio:"), c);
        c.gridx = 3; c.weightx = 1;
        campos.add(txtBuscar, c);
        JButton btnBuscar = Styles.actionButton("Buscar");
        btnBuscar.addActionListener(e -> loadCatalog());
        c.gridx = 4; c.weightx = 0;
        campos.add(btnBuscar, c);

        c.gridy = 1; c.gridx = 0;
        campos.add(new JLabel("Ítem:"), c);
        c.gridx = 1; c.weightx = 1;
        campos.add(cmbItem, c);
        c.gridx = 2; c.weightx = 0;
        campos.add(new JLabel("Cantidad:"), c);
        c.gridx = 3;
        campos.add(spCantidad, c);
        c.gridx = 4;
        campos.add(btnAgregar, c);

        panel.add(campos, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCart() {
        JPanel panel = card("Detalle de la venta");
        tablaCarrito.setFillsViewportHeight(true);
        panel.add(new JScrollPane(tablaCarrito), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(10, 5));
        pie.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        actions.add(btnQuitar);
        actions.add(new JLabel("Descuento (S/):"));
        txtDescuento.setHorizontalAlignment(JTextField.RIGHT);
        actions.add(txtDescuento);
        actions.add(btnRegistrar);
        pie.add(actions, BorderLayout.CENTER);

        JPanel totales = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 4));
        totales.setOpaque(false);
        totales.add(new JLabel("Subtotal:"));
        totales.add(lblSubtotal);
        totales.add(new JLabel("Descuento:"));
        totales.add(lblDescuento);
        JLabel totalTitulo = new JLabel("TOTAL:");
        totalTitulo.setFont(totalTitulo.getFont().deriveFont(Font.BOLD));
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 17f));
        totales.add(totalTitulo);
        totales.add(lblTotal);
        pie.add(totales, BorderLayout.SOUTH);
        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildHistory() {
        JPanel panel = card("Historial reciente de ventas");
        panel.setPreferredSize(new Dimension(100, 220));
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        barra.setOpaque(false);
        barra.add(btnActualizar);
        barra.add(btnVerBoleta);
        panel.add(barra, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);
        return panel;
    }

    private JPanel card(String title) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                Styles.cardBorder(12, 12),
                BorderFactory.createTitledBorder(title)));
        return p;
    }

    private void loadInitialData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void, Void>() {
            List<Customer> customers;
            List<CommercialItem> catalogo;
            @Override protected Void doInBackground() throws Exception {
                if (PermissionPolicy.has(currentUser.getRole(), Permission.REGISTER_SALES)) {
                    var data = facade.prepareSale(currentUser);
                    customers = data.customers();
                    catalogo = data.catalog();
                }
                return null;
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    if (customers != null) for (Customer customer : customers) {
                        cmbCliente.addItem(new CustomerOption(customer));
                    }
                    if (catalogo != null) for (CommercialItem item : catalogo) cmbItem.addItem(item);
                    loadHistory();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError("Se interrumpió la carga de ventas.");
                } catch (ExecutionException ex) {
                    showError(message(ex.getCause()));
                }
            }
        }.execute();
    }

    private void loadCatalog() {
        try {
            List<CommercialItem> items = facade.searchCatalog(currentUser, txtBuscar.getText());
            cmbItem.removeAllItems();
            for (CommercialItem item : items) cmbItem.addItem(item);
            cmbItem.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list,
                        Object value, int index, boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    if (value instanceof CommercialItem item) {
                        setText(item.getType() + " | " + item.getCode() + " | "
                                + item.getName() + " — " + money(item.getSalePrice()));
                    }
                    return this;
                }
            });
        } catch (SQLException | SecurityException ex) {
            showError(message(ex));
        }
    }

    private void addItem() {
        CommercialItem item = (CommercialItem) cmbItem.getSelectedItem();
        if (item == null) {
            showError("Selecciona un producto o servicio.");
            return;
        }
        int quantity = (Integer) spCantidad.getValue();
        for (int i = 0; i < cart.size(); i++) {
            CartLine line = cart.get(i);
            if (line.item().getId() == item.getId()
                    && line.item().getType() == item.getType()) {
                cart.set(i, new CartLine(line.item(), line.quantity() + quantity));
                refreshCart();
                return;
            }
        }
        cart.add(new CartLine(item, quantity));
        refreshCart();
    }

    private void removeItem() {
        int row = tablaCarrito.getSelectedRow();
        if (row < 0) {
            showError("Selecciona una línea del carrito para quitarla.");
            return;
        }
        cart.remove(row);
        refreshCart();
    }

    private void refreshCart() {
        modeloCarrito.setRowCount(0);
        for (CartLine line : cart) {
            modeloCarrito.addRow(new Object[]{
                    line.item().getType(), line.item().getCode(),
                    line.item().getName(), line.quantity(),
                    money(line.item().getSalePrice()), money(line.subtotal())
            });
        }
        updateTotals();
    }

    private BigDecimal readDiscount() {
        String value = txtDescuento.getText().trim().replace(",", ".");
        if (value.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Escribe un descuento válido. Ejemplo: 10.50");
        }
    }

    private void updateTotals() {
        BigDecimal subtotal = cart.stream().map(CartLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount;
        try {
            discount = readDiscount();
            if (discount.signum() < 0 || discount.compareTo(subtotal) > 0) {
                throw new IllegalArgumentException("El descuento debe estar entre S/ 0.00 y el subtotal.");
            }
            lblDescuento.setText(money(discount));
            lblTotal.setText(money(subtotal.subtract(discount)));
        } catch (IllegalArgumentException ex) {
            lblDescuento.setText("Inválido");
            lblTotal.setText(money(subtotal));
        }
        lblSubtotal.setText(money(subtotal));
    }

    private void registerSale() {
        CustomerOption opcion = (CustomerOption) cmbCliente.getSelectedItem();
        if (opcion == null) {
            showError("Registra o selecciona un cliente activo.");
            return;
        }
        if (cart.isEmpty()) {
            showError("Agrega al menos un producto o servicio.");
            return;
        }
        final BigDecimal discount;
        try {
            discount = readDiscount();
            updateTotals();
            BigDecimal subtotal = cart.stream().map(CartLine::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (discount.signum() < 0 || discount.compareTo(subtotal) > 0) {
                throw new IllegalArgumentException("El descuento no es válido.");
            }
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        List<SaleService.RequestedLine> request = cart.stream()
                .map(l -> new SaleService.RequestedLine(l.item(), l.quantity())).toList();
        btnRegistrar.setEnabled(false);
        new SwingWorker<Sale, Void>() {
            @Override protected Sale doInBackground() throws Exception {
                return facade.registerSale(currentUser, opcion.customer().getId(),
                        request, discount);
            }
            @Override protected void done() {
                btnRegistrar.setEnabled(PermissionPolicy.has(
                        currentUser.getRole(), Permission.REGISTER_SALES));
                try {
                    Sale sale = get();
                    JOptionPane.showMessageDialog(SalesPanel.this,
                            "Venta registrada correctamente.\nBoleta: "
                                    + sale.getReceiptNumber() + "\nTotal: "
                                    + money(sale.getTotal()),
                            "Venta registrada", JOptionPane.INFORMATION_MESSAGE);
                    new ReceiptDialog(
                            SwingUtilities.getWindowAncestor(SalesPanel.this),
                            sale).setVisible(true);
                    cart.clear();
                    txtDescuento.setText("0.00");
                    refreshCart();
                    loadCatalog();
                    loadHistory();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError("Se interrumpió el registro de la venta.");
                } catch (ExecutionException ex) {
                    showError(message(ex.getCause()));
                }
            }
        }.execute();
    }

    private void loadHistory() {
        boolean puedeConsultar = PermissionPolicy.has(
                currentUser.getRole(), Permission.VIEW_OWN_SALES)
                || PermissionPolicy.has(currentUser.getRole(), Permission.VIEW_ALL_SALES);
        if (!puedeConsultar) return;
        new SwingWorker<List<Sale>, Void>() {
            @Override protected List<Sale> doInBackground() throws Exception {
                return facade.listSales(currentUser);
            }
            @Override protected void done() {
                try {
                    loadedSales = get();
                    modeloHistorial.setRowCount(0);
                    for (Sale sale : loadedSales) {
                        modeloHistorial.addRow(new Object[]{
                                sale.getId(), sale.getReceiptNumber(),
                                sale.getCustomerName(), sale.getDate(),
                                money(sale.getSubtotal()), money(sale.getDiscount()),
                                money(sale.getTotal()), sale.getStatus()
                        });
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showError(message(ex.getCause()));
                }
            }
        }.execute();
    }

    private void showSelectedReceipt() {
        int filaVista = tablaHistorial.getSelectedRow();
        if (filaVista < 0) {
            showError("Selecciona una venta del historial para ver su boleta.");
            return;
        }

        int filaModelo = tablaHistorial.convertRowIndexToModel(filaVista);
        if (filaModelo < 0 || filaModelo >= loadedSales.size()) {
            showError("No se pudo identificar la venta seleccionada.");
            return;
        }

        Sale sale = loadedSales.get(filaModelo);
        new ReceiptDialog(
                SwingUtilities.getWindowAncestor(this), sale).setVisible(true);
    }

    private static String money(BigDecimal value) {
        return CURRENCY.format(value == null ? BigDecimal.ZERO : value);
    }

    private void showError(String text) {
        JOptionPane.showMessageDialog(this, text, "TecnoStock",
                JOptionPane.WARNING_MESSAGE);
    }

    private static String message(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof SecurityException) {
            return error.getMessage();
        }
        LOG.log(Level.WARNING, "Error en el módulo de ventas", error);
        return "No se pudo completar la operación. Revisa la conexión y los datos.";
    }
}