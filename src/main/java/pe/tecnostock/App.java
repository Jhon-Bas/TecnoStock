
package pe.tecnostock;

import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatLightLaf;

import pe.tecnostock.view.MainFrame;

public class App {

    public static void main(String[] args) {

        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            MainFrame ventana = new MainFrame();
            ventana.setVisible(true);
        });

    }
}
