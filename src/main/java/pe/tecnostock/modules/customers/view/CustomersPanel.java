package pe.tecnostock.modules.customers.view;

import pe.tecnostock.model.Customer;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.customers.repository.CustomerRepository;
import pe.tecnostock.modules.customers.service.CustomerService;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.security.Permission;
import pe.tecnostock.security.PermissionPolicy;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registro, consulta y edición de clientes.
 *
 * Los botones se habilitan según los permisos del rol, pero eso
 * solo es comodidad: cada operación vuelve a validarse en
 * ClienteService.
 */
public class CustomersPanel extends JPanel {

    private static final Logger LOGGER =
            Logger.getLogger(CustomersPanel.class.getName());

    private final User currentUser;

    private final CustomerService service = new CustomerService(
            new CustomerRepository(),
            new AuthorizationService()
    );

    private final CustomerTableModel tableModel = new CustomerTableModel();
    private final JTable table = new JTable(tableModel);

    private final JTextField documentField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField addressField = new JTextField();

    private final JTextField searchField = new JTextField(22);
    private final JCheckBox inactiveCheck =
            new JCheckBox("Mostrar inactivos");

    private final JButton registerButton = new JButton("Registrar");
    private final JButton updateButton = new JButton("Actualizar");
    private final JButton statusButton = new JButton("Desactivar");
    private final JButton clearButton = new JButton("Limpiar");

    /** Cliente elegido en la tabla, o null si se está creando uno. */
    private Customer selected;

    /** Inicializa el panel con la sesión autenticada. */
    public CustomersPanel(User currentUser) {

        if (currentUser == null || !PermissionPolicy.has(
                currentUser.getRole(), Permission.VIEW_CUSTOMERS)) {
            throw new SecurityException(
                    "No tienes permiso para consultar clientes."
            );
        }

        this.currentUser = currentUser;

        buildScreen();
        updateButtons();
        loadCustomers();
    }

    /** Organiza los componentes del panel. */
    private void buildScreen() {

        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(buildForm(), BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);
    }

    /** Construye el formulario de datos personales. */
    private JPanel buildForm() {

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);

        addField(fields, 0, "Documento (DNI o RUC):", documentField);
        addField(fields, 1, "Nombre o razón social:", nameField);
        addField(fields, 2, "Teléfono:", phoneField);
        addField(fields, 3, "Correo:", emailField);
        addField(fields, 4, "Dirección:", addressField);

        registerButton.addActionListener(e -> register());
        updateButton.addActionListener(e -> update());
        statusButton.addActionListener(e -> changeStatus());
        clearButton.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0)
        );
        buttons.setOpaque(false);
        buttons.add(registerButton);
        buttons.add(updateButton);
        buttons.add(statusButton);
        buttons.add(clearButton);

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setOpaque(false);
        form.setBorder(
                BorderFactory.createTitledBorder("Datos del cliente")
        );
        form.add(fields, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);

        return form;
    }

    /** Añade un campo y su etiqueta al formulario. */
    private static void addField(
            JPanel panel, int row, String label, JTextField field
    ) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        panel.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    /** Construye el listado y los controles de búsqueda. */
    private JPanel buildList() {

        JButton searchButton = new JButton("Buscar");
        searchButton.addActionListener(e -> loadCustomers());
        searchField.addActionListener(e -> loadCustomers());
        inactiveCheck.setOpaque(false);
        inactiveCheck.addActionListener(e -> loadCustomers());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Buscar por documento o nombre:"));
        toolbar.add(searchField);
        toolbar.add(searchButton);
        toolbar.add(inactiveCheck);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);

        // El identificador interno solo es visible para el administrador.
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

        JPanel listing = new JPanel(new BorderLayout(0, 8));
        listing.setOpaque(false);
        listing.add(toolbar, BorderLayout.NORTH);
        listing.add(new JScrollPane(table), BorderLayout.CENTER);

        return listing;
    }

    /** Consulta en segundo plano para no bloquear la interfaz. */
    private void loadCustomers() {

        String text = searchField.getText();
        boolean inactive = inactiveCheck.isSelected();

        new SwingWorker<List<Customer>, Void>() {

            @Override
            protected List<Customer> doInBackground()
                    throws SQLException {
                return service.search(currentUser, text, inactive);
            }

            @Override
            protected void done() {

                try {
                    tableModel.setCustomers(get());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                } catch (ExecutionException e) {
                    showError(e.getCause());
                }
            }
        }.execute();
    }

    /** Registra un cliente tras comprobar los permisos y sus datos. */
    private void register() {

        try {
            service.register(currentUser, Customer.create(
                    documentField.getText(),
                    nameField.getText(),
                    phoneField.getText(),
                    emailField.getText(),
                    addressField.getText()
            ));

            clearForm();
            loadCustomers();
            showInfo("Cliente registrado correctamente.");

        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    /** Actualiza los datos del cliente seleccionado. */
    private void update() {

        try {
            service.update(currentUser, new Customer(
                    selected.getId(),
                    documentField.getText(),
                    nameField.getText(),
                    phoneField.getText(),
                    emailField.getText(),
                    addressField.getText(),
                    selected.isActive()
            ));

            clearForm();
            loadCustomers();
            showInfo("Cliente actualizado correctamente.");

        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    /** Actualiza el estado sin eliminar el registro. */
    private void changeStatus() {

        boolean activate = !selected.isActive();

        int option = JOptionPane.showConfirmDialog(
                this,
                (activate ? "¿Reactivar" : "¿Desactivar")
                        + " al cliente " + selected.getName() + "?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );

        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.changeStatus(currentUser, selected.getId(), activate);

            clearForm();
            loadCustomers();

        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    /** Carga los datos del cliente seleccionado. */
    private void selectRow() {

        int row = table.getSelectedRow();

        if (row < 0) {
            selected = null;
        } else {
            selected = tableModel.getCustomer(row);

            documentField.setText(selected.getDocument());
            nameField.setText(selected.getName());
            phoneField.setText(value(selected.getPhone()));
            emailField.setText(value(selected.getEmail()));
            addressField.setText(value(selected.getAddress()));
        }

        updateButtons();
    }

    /** Limpia los campos y restablece la selección. */
    private void clearForm() {

        table.clearSelection();
        selected = null;

        documentField.setText("");
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addressField.setText("");

        updateButtons();
    }

    /** Habilita acciones según la selección y los permisos. */
    private void updateButtons() {

        boolean hasCustomer = selected != null;

        registerButton.setEnabled(
                !hasCustomer && can(Permission.REGISTER_CUSTOMERS));
        updateButton.setEnabled(
                hasCustomer && can(Permission.MANAGE_CUSTOMERS));
        statusButton.setEnabled(
                hasCustomer && can(Permission.MANAGE_CUSTOMERS));

        statusButton.setText(
                hasCustomer && !selected.isActive()
                        ? "Reactivar" : "Desactivar"
        );
    }

    /** Consulta los permisos del rol de la sesión. */
    private boolean can(Permission permission) {
        return PermissionPolicy.has(currentUser.getRole(), permission);
    }

    /** Convierte un texto opcional en una cadena visible. */
    private static String value(String text) {
        return text == null ? "" : text;
    }

    /** Presenta una confirmación al usuario. */
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
            LOGGER.log(Level.WARNING, "Error en clientes", error);
            message = "No se pudo completar la operación.";
        }

        JOptionPane.showMessageDialog(
                this, message, "TecnoStock",
                JOptionPane.WARNING_MESSAGE
        );
    }
}