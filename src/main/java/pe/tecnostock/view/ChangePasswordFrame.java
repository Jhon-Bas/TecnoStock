
package pe.tecnostock.view;

import pe.tecnostock.model.User;
import pe.tecnostock.service.PasswordChangeService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Permite reemplazar una contraseña temporal.
 *
 * Después del cambio solicita iniciar sesión de nuevo
 * para obtener una sesión con la versión actualizada.
 */
public class ChangePasswordFrame extends JFrame {

    private final User currentUser;

    private final PasswordChangeService service =
            new PasswordChangeService();

    private final JPasswordField oldPasswordField =
            new JPasswordField();

    private final JPasswordField newPasswordField =
            new JPasswordField();

    private final JPasswordField confirmationField =
            new JPasswordField();

    private final JButton saveButton =
            new JButton("Guardar nueva contraseña");

    private final JButton backButton =
            new JButton("Volver al login");

    /**
     * Construye el formulario de cambio obligatorio.
     */
    public ChangePasswordFrame(User currentUser) {

        this.currentUser = currentUser;

        setTitle("TecnoStock - Cambiar contraseña");
        setSize(490, 390);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initializeComponents();
    }

    /**
     * Construye el formulario y sus controles.
     */
    private void initializeComponents() {

        JPanel mainPanel =
                new JPanel(new BorderLayout(15, 15));

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        25, 30, 25, 30
                )
        );

        JLabel title = new JLabel(
                "CAMBIO OBLIGATORIO DE CONTRASEÑA",
                SwingConstants.CENTER
        );

        title.setFont(
                new Font("SansSerif", Font.BOLD, 17)
        );

        JLabel explanation = new JLabel(
                "<html><center>Tu contraseña es temporal. "
                + "Debes cambiarla antes de acceder "
                + "a TecnoStock.</center></html>",
                SwingConstants.CENTER
        );

        JPanel heading =
                new JPanel(new GridLayout(2, 1, 5, 5));

        heading.add(title);
        heading.add(explanation);

        JPanel form =
                new JPanel(new GridLayout(3, 2, 10, 15));

        form.add(new JLabel("Contraseña temporal:"));
        form.add(oldPasswordField);

        form.add(new JLabel("Nueva contraseña:"));
        form.add(newPasswordField);

        form.add(new JLabel("Confirmar contraseña:"));
        form.add(confirmationField);

        saveButton.addActionListener(
                event -> changePassword()
        );

        backButton.addActionListener(event -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel actions =
                new JPanel(new GridLayout(2, 1, 8, 8));

        actions.add(saveButton);
        actions.add(backButton);

        mainPanel.add(heading, BorderLayout.NORTH);
        mainPanel.add(form, BorderLayout.CENTER);
        mainPanel.add(actions, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        getRootPane().setDefaultButton(saveButton);
    }

    /**
     * Cambia la contraseña fuera del hilo visual
     * para evitar congelar la interfaz.
     */
    private void changePassword() {

        char[] oldPassword =
                oldPasswordField.getPassword();

        char[] newPassword =
                newPasswordField.getPassword();

        char[] confirmation =
                confirmationField.getPassword();

        oldPasswordField.setText("");
        newPasswordField.setText("");
        confirmationField.setText("");

        saveButton.setEnabled(false);
        backButton.setEnabled(false);

        SwingWorker<Void, Void> worker =
                new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {

                try {
                    service.changePassword(
                            currentUser,
                            oldPassword,
                            newPassword,
                            confirmation
                    );

                    return null;

                } finally {
                    // Limpia los arreglos de contraseñas.
                    Arrays.fill(oldPassword, '\0');
                    Arrays.fill(newPassword, '\0');
                    Arrays.fill(confirmation, '\0');
                }
            }

            @Override
            protected void done() {

                try {
                    get();

                    JOptionPane.showMessageDialog(
                            ChangePasswordFrame.this,
                            "Contraseña actualizada correctamente. "
                            + "Inicia sesión con tu nueva contraseña.",
                            "Cambio exitoso",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    // La contraseña cambió la versión de sesión.
                    // Se solicita autenticación nuevamente.
                    dispose();
                    new LoginFrame().setVisible(true);

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    JOptionPane.showMessageDialog(
                            ChangePasswordFrame.this,
                            "La operación fue interrumpida."
                    );

                    saveButton.setEnabled(true);
                    backButton.setEnabled(true);

                } catch (ExecutionException e) {

                    Throwable cause = e.getCause();

                    String message =
                            cause instanceof SQLException
                            ? "No se pudo guardar la contraseña."
                            : cause == null
                              ? "Ocurrió un error inesperado."
                              : cause.getMessage();

                    JOptionPane.showMessageDialog(
                            ChangePasswordFrame.this,
                            message,
                            "Operación no completada",
                            JOptionPane.WARNING_MESSAGE
                    );

                    saveButton.setEnabled(true);
                    backButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }
}
