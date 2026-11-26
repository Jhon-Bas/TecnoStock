package pe.tecnostock.modules.sales.view;
import pe.tecnostock.modules.sales.command.CartCommandHistory;
import pe.tecnostock.modules.sales.command.CartCommandAction;

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

    private final JComboBox<CustomerOption> customerBox = new JComboBox<>();
    private final JComboBox<CommercialItem> cmbItem = new JComboBox<>();
    private final JSpinner quantitySpinner = new JSpinner(
            new SpinnerNumberModel(1, 1, 9999, 1));
    private final JTextField discountField = new JTextField("0.00", 9);
    private final JTextField searchField = new JTextField(16);
    private final JLabel lblSubtotal = new JLabel("S/ 0.00");
    private final JLabel discountLabel = new JLabel("S/ 0.00");
    private final JLabel lblTotal = new JLabel("S/ 0.00");

    private final DefaultTableModel cartModel = model(
            "Tipo", "Código", "Descripción", "Cant.", "P. unitario", "Importe");
    private final DefaultTableModel historyModel = model(
            "N.º", "Boleta", "Cliente", "Fecha", "Subtotal", "Descuento", "Total", "Estado");

    private final JTable cartTable = new JTable(cartModel);
    private final JTable historyTable = new JTable(historyModel);
    private final List<CartLine> cart = new ArrayList<>();
    private final CartCommandHistory cartHistory = new CartCommandHistory();
    private final JButton btnUndo = Styles.actionButton("Deshacer");
    private List<Sale> loadedSales = List.of();

    private final JButton addButton = Styles.actionButton("Agregar ítem");
    private final JButton removeButton = Styles.actionButton("Quitar seleccionado");
    private final JButton registerButton = Styles.actionButton("Registrar venta");
    private final JButton refreshButton = Styles.actionButton("Actualizar historial");
    private final JButton receiptButton = Styles.actionButton("Ver / imprimir boleta");

    public SalesPanel(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser, "El usuario es obligatorio.");
        boolean canRegister = PermissionPolicy.has(
                currentUser.getRole(), Permission.REGISTER_SALES);
        boolean canView = PermissionPolicy.has(currentUser.getRole(),
                Permission.VIEW_OWN_SALES)
                || PermissionPolicy.has(currentUser.getRole(), Permission.VIEW_ALL_SALES);
        if (!canRegister && !canView) {
            throw new SecurityException("No tienes permiso para acceder a ventas.");
        }
        buildInterface();
        registerButton.setEnabled(canRegister);
        discountField.setEnabled(PermissionPolicy.has(
                currentUser.getRole(), Permission.APPLY_DISCOUNT));
        if (!canRegister) {
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
            btnUndo.setEnabled(false);
            customerBox.setEnabled(false);
            cmbItem.setEnabled(false);
            quantitySpinner.setEnabled(false);
            searchField.setEnabled(false);
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

    /** Organiza los componentes y conecta las acciones de la pantalla. */
    private void buildInterface() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Styles.BACKGROUND);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel upperPanel = new JPanel(new BorderLayout(10, 10));
        upperPanel.setOpaque(false);
        upperPanel.add(buildForm(), BorderLayout.NORTH);
        upperPanel.add(buildCart(), BorderLayout.CENTER);

        add(upperPanel, BorderLayout.CENTER);
        add(buildHistory(), BorderLayout.SOUTH);

        // La boleta es el identificador comercial visible.
        historyTable.getColumnModel().getColumn(0)
                .setHeaderValue("ID");

        if (currentUser.getRole() != pe.tecnostock.model.Role.ADMIN) {
            historyTable.removeColumn(
                    historyTable.getColumnModel().getColumn(0));
        }

        addButton.addActionListener(e -> addItem());
        removeButton.addActionListener(e -> removeItem());
        btnUndo.addActionListener(e -> {
            if (cartHistory.undo()) refreshCart();
        });
        registerButton.setBackground(new Color(22, 163, 74));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFont(
                registerButton.getFont().deriveFont(Font.BOLD)
        );

        registerButton.addActionListener(e -> registerSale());
        refreshButton.addActionListener(e -> loadHistory());
        searchField.addActionListener(e -> loadCatalog());
        discountField.addActionListener(e -> updateTotals());

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
        cartTable.setRowHeight(25);
        historyTable.setRowHeight(24);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        receiptButton.addActionListener(e -> showSelectedReceipt());
    }

    private JPanel buildForm() {
        JPanel panel = card("Datos de la venta");
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0; c.gridx = 0;
        fields.add(new JLabel("Cliente:"), c);
        c.gridx = 1; c.weightx = 1;
        fields.add(customerBox, c);
        c.gridx = 2; c.weightx = 0;
        fields.add(new JLabel("Buscar producto/servicio:"), c);
        c.gridx = 3; c.weightx = 1;
        fields.add(searchField, c);
        JButton searchButton = Styles.actionButton("Buscar");
        searchButton.addActionListener(e -> loadCatalog());
        c.gridx = 4; c.weightx = 0;
        fields.add(searchButton, c);

        c.gridy = 1; c.gridx = 0;
        fields.add(new JLabel("Ítem:"), c);
        c.gridx = 1; c.weightx = 1;
        fields.add(cmbItem, c);
        c.gridx = 2; c.weightx = 0;
        fields.add(new JLabel("Cantidad:"), c);
        c.gridx = 3;
        fields.add(quantitySpinner, c);
        c.gridx = 4;
        fields.add(addButton, c);

        panel.add(fields, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCart() {
        JPanel panel = card("Detalle de la venta");
        cartTable.setFillsViewportHeight(true);
        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(10, 5));
        footer.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        actions.add(removeButton);
        actions.add(new JLabel("Descuento (S/):"));
        discountField.setHorizontalAlignment(JTextField.RIGHT);
        actions.add(discountField);
        actions.add(registerButton);
        footer.add(actions, BorderLayout.CENTER);

        JPanel totals = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 4));
        totals.setOpaque(false);
        totals.add(btnUndo);
        totals.add(new JLabel("Subtotal:"));
        totals.add(lblSubtotal);
        totals.add(new JLabel("Descuento:"));
        totals.add(discountLabel);
        JLabel totalTitle = new JLabel("TOTAL:");
        totalTitle.setFont(totalTitle.getFont().deriveFont(Font.BOLD));
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 17f));
        totals.add(totalTitle);
        totals.add(lblTotal);
        footer.add(totals, BorderLayout.SOUTH);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildHistory() {
        JPanel panel = card("Historial reciente de ventas");
        panel.setPreferredSize(new Dimension(100, 220));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        toolbar.setOpaque(false);
        toolbar.add(refreshButton);
        toolbar.add(receiptButton);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
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

    /** Carga clientes y catálogo en segundo plano mediante la fachada. */
    private void loadInitialData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void, Void>() {
            List<Customer> customers;
            List<CommercialItem> catalog;
            @Override protected Void doInBackground() throws Exception {
                if (PermissionPolicy.has(currentUser.getRole(), Permission.REGISTER_SALES)) {
                    var data = facade.prepareSale(currentUser);
                    customers = data.customers();
                    catalog = data.catalog();
                }
                return null;
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    if (customers != null) for (Customer customer : customers) {
                        customerBox.addItem(new CustomerOption(customer));
                    }
                    if (catalog != null) for (CommercialItem item : catalog) cmbItem.addItem(item);
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

    /** Actualiza el catálogo disponible según el filtro de búsqueda. */
    private void loadCatalog() {
        try {
            List<CommercialItem> items = facade.searchCatalog(currentUser, searchField.getText());
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

    /** Agrega un artículo al carrito mediante un comando reversible. */
    private void addItem() {
        CommercialItem item = (CommercialItem) cmbItem.getSelectedItem();
        if (item == null) {
            showError("Selecciona un producto o servicio.");
            return;
        }
        int quantity = (Integer) quantitySpinner.getValue();
        for (int i = 0; i < cart.size(); i++) {
            CartLine line = cart.get(i);
            if (line.item().getId() == item.getId()
                    && line.item().getType() == item.getType()) {
                final int index = i;
                cartHistory.execute(new CartCommandAction(
                        () -> cart.set(index, new CartLine(line.item(), Math.addExact(line.quantity(), quantity))),
                        () -> cart.set(index, line)));
                refreshCart();
                return;
            }
        }
        CartLine added = new CartLine(item, quantity);
        int index = cart.size();
        cartHistory.execute(new CartCommandAction(
                () -> cart.add(added), () -> cart.remove(index)));
        refreshCart();
    }

    /** Quita la línea seleccionada mediante un comando reversible. */
    private void removeItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            showError("Selecciona una línea del carrito para quitarla.");
            return;
        }
        int index = cartTable.convertRowIndexToModel(row);
        CartLine removed = cart.get(index);
        cartHistory.execute(new CartCommandAction(
                () -> cart.remove(index), () -> cart.add(index, removed)));
        refreshCart();
    }

    /** Actualiza la tabla del carrito y sus importes. */
    private void refreshCart() {
        cartModel.setRowCount(0);
        for (CartLine line : cart) {
            cartModel.addRow(new Object[]{
                    line.item().getType(), line.item().getCode(),
                    line.item().getName(), line.quantity(),
                    money(line.item().getSalePrice()), money(line.subtotal())
            });
        }
        updateTotals();
    }

    /** Lee el importe de descuento ingresado por el usuario. */
    private BigDecimal readDiscount() {
        String value = discountField.getText().trim().replace(",", ".");
        if (value.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Escribe un descuento válido. Ejemplo: 10.50");
        }
    }

    /** Calcula y presenta los importes actuales del carrito. */
    private void updateTotals() {
        BigDecimal subtotal = cart.stream().map(CartLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount;
        try {
            discount = readDiscount();
            if (discount.signum() < 0 || discount.compareTo(subtotal) > 0) {
                throw new IllegalArgumentException("El descuento debe estar entre S/ 0.00 y el subtotal.");
            }
            discountLabel.setText(money(discount));
            lblTotal.setText(money(subtotal.subtract(discount)));
        } catch (IllegalArgumentException ex) {
            discountLabel.setText("Inválido");
            lblTotal.setText(money(subtotal));
        }
        lblSubtotal.setText(money(subtotal));
    }

    /** Solicita el registro de la venta mediante la fachada en segundo plano. */
    private void registerSale() {
        CustomerOption option = (CustomerOption) customerBox.getSelectedItem();
        if (option == null) {
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
        registerButton.setEnabled(false);
        addButton.setEnabled(false);
        removeButton.setEnabled(false);
        btnUndo.setEnabled(false);
        new SwingWorker<Sale, Void>() {
            @Override protected Sale doInBackground() throws Exception {
                return facade.registerSale(currentUser, option.customer().getId(),
                        request, discount);
            }
            @Override protected void done() {
                boolean canEdit = PermissionPolicy.has(currentUser.getRole(), Permission.REGISTER_SALES);
                addButton.setEnabled(canEdit);
                removeButton.setEnabled(canEdit);
                btnUndo.setEnabled(canEdit);
                registerButton.setEnabled(PermissionPolicy.has(
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
                    cartHistory.clear();
                    discountField.setText("0.00");
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

    /** Consulta las ventas permitidas para el usuario autenticado. */
    private void loadHistory() {
        boolean canView = PermissionPolicy.has(
                currentUser.getRole(), Permission.VIEW_OWN_SALES)
                || PermissionPolicy.has(currentUser.getRole(), Permission.VIEW_ALL_SALES);
        if (!canView) return;
        new SwingWorker<List<Sale>, Void>() {
            @Override protected List<Sale> doInBackground() throws Exception {
                return facade.listSales(currentUser);
            }
            @Override protected void done() {
                try {
                    loadedSales = get();
                    historyModel.setRowCount(0);
                    for (Sale sale : loadedSales) {
                        historyModel.addRow(new Object[]{
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

    /** Abre el comprobante de la venta seleccionada. */
    private void showSelectedReceipt() {
        int viewRow = historyTable.getSelectedRow();
        if (viewRow < 0) {
            showError("Selecciona una venta del historial para ver su boleta.");
            return;
        }

        int modelRow = historyTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= loadedSales.size()) {
            showError("No se pudo identificar la venta seleccionada.");
            return;
        }

        Sale sale = loadedSales.get(modelRow);
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