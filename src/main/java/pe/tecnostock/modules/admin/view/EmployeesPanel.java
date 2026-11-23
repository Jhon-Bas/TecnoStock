
package pe.tecnostock.modules.admin.view;

import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.admin.repository.EmployeeRepository;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.modules.admin.service.EmployeeService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel administrativo para gestionar empleados.
 *
 * Permite crear, consultar, actualizar, desactivar y
 * reactivar cuentas. Incluye búsqueda y paginación SQL.
 */
public class EmployeesPanel extends JPanel {

    private static final int PAGE_SIZE = 10;

    private final User currentUser;

    private final EmployeeService service =
            new EmployeeService(
                    new EmployeeRepository(),
                    new UserRepository()
            );

    private final JTextField firstNameField = new JTextField();
    private final JTextField lastNameField = new JTextField();
    private final JTextField documentField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField searchField = new JTextField(20);

    private final JComboBox<Role> roleBox =
            new JComboBox<>(new Role[]{
                    Role.GERENTE, Role.EMPLEADO
            });

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[]{
                            "ID", "Nombres", "Apellidos", "DNI",
                            "Usuario", "Rol", "Estado",
                            "Creado por", "Fecha creación",
                            "Última modificación"
                    }, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };

    private final JTable employeeTable =
            new JTable(tableModel);

    private final JLabel pageLabel =
            new JLabel("Página 1 de 1");

    private final JButton previousButton =
            new JButton("Anterior");

    private final JButton nextButton =
            new JButton("Siguiente");

    private final JButton updateButton =
            new JButton("Actualizar");

    private final JButton statusButton =
            new JButton("Desactivar");

    private final List<EmployeeRepository.EmployeeRow> visibleRows =
            new ArrayList<>();

    private int currentPage = 0;
    private int selectedId = -1;
    private boolean selectedActive = true;

    /**
     * Construye la pantalla para el administrador.
     */
    public EmployeesPanel(User currentUser) {

        if (currentUser == null
                || currentUser.getRole() != Role.ADMIN) {
            throw new SecurityException(
                    "Acceso exclusivo del administrador."
            );
        }

        this.currentUser = currentUser;


        initializeComponents();
        loadEmployees();
    }

    /**
     * Organiza el formulario, la tabla y la paginación.
     */
    private void initializeComponents() {

        JPanel mainPanel = new JPanel(
                new BorderLayout(12, 12)
        );

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        );

        JLabel title = new JLabel("GESTIÓN DE EMPLEADOS");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));

        JPanel form = new JPanel(
                new GridLayout(6, 2, 8, 8)
        );

        form.setBorder(
                BorderFactory.createTitledBorder(
                        "Datos del empleado"
                )
        );

        form.add(new JLabel("Nombres:"));
        form.add(firstNameField);
        form.add(new JLabel("Apellidos:"));
        form.add(lastNameField);
        form.add(new JLabel("DNI:"));
        form.add(documentField);
        form.add(new JLabel("Usuario:"));
        form.add(usernameField);
        form.add(new JLabel("Contraseña (solo nuevo):"));
        form.add(passwordField);
        form.add(new JLabel("Rol:"));
        form.add(roleBox);

        JButton createButton =
                new JButton("Registrar");

        JButton newButton =
                new JButton("Nuevo / Limpiar");

        createButton.addActionListener(e -> createEmployee());
        newButton.addActionListener(e -> clearForm());

        updateButton.addActionListener(e -> updateEmployee());
        statusButton.addActionListener(e -> changeStatus());

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        actions.add(newButton);
        actions.add(createButton);
        actions.add(updateButton);
        actions.add(statusButton);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(title, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);

        JPanel searchPanel = new JPanel(
                new BorderLayout(10, 0)
        );

        JButton searchButton = new JButton("Buscar");

        searchButton.addActionListener(e -> {
            currentPage = 0;
            loadEmployees();
        });

        searchField.addActionListener(e -> {
            currentPage = 0;
            loadEmployees();
        });

        searchPanel.add(
                new JLabel("Buscar por nombre, DNI o usuario:"),
                BorderLayout.WEST
        );
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        employeeTable.setRowHeight(27);
        employeeTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        // Mantiene el orden proporcionado por SQLite.
        employeeTable.setAutoCreateRowSorter(false);

        employeeTable.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        selectEmployee();
                    }
                });

        JScrollPane scrollPane =
                new JScrollPane(employeeTable);

        JPanel pagination = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        previousButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadEmployees();
            }
        });

        nextButton.addActionListener(e -> {
            currentPage++;
            loadEmployees();
        });

        pagination.add(previousButton);
        pagination.add(pageLabel);
        pagination.add(nextButton);

        JPanel listing = new JPanel(
                new BorderLayout(8, 8)
        );

        listing.add(searchPanel, BorderLayout.NORTH);
        listing.add(scrollPane, BorderLayout.CENTER);
        listing.add(pagination, BorderLayout.SOUTH);

        mainPanel.add(top, BorderLayout.NORTH);
        mainPanel.add(listing, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        clearForm();
    }

    /**
     * Consulta la página actual y actualiza la tabla.
     */
    private void loadEmployees() {

        try {
            var result = service.listEmployees(
                    currentUser,
                    searchField.getText(),
                    currentPage,
                    PAGE_SIZE
            );

            int totalPages = Math.max(
                    1,
                    (result.total() + PAGE_SIZE - 1) / PAGE_SIZE
            );

            // Ajusta la página si se quedó fuera del rango.
            if (currentPage >= totalPages) {
                currentPage = totalPages - 1;
                loadEmployees();
                return;
            }

            visibleRows.clear();
            visibleRows.addAll(result.employees());

            tableModel.setRowCount(0);

            for (var employee : visibleRows) {

                tableModel.addRow(new Object[]{
                        employee.id(),
                        employee.firstName(),
                        employee.lastName(),
                        employee.document(),
                        employee.username(),
                        employee.role(),
                        employee.active() ? "ACTIVO" : "INACTIVO",
                        employee.createdBy(),
                        employee.createdAt(),
                        employee.updatedAt() == null
                                ? "Sin modificaciones"
                                : employee.updatedAt()
                });
            }

            pageLabel.setText(
                    "Página " + (currentPage + 1)
                    + " de " + totalPages
                    + " | Total: " + result.total()
            );

            previousButton.setEnabled(currentPage > 0);
            nextButton.setEnabled(currentPage < totalPages - 1);

            clearForm();

        } catch (SQLException | SecurityException ex) {
            showError(ex);
        }
    }

    /**
     * Carga los datos de la fila seleccionada.
     */
    private void selectEmployee() {

        int row = employeeTable.getSelectedRow();

        if (row < 0 || row >= visibleRows.size()) {
            return;
        }

        var employee = visibleRows.get(row);

        selectedId = employee.id();
        selectedActive = employee.active();

        firstNameField.setText(employee.firstName());
        lastNameField.setText(employee.lastName());
        documentField.setText(employee.document());
        usernameField.setText(employee.username());
        passwordField.setText("");

        // La contraseña solo se establece al crear cuentas.
        passwordField.setEnabled(false);

        if ("ADMIN".equals(employee.role())) {
            roleBox.setSelectedItem(Role.GERENTE);
            updateButton.setEnabled(false);
            statusButton.setEnabled(false);
            return;
        }

        roleBox.setSelectedItem(
                Role.valueOf(employee.role())
        );

        updateButton.setEnabled(true);
        statusButton.setEnabled(true);
        statusButton.setText(
                selectedActive ? "Desactivar" : "Reactivar"
        );
    }

    /**
     * Registra un nuevo empleado.
     */
    private void createEmployee() {

        if (selectedId != -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Presiona Nuevo / Limpiar antes de registrar."
            );
            return;
        }

        char[] password = passwordField.getPassword();

        try {
            service.createEmployee(
                    currentUser,
                    firstNameField.getText(),
                    lastNameField.getText(),
                    documentField.getText().trim(),
                    usernameField.getText().trim(),
                    (Role) roleBox.getSelectedItem(),
                    password
            );

            JOptionPane.showMessageDialog(
                    this, "Empleado registrado correctamente."
            );

            currentPage = 0;
            loadEmployees();

        } catch (SQLException | RuntimeException ex) {
            showError(ex);

        } finally {
            java.util.Arrays.fill(password, '\0');
            passwordField.setText("");
        }
    }

    /**
     * Actualiza los datos del empleado seleccionado.
     */
    private void updateEmployee() {

        if (selectedId < 0) {
            return;
        }

        try {
            service.updateEmployee(
                    currentUser,
                    selectedId,
                    firstNameField.getText(),
                    lastNameField.getText(),
                    documentField.getText().trim(),
                    usernameField.getText().trim(),
                    (Role) roleBox.getSelectedItem()
            );

            JOptionPane.showMessageDialog(
                    this, "Empleado actualizado correctamente."
            );

            loadEmployees();

        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    /**
     * Cambia el estado sin eliminar el registro.
     */
    private void changeStatus() {

        if (selectedId < 0) {
            return;
        }

        boolean newStatus = !selectedActive;

        String action = newStatus ? "reactivar" : "desactivar";

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas " + action + " esta cuenta?",
                "Confirmar operación",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.changeEmployeeStatus(
                    currentUser, selectedId, newStatus
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Estado actualizado correctamente."
            );

            loadEmployees();

        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    /**
     * Restablece el formulario al modo de registro.
     */
    private void clearForm() {

        selectedId = -1;
        selectedActive = true;

        employeeTable.clearSelection();

        firstNameField.setText("");
        lastNameField.setText("");
        documentField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        passwordField.setEnabled(true);

        roleBox.setSelectedItem(Role.EMPLEADO);

        updateButton.setEnabled(false);
        statusButton.setEnabled(false);
        statusButton.setText("Desactivar");
    }

    /**
     * Presenta errores de validación o persistencia.
     */
    private void showError(Exception ex) {

        String message = ex instanceof SQLException
                ? "No se pudo completar la operación. "
                  + "Verifica que el DNI y usuario no estén repetidos."
                : ex.getMessage();

        JOptionPane.showMessageDialog(
                this,
                message,
                "Operación no completada",
                JOptionPane.ERROR_MESSAGE
        );

        // Conserva el detalle técnico en la consola.
        ex.printStackTrace();
    }
}
