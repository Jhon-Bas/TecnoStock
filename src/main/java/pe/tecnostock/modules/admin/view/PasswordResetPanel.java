
package pe.tecnostock.modules.admin.view;

import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.modules.admin.service.PasswordResetService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Formulario administrativo para restablecer contraseñas.
 *
 * Permite buscar empleados, seleccionar una cuenta
 * por su ID y actualizar sus credenciales.
 */
public class PasswordResetPanel extends JPanel {

    private final User administrator;

    private final PasswordResetService service =
            new PasswordResetService();

    private final JTextField searchField =
            new JTextField();

    private final JTextField selectedUserField =
            new JTextField();

    private final JPasswordField passwordField =
            new JPasswordField();

    private final JPasswordField confirmationField =
            new JPasswordField();

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[]{
                        "ID", "Nombres", "Apellidos",
                        "DNI", "Usuario", "Rol", "Estado"
                    }, 0
            ) {
                @Override
                public boolean isCellEditable(
                        int row, int column) {
                    return false;
                }
            };

    private final JTable usersTable =
            new JTable(tableModel);

    private final List<UserRepository.ResetCandidate> results =
            new ArrayList<>();

    private final JButton resetButton =
            new JButton("Restablecer contraseña");

    private final JButton searchButton =
            new JButton("Buscar");

    private int selectedUserId = -1;
    private String selectedUsername = "";

    /**
     * Construye el formulario exclusivo
     * para administradores.
     */
    public PasswordResetPanel(User administrator) {

        if (administrator == null
                || administrator.getRole() != Role.ADMIN) {
            throw new SecurityException(
                    "Acceso exclusivo del administrador."
            );
        }

        this.administrator = administrator;


        initializeComponents();
        searchUsers();
    }

    /**
     * Construye el buscador, la tabla y los campos
     * necesarios para cambiar la contraseña.
     */
    private void initializeComponents() {

        JPanel mainPanel =
                new JPanel(new BorderLayout(15, 15));

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 20, 20, 20
                )
        );

        JLabel title =
                new JLabel("RESTABLECER CONTRASEÑAS");

        title.setFont(
                new Font("SansSerif", Font.BOLD, 23)
        );

        JLabel description = new JLabel(
                "Selecciona un gerente o empleado "
                + "desde la lista."
        );

        JPanel header =
                new JPanel(new BorderLayout(5, 5));

        header.add(title, BorderLayout.NORTH);
        header.add(description, BorderLayout.SOUTH);

        JPanel searchPanel =
                new JPanel(new BorderLayout(10, 0));

        searchField.putClientProperty(
                "JTextField.placeholderText",
                "Nombre, apellido, DNI o usuario"
        );

        searchButton.addActionListener(
                event -> searchUsers()
        );

        searchField.addActionListener(
                event -> searchUsers()
        );

        searchPanel.add(
                searchField, BorderLayout.CENTER
        );

        searchPanel.add(
                searchButton, BorderLayout.EAST
        );

        usersTable.setRowHeight(28);

        usersTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        // Conserva el orden de la lista consultada.
        usersTable.setAutoCreateRowSorter(false);

        usersTable.getSelectionModel()
                .addListSelectionListener(event -> {

                    if (!event.getValueIsAdjusting()) {
                        selectUser();
                    }
                });

        JScrollPane scrollPane =
                new JScrollPane(usersTable);

        JPanel listPanel =
                new JPanel(new BorderLayout(8, 8));

        listPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Cuentas disponibles (máximo 50)"
                )
        );

        listPanel.add(searchPanel, BorderLayout.NORTH);
        listPanel.add(scrollPane, BorderLayout.CENTER);

        selectedUserField.setEditable(false);

        JPanel passwordPanel =
                new JPanel(new GridLayout(3, 2, 10, 12));

        passwordPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Nueva contraseña"
                )
        );

        passwordPanel.add(
                new JLabel("Cuenta seleccionada:")
        );

        passwordPanel.add(selectedUserField);

        passwordPanel.add(
                new JLabel("Nueva contraseña:")
        );

        passwordPanel.add(passwordField);

        passwordPanel.add(
                new JLabel("Confirmar contraseña:")
        );

        passwordPanel.add(confirmationField);

        resetButton.setEnabled(false);

        resetButton.addActionListener(
                event -> resetPassword()
        );

        JPanel bottom =
                new JPanel(new BorderLayout(10, 10));

        bottom.add(passwordPanel, BorderLayout.CENTER);
        bottom.add(resetButton, BorderLayout.SOUTH);

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(listPanel, BorderLayout.CENTER);
        mainPanel.add(bottom, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Busca cuentas según el texto ingresado.
     *
     * Una búsqueda nueva limpia la selección anterior
     * para evitar modificar una cuenta equivocada.
     */
    private void searchUsers() {

        clearSelection();

        try {
            List<UserRepository.ResetCandidate> found =
                    service.searchUsers(
                            administrator,
                            searchField.getText()
                    );

            results.clear();
            results.addAll(found);

            tableModel.setRowCount(0);

            for (var user : results) {

                tableModel.addRow(new Object[]{
                        user.userId(),
                        user.firstName(),
                        user.lastName(),
                        user.document(),
                        user.username(),
                        user.role(),
                        user.active()
                                ? "ACTIVO"
                                : "INACTIVO"
                });
            }

        } catch (SQLException | SecurityException e) {

            results.clear();
            tableModel.setRowCount(0);

            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo consultar el listado: "
                    + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Obtiene el identificador real de la cuenta
     * correspondiente a la fila seleccionada.
     */
    private void selectUser() {

        int row = usersTable.getSelectedRow();

        if (row < 0 || row >= results.size()) {
            clearSelection();
            return;
        }

        var user = results.get(row);

        selectedUserId = user.userId();
        selectedUsername = user.username();

        selectedUserField.setText(
                user.username()
                + " (" + user.role() + ")"
        );

        resetButton.setEnabled(true);
    }

    /**
     * Solicita confirmación antes de cambiar
     * las credenciales de la cuenta seleccionada.
     */
    private void resetPassword() {

        if (selectedUserId <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Selecciona primero una cuenta."
            );
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "¿Restablecer la contraseña de "
                + selectedUsername + "?",
                "Confirmar operación",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        final int targetId = selectedUserId;

        final char[] password =
                passwordField.getPassword();

        final char[] repeatedPassword =
                confirmationField.getPassword();

        // Limpia los campos visuales antes de iniciar
        // el procesamiento de las credenciales.
        passwordField.setText("");
        confirmationField.setText("");

        resetButton.setEnabled(false);
        searchButton.setEnabled(false);

        // Ejecuta el cálculo del hash fuera del hilo
        // de Swing para mantener la panel responsiva.
        SwingWorker<Void, Void> worker =
                new SwingWorker<>() {

            @Override
            protected Void doInBackground()
                    throws Exception {

                try {
                    service.resetPassword(
                            administrator,
                            targetId,
                            password,
                            repeatedPassword
                    );
                    return null;

                } finally {
                    Arrays.fill(password, '\0');
                    Arrays.fill(repeatedPassword, '\0');
                }
            }

            @Override
            protected void done() {

                searchButton.setEnabled(true);

                try {
                    get();

                    JOptionPane.showMessageDialog(
                            PasswordResetPanel.this,
                            "Contraseña actualizada correctamente.",
                            "Operación exitosa",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    searchUsers();

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    showError(
                            "La operación fue interrumpida."
                    );

                } catch (ExecutionException e) {

                    Throwable cause = e.getCause();

                    if (cause instanceof IllegalArgumentException
                            || cause instanceof SecurityException) {

                        showError(cause.getMessage());

                    } else {
                        showError(
                                "No se pudo actualizar la contraseña."
                        );
                        java.util.logging.Logger.getLogger(PasswordResetPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Error durante la operacion.", cause);
                    }

                    // Permite corregir los campos y reintentar.
                    resetButton.setEnabled(selectedUserId > 0);
                }
            }
        };

        worker.execute();
    }

    /**
     * Elimina la selección actual de la cuenta.
     */
    private void clearSelection() {

        selectedUserId = -1;
        selectedUsername = "";

        selectedUserField.setText("");
        resetButton.setEnabled(false);

        usersTable.clearSelection();
    }

    /**
     * Muestra un error sin revelar datos sensibles.
     */
    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Operación no completada",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
