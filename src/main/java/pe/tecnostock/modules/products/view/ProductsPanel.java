package pe.tecnostock.modules.products.view;

import pe.tecnostock.model.Category;
import pe.tecnostock.model.CommercialItem;
import pe.tecnostock.model.Product;
import pe.tecnostock.model.ServiceItem;
import pe.tecnostock.model.ItemType;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.products.factory.CommercialItemFactory;
import pe.tecnostock.modules.products.factory.CommercialItemFactory.ItemData;
import pe.tecnostock.modules.products.repository.ProductRepository;
import pe.tecnostock.modules.products.repository.ServiceItemRepository;
import pe.tecnostock.modules.products.service.CatalogService;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.security.Permission;
import pe.tecnostock.security.PermissionPolicy;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catálogo de productos y servicios.
 *
 * El formulario elige el tipo y activa solo los campos que le
 * corresponden: un servicio no tiene precio de compra, stock mínimo
 * ni categoría. El tipo y los datos se entregan al servicio, que usa
 * la fábrica para crear el objeto correcto.
 *
 * Las existencias no se editan aquí: entran y salen por movimientos
 * de inventario. Los botones se habilitan según los permisos del rol,
 * pero cada operación vuelve a validarse en CatalogoService.
 */
public class ProductsPanel extends JPanel {

    private static final Logger LOGGER =
            Logger.getLogger(ProductsPanel.class.getName());

    private final User currentUser;
    private final boolean canManage;

    private final CatalogService service = new CatalogService(
            new ProductRepository(),
            new ServiceItemRepository(),
            new CommercialItemFactory(),
            new AuthorizationService()
    );

    private final JComboBox<ItemType> typeBox =
            new JComboBox<>(ItemType.values());
    private final JTextField codeField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField descriptionField = new JTextField();
    private final JTextField salePriceField = new JTextField();
    private final JTextField purchasePriceField = new JTextField();
    private final JTextField minimumStockField = new JTextField();
    private final JComboBox<Category> categoryBox = new JComboBox<>();

    private final JTextField searchField = new JTextField(22);
    private final JCheckBox inactiveCheck =
            new JCheckBox("Mostrar inactivos");

    private final JButton registerButton = new JButton("Registrar");
    private final JButton updateButton = new JButton("Actualizar");
    private final JButton statusButton = new JButton("Desactivar");
    private final JButton clearButton = new JButton("Limpiar");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {
                    "Tipo", "Código", "Nombre", "Categoría",
                    "P. compra", "P. venta", "Stock", "Estado", "ID"
            }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    /** Elementos que muestra la tabla, en el mismo orden que sus filas. */
    private final List<CommercialItem> visibleItems = new ArrayList<>();

    /** Elemento elegido en la tabla, o null si se está creando uno. */
    private CommercialItem selected;

    public ProductsPanel(User currentUser) {

        if (currentUser == null || !PermissionPolicy.has(
                currentUser.getRole(), Permission.VIEW_PRODUCTS)) {
            throw new SecurityException(
                    "No tienes permiso para consultar productos."
            );
        }

        this.currentUser = currentUser;
        this.canManage = PermissionPolicy.has(
                currentUser.getRole(), Permission.MANAGE_PRODUCTS);

        buildScreen();
        loadCategories();
        updateFormState();
        loadItems();
    }

    private void buildScreen() {

        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildForm(), BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);
    }

    private JPanel buildForm() {

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);

        addPair(campos, 0, 0, "Tipo:", typeBox);
        addPair(campos, 0, 1, "Código:", codeField);
        addPair(campos, 1, 0, "Nombre:", nameField);
        addPair(campos, 1, 1, "Descripción:", descriptionField);
        addPair(campos, 2, 0, "Precio de venta (S/):", salePriceField);
        addPair(campos, 2, 1, "Precio de compra (S/):", purchasePriceField);
        addPair(campos, 3, 0, "Stock mínimo:", minimumStockField);
        addPair(campos, 3, 1, "Categoría:", categoryBox);

        typeBox.addActionListener(e -> updateFormState());
        registerButton.addActionListener(e -> register());
        updateButton.addActionListener(e -> update());
        statusButton.addActionListener(e -> changeStatus());
        clearButton.addActionListener(e -> clearForm());

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0)
        );
        botones.setOpaque(false);
        botones.add(registerButton);
        botones.add(updateButton);
        botones.add(statusButton);
        botones.add(clearButton);

        JPanel formulario = new JPanel(new BorderLayout(0, 10));
        formulario.setOpaque(false);
        formulario.setBorder(BorderFactory.createTitledBorder(
                "Producto o servicio"));
        formulario.add(campos, BorderLayout.CENTER);
        formulario.add(botones, BorderLayout.SOUTH);

        return formulario;
    }

    /** Coloca etiqueta y campo en la fila y columna (0 o 1) indicadas. */
    private static void addPair(
            JPanel panel, int row, int column,
            String label, JComponent field
    ) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = column * 2;
        panel.add(new JLabel(label), c);

        c.gridx = column * 2 + 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    private JPanel buildList() {

        JButton buscarButton = new JButton("Buscar");
        buscarButton.addActionListener(e -> loadItems());
        searchField.addActionListener(e -> loadItems());
        inactiveCheck.setOpaque(false);
        inactiveCheck.addActionListener(e -> loadItems());

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        barra.add(new JLabel("Buscar por código o nombre:"));
        barra.add(searchField);
        barra.add(buscarButton);
        barra.add(inactiveCheck);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);

        // Conserva el ID en el modelo sin exponerlo a otros roles.
        if (currentUser.getRole() != pe.tecnostock.model.Role.ADMIN) {
            table.removeColumn(table.getColumn("ID"));
        } else {
            table.moveColumn(table.getColumnCount() - 1, 0);
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectRow();
            }
        });

        JPanel listado = new JPanel(new BorderLayout(0, 8));
        listado.setOpaque(false);
        listado.add(barra, BorderLayout.NORTH);
        listado.add(new JScrollPane(table), BorderLayout.CENTER);

        return listado;
    }

    private void loadCategories() {

        try {
            for (Category category : service.listCategories(currentUser)) {
                categoryBox.addItem(category);
            }

        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    /** Consulta en segundo plano para no bloquear la interfaz. */
    private void loadItems() {

        String text = searchField.getText();
        boolean inactivos = inactiveCheck.isSelected();

        new SwingWorker<List<CommercialItem>, Void>() {

            @Override
            protected List<CommercialItem> doInBackground()
                    throws SQLException {
                return service.search(currentUser, text, inactivos);
            }

            @Override
            protected void done() {

                try {
                    showItems(get());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                } catch (ExecutionException e) {
                    showError(e.getCause());
                }
            }
        }.execute();
    }

    private void showItems(List<CommercialItem> items) {

        tableModel.setRowCount(0);
        visibleItems.clear();

        for (CommercialItem item : items) {
            visibleItems.add(item);
            tableModel.addRow(row(item));
        }
    }

    private static Object[] row(CommercialItem item) {

        String estado = item.isActive() ? "Activo" : "Inactivo";

        return switch (item) {
            case Product p -> new Object[] {
                    p.getType(), p.getCode(), p.getName(),
                    p.getCategory().name(),
                    soles(p.getPurchasePrice()), soles(p.getSalePrice()),
                    p.getCurrentStock(), estado, p.getId()
            };
            case ServiceItem s -> new Object[] {
                    s.getType(), s.getCode(), s.getName(),
                    "—", "—", soles(s.getSalePrice()), "—", estado, s.getId()
            };
        };
    }

    private static String soles(BigDecimal amount) {
        return "S/ " + amount.toPlainString();
    }

    private void register() {

        try {
            service.register(
                    currentUser, selectedType(), readData(0));

            clearForm();
            loadItems();
            showInfo("Registrado correctamente.");

        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    private void update() {

        try {
            service.update(
                    currentUser, selected.getType(),
                    readData(selected.getId()));

            clearForm();
            loadItems();
            showInfo("Actualizado correctamente.");

        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    private void changeStatus() {

        boolean activar = !selected.isActive();

        int opcion = JOptionPane.showConfirmDialog(
                this,
                (activar ? "¿Reactivar " : "¿Desactivar ")
                        + selected.getName() + "?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );

        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.changeStatus(currentUser, selected, activar);

            clearForm();
            loadItems();

        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    private ItemType selectedType() {
        return (ItemType) typeBox.getSelectedItem();
    }

    /** Un servicio no usa precio de compra, stock mínimo ni categoría. */
    private ItemData readData(int id) {

        boolean esProducto = selectedType() == ItemType.PRODUCT;

        return new ItemData(
                id,
                codeField.getText(),
                nameField.getText(),
                descriptionField.getText(),
                readPrice(salePriceField, "precio de venta"),
                esProducto
                        ? readPrice(purchasePriceField, "precio de compra")
                        : null,
                esProducto ? readInteger(minimumStockField) : 0,
                esProducto ? (Category) categoryBox.getSelectedItem() : null
        );
    }

    /** Un campo vacío se entrega como null para que el modelo lo rechace. */
    private static BigDecimal readPrice(JTextField field, String name) {

        String text = field.getText().trim();

        if (text.isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(text.replace(',', '.'));

        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "El " + name + " debe ser un número, "
                    + "por ejemplo 129.90."
            );
        }
    }

    private static int readInteger(JTextField field) {

        String text = field.getText().trim();

        if (text.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(text);

        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "El stock mínimo debe ser un número entero."
            );
        }
    }

    private void selectRow() {

        int row = table.getSelectedRow();

        if (row < 0) {
            selected = null;
        } else {
            selected = visibleItems.get(row);
            fillForm(selected);
        }

        updateFormState();
    }

    private void fillForm(CommercialItem item) {

        typeBox.setSelectedItem(item.getType());
        codeField.setText(item.getCode());
        nameField.setText(item.getName());
        descriptionField.setText(
                item.getDescription() == null
                        ? "" : item.getDescription());
        salePriceField.setText(item.getSalePrice().toPlainString());

        switch (item) {
            case Product p -> {
                purchasePriceField.setText(
                        p.getPurchasePrice().toPlainString());
                minimumStockField.setText(
                        String.valueOf(p.getMinimumStock()));
                categoryBox.setSelectedItem(p.getCategory());
            }
            case ServiceItem s -> {
                purchasePriceField.setText("");
                minimumStockField.setText("");
            }
        }
    }

    private void clearForm() {

        table.clearSelection();
        selected = null;

        codeField.setText("");
        nameField.setText("");
        descriptionField.setText("");
        salePriceField.setText("");
        purchasePriceField.setText("");
        minimumStockField.setText("");

        updateFormState();
    }

    /**
     * Habilita cada control según el permiso, el tipo elegido y si hay
     * un elemento seleccionado (el tipo de uno existente no cambia).
     */
    private void updateFormState() {

        boolean hayElemento = selected != null;
        boolean esProducto = selectedType() == ItemType.PRODUCT;

        typeBox.setEnabled(canManage && !hayElemento);
        codeField.setEnabled(canManage);
        nameField.setEnabled(canManage);
        descriptionField.setEnabled(canManage);
        salePriceField.setEnabled(canManage);
        purchasePriceField.setEnabled(canManage && esProducto);
        minimumStockField.setEnabled(canManage && esProducto);
        categoryBox.setEnabled(canManage && esProducto);

        registerButton.setEnabled(canManage && !hayElemento);
        updateButton.setEnabled(canManage && hayElemento);
        statusButton.setEnabled(canManage && hayElemento);

        statusButton.setText(
                hayElemento && !selected.isActive()
                        ? "Reactivar" : "Desactivar"
        );
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(
                this, message, "TecnoStock",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /** Muestra los errores de validación; oculta los técnicos. */
    private void showError(Throwable error) {

        String message;

        if (error instanceof IllegalArgumentException
                || error instanceof SecurityException) {
            message = error.getMessage();
        } else {
            LOGGER.log(Level.WARNING, "Error en productos", error);
            message = "No se pudo completar la operación.";
        }

        JOptionPane.showMessageDialog(
                this, message, "TecnoStock",
                JOptionPane.WARNING_MESSAGE
        );
    }
}