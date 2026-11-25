
package pe.tecnostock.view;

import pe.tecnostock.service.BootstrapService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Formulario utilizado para crear la primera cuenta
 * administradora de TecnoStock.
 *
 * Solo se muestra cuando no existen usuarios.
 */
public class SetupFrame extends JFrame {

    private final BootstrapService bootstrapService;

    private final JTextField firstNameField =
            new JTextField();
    private final JTextField lastNameField =
            new JTextField();
    private final JTextField documentField =
            new JTextField();
    private final JTextField usernameField =
            new JTextField();
    private final JPasswordField passwordField =
            new JPasswordField();

    public SetupFrame(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;

        setTitle("TecnoStock - Configuración inicial");
        setSize(460, 440);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initializeComponents();
    }

    /**
     * Construye los campos y controles del formulario.
     */
    private void initializeComponents() {

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 15));
        panel.setBorder(
                BorderFactory.createEmptyBorder(30, 25, 30, 25)
        );

        panel.add(new JLabel("Nombres:"));
        panel.add(firstNameField);

        panel.add(new JLabel("Apellidos:"));
        panel.add(lastNameField);

        panel.add(new JLabel("DNI:"));
        panel.add(documentField);

        panel.add(new JLabel("Usuario:"));
        panel.add(usernameField);

        panel.add(new JLabel("Contraseña:"));
        panel.add(passwordField);

        JButton saveButton =
                new JButton("Crear administrador");

        saveButton.addActionListener(
                event -> createAdministrator()
        );

        panel.add(new JLabel());
        panel.add(saveButton);

        add(panel);
    }

    /**
     * Valida y registra al primer administrador.
     * Después muestra el formulario de inicio de sesión.
     */
    private void createAdministrator() {

        char[] password = passwordField.getPassword();

        try {
            bootstrapService.createInitialAdmin(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    documentField.getText().trim(),
                    usernameField.getText().trim(),
                    password
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Administrador creado correctamente."
            );

            new LoginFrame().setVisible(true);
            dispose();

        } catch (IllegalArgumentException |
                 IllegalStateException e) {

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Validación",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo guardar el administrador. "
                    + "Verifica el DNI y los datos ingresados.",
                    "Error de base de datos",
                    JOptionPane.ERROR_MESSAGE
            );

            java.util.logging.Logger.getLogger(SetupFrame.class.getName()).log(java.util.logging.Level.SEVERE, "Error durante la operacion.", e);

        } finally {
            Arrays.fill(password, '\0');
            passwordField.setText("");
        }
    }
}
