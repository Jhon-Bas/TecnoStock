
package pe.tecnostock;

import com.formdev.flatlaf.FlatLightLaf;

import pe.tecnostock.config.migration.SchemaMigrator;
import pe.tecnostock.service.BootstrapService;
import pe.tecnostock.view.LoginFrame;
import pe.tecnostock.view.SetupFrame;

import javax.swing.*;
import java.sql.SQLException;

/**
 * Clase principal de TecnoStock.
 *
 * Actualiza el esquema de la base de datos mediante
 * migraciones versionadas antes de mostrar la interfaz.
 */
public class App {

    public static void main(String[] args) {

        // Configura la apariencia visual de Swing.
        FlatLightLaf.setup();

        try {
            // Actualiza el esquema de SQLite a la última versión.
            new SchemaMigrator().migrate();

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
