package pe.tecnostock.modules.inventory.view;

import pe.tecnostock.model.InventoryMovement;
import pe.tecnostock.model.Product;
import pe.tecnostock.model.MovementType;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.inventory.service.InventoryService;
import pe.tecnostock.security.Permission;
import pe.tecnostock.security.PermissionPolicy;
import pe.tecnostock.modules.inventory.repository.InventoryRepository;
import pe.tecnostock.modules.products.repository.ProductRepository;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.model.Role;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Panel integrado para registrar y consultar movimientos de stock. */
public class InventoryPanel extends JPanel {

    private record ProductOption(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final InventoryService inventoryService;
    private final User currentUser;

    private final JComboBox<ProductOption> productBox = new JComboBox<>();
    private final JComboBox<MovementType> typeBox = new JComboBox<>(
            MovementType.values()
    );
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 0, MovementType.MAX_STOCK, 1));
    private final JTextField reasonField = new JTextField(25);
    private final JButton registerButton = new JButton("Registrar movimiento");
    private final JButton refreshButton = new JButton("Actualizar");
    private final DefaultTableModel movementModel;

    /** Construye el panel con los servicios del módulo. */
    public InventoryPanel(User currentUser) {
        this(new InventoryService(new InventoryRepository(),
                new ProductRepository(), new AuthorizationService()), currentUser);
    }

    /** Construye el panel con un servicio proporcionado. */
    public InventoryPanel(InventoryService inventoryService, User currentUser) {
        this.inventoryService = Objects.requireNonNull(
                inventoryService,
                "El servicio de inventario es obligatorio."
        );
        this.currentUser = Objects.requireNonNull(
                currentUser,
                "El usuario de sesión es obligatorio."
        );

        this.movementModel = new DefaultTableModel(
                new String[]{
                        "ID", "Código de producto", "Tipo", "Cantidad",
                        "Stock Resultante", "Motivo", "Fecha", "Usuario"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        buildScreen();
        configurePermissions();
        loadProducts();
        loadMovements();
    }

    /** Construye el formulario y el historial. */
    private void buildScreen() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(12, 14, 12, 14),
                BorderFactory.createTitledBorder("Movimiento de inventario")
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Producto:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(productBox, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 3;
        formPanel.add(typeBox, gbc);

        gbc.gridx = 4;
        formPanel.add(new JLabel("Cantidad / Stock:"), gbc);
        gbc.gridx = 5;
        formPanel.add(quantitySpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Motivo:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 4;
        formPanel.add(reasonField, gbc);

        registerButton.addActionListener(e -> registerMovement());
        gbc.gridx = 5; gbc.gridwidth = 1;
        formPanel.add(registerButton, gbc);

        topPanel.add(formPanel, BorderLayout.CENTER);

        JPanel tablePanel = new JPanel(new BorderLayout(8, 8));
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(12, 14, 12, 14),
                BorderFactory.createTitledBorder("Historial de movimientos")
        ));

        JPanel tableToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tableToolbar.setOpaque(false);
        refreshButton.addActionListener(e -> {
            loadProducts();
            loadMovements();
        });
        tableToolbar.add(refreshButton);

        tablePanel.add(tableToolbar, BorderLayout.NORTH);
        JTable table = new JTable(movementModel);
        if (currentUser.getRole() != Role.ADMIN) {
            table.removeColumn(table.getColumnModel().getColumn(0));
        }
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
    }

    /** Configura los controles según el rol. */
    private void configurePermissions() {
        boolean canManage = currentUser.isActive()
                && PermissionPolicy.has(currentUser.getRole(), Permission.MANAGE_INVENTORY);
        productBox.setEnabled(canManage);
        typeBox.setEnabled(canManage);
        quantitySpinner.setEnabled(canManage);
        reasonField.setEditable(canManage);
        reasonField.setEnabled(canManage);
        registerButton.setEnabled(canManage);
    }

    /** Consulta los productos disponibles. */
    private void loadProducts() {
        Integer selectedId = getSelectedId();
        productBox.removeAllItems();
        try {
            List<Product> products = inventoryService.searchProducts(currentUser, "", false);
            for (Product prod : products) {
                ProductOption option = new ProductOption(
                        prod.getId(),
                        prod.getName() + " (Stock: " + prod.getCurrentStock() + ")"
                );
                productBox.addItem(option);
                if (selectedId != null && selectedId == prod.getId()) {
                    productBox.setSelectedItem(option);
                }
            }
        } catch (SecurityException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    /** Obtiene el identificador interno seleccionado. */
    private Integer getSelectedId() {
        ProductOption current = (ProductOption) productBox.getSelectedItem();
        return current != null ? current.id() : null;
    }

    /** Valida y registra el movimiento seleccionado. */
    private void registerMovement() {
        ProductOption selected = (ProductOption) productBox.getSelectedItem();
        if (selected == null) {
            showError("Seleccione un producto.");
            return;
        }

        MovementType type = (MovementType) typeBox.getSelectedItem();
        int quantity = (Integer) quantitySpinner.getValue();
        String reason = reasonField.getText().trim();

        try {
            inventoryService.register(currentUser, selected.id(), type, quantity, reason);
            JOptionPane.showMessageDialog(
                    this,
                    "Movimiento de inventario registrado correctamente.",
                    "Inventario actualizado",
                    JOptionPane.INFORMATION_MESSAGE
            );
            reasonField.setText("");
            quantitySpinner.setValue(1);
            loadProducts();
            loadMovements();
        } catch (IllegalArgumentException | SecurityException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    /** Actualiza el historial de movimientos. */
    private void loadMovements() {
        movementModel.setRowCount(0);
        try {
            List<InventoryMovement> movements = inventoryService.listMovements(currentUser, 0);
            for (InventoryMovement m : movements) {
                movementModel.addRow(new Object[]{
                        m.id(),
                        m.productCode(),
                        m.type(),
                        m.quantity(),
                        m.resultingStock(),
                        m.reason(),
                        m.date(),
                        m.username()
                        
                });
            }
        } catch (SecurityException | SQLException ex) {
            showError(ex.getMessage());
        }
    }

    /** Presenta un mensaje de error al usuario. */
    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                (message == null || message.isBlank()) ? "No se pudo completar la operación." : message,
                "Aviso",
                JOptionPane.WARNING_MESSAGE
        );
    }
}