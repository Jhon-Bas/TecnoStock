
package pe.tecnostock.view;

import pe.tecnostock.model.User;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.service.AuthService;
import pe.tecnostock.service.PasswordChangeService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Login único para administradores, gerentes y empleados.
 *
 * Comprueba las credenciales y dirige al usuario
 * al cambio obligatorio de contraseña cuando corresponde.
 */
public class LoginFrame extends JFrame {

    private final JTextField usernameField =
            new JTextField(20);

    private final JPasswordField passwordField =
            new JPasswordField(20);

    private final AuthService authService =
            new AuthService(new UserRepository());

    private final PasswordChangeService passwordChangeService =
            new PasswordChangeService();

    /**
     * Construye el formulario de autenticación.
     */
    public LoginFrame() {

        setTitle("TecnoStock - Iniciar sesión");
        setSize(420, 330);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initializeComponents();
    }

    /**
     * Organiza los campos y el botón de inicio.
     */
    private void initializeComponents() {

        JPanel panel =
                new JPanel(new GridLayout(4, 1, 10, 12));

        panel.setBorder(
                BorderFactory.createEmptyBorder(25, 35, 25, 35)
        );

        JLabel title = new JLabel(
                "INICIAR SESIÓN",
                SwingConstants.CENTER
        );

        title.setFont(
                new Font("SansSerif", Font.BOLD, 22)
        );

        JButton loginButton = new JButton("Ingresar");

        loginButton.addActionListener(
                event -> authenticate()
        );

        passwordField.addActionListener(
                event -> authenticate()
        );

        usernameField.putClientProperty(
                "JTextField.placeholderText",
                "Nombre de usuario"
        );

        passwordField.putClientProperty(
                "JTextField.placeholderText",
                "Contraseña"
        );

        panel.add(title);
        panel.add(usernameField);
        panel.add(passwordField);
        panel.add(loginButton);

        add(panel);
        getRootPane().setDefaultButton(loginButton);
    }

    /**
     * Autentica y decide cuál ventana debe abrir.
     */
    private void authenticate() {

        char[] password = passwordField.getPassword();

        try {
            Optional<User> account = authService.login(
                    usernameField.getText(),
                    password
            );

            if (account.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Usuario o contraseña incorrectos, "
                        + "o cuenta desactivada.",
                        "Acceso denegado",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            User currentUser = account.get();

            // Comprueba si la contraseña debe cambiarse
            // antes de permitir el acceso al sistema.
            boolean changeRequired =
                    passwordChangeService.mustChangePassword(
                            currentUser
                    );

            if (changeRequired) {
                new ChangePasswordFrame(currentUser)
                        .setVisible(true);
            } else {
                new MainFrame(currentUser)
                        .setVisible(true);
            }

            dispose();

        } catch (SQLException | SecurityException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo validar el acceso. "
                    + "Comprueba el estado de la cuenta.",
                    "Error de acceso",
                    JOptionPane.ERROR_MESSAGE
            );

            e.printStackTrace();

        } finally {
            // AuthService también limpia el arreglo,
            // pero se garantiza la limpieza en esta vista.
            Arrays.fill(password, '\0');
            passwordField.setText("");
        }
    }
}
