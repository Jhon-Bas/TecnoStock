
package pe.tecnostock;

import com.formdev.flatlaf.FlatLightLaf;

import pe.tecnostock.config.DatabaseInitializer;
import pe.tecnostock.config.AuditInitializer;
import pe.tecnostock.config.PasswordPolicyInitializer;
import pe.tecnostock.config.SessionVersionInitializer;
import pe.tecnostock.service.BootstrapService;
import pe.tecnostock.view.LoginFrame;
import pe.tecnostock.view.SetupFrame;

import javax.swing.*;
import java.sql.SQLException;

/**
 * Clase principal de TecnoStock.
 *
 * Inicializa la persistencia, auditoría,
 * seguridad de contraseñas y control de sesiones
 * antes de mostrar la interfaz.
 */
public class App {

    public static void main(String[] args) {

        // Configura la apariencia visual de Swing.
        FlatLightLaf.setup();

        try {
            // Prepara las tablas principales.
            DatabaseInitializer.initialize();

            // Prepara el historial de actividades.
            AuditInitializer.initialize();

            // Instala la política de contraseñas temporales.
            PasswordPolicyInitializer.initialize();

            // Instala la invalidación de sesiones antiguas.
            SessionVersionInitializer.initialize();

            BootstrapService bootstrapService =
                    new BootstrapService();

            boolean hasUsers = bootstrapService.hasUsers();

            // Ejecuta la interfaz en el hilo de Swing.
            SwingUtilities.invokeLater(() -> {

                if (hasUsers) {
                    new LoginFrame().setVisible(true);
                } else {
                    new SetupFrame(bootstrapService)
                            .setVisible(true);
                }
            });

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    null,
                    "No se pudo iniciar TecnoStock.",
                    "Error de inicio",
                    JOptionPane.ERROR_MESSAGE
            );

            e.printStackTrace();
        }
    }
}
