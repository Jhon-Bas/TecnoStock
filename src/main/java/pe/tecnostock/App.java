
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

        // Configura una apariencia consistente para toda la aplicación.
        UIManager.put("defaultFont",
                new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));

        // Redondea los componentes interactivos.
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);

        // Unifica la altura y apariencia de las tablas.
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor",
                new java.awt.Color(226, 232, 240));
        UIManager.put("Table.alternateRowColor",
                new java.awt.Color(248, 250, 252));

        UIManager.put("Table.selectionBackground",
                new java.awt.Color(219, 234, 254));
        UIManager.put("Table.selectionForeground",
                new java.awt.Color(30, 58, 138));

        UIManager.put("TableHeader.height", 36);
        UIManager.put("TableHeader.background",
                new java.awt.Color(241, 245, 249));
        UIManager.put("TableHeader.foreground",
                new java.awt.Color(30, 41, 59));

        // Mantiene un estilo discreto en las barras de desplazamiento.
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.showButtons", false);

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

            java.util.logging.Logger.getLogger(App.class.getName()).log(java.util.logging.Level.SEVERE, "Error durante la operacion.", e);
        }
    }
}
