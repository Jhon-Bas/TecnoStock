package pe.tecnostock.view;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Colores y componentes visuales compartidos por todas las pantallas,
 * para que cada módulo conserve la misma apariencia.
 */
public final class Styles {

    public static final Color DARK_BLUE = new Color(20, 36, 58);
    public static final Color MEDIUM_BLUE = new Color(41, 73, 103);
    public static final Color ACCENT = new Color(38, 119, 191);
    public static final Color BACKGROUND = new Color(245, 248, 252);
    public static final Color TEXT = new Color(35, 49, 67);
    public static final Color MUTED = new Color(112, 126, 143);

    private static final Color BORDER = new Color(223, 230, 239);

    private Styles() {
        // Impide crear instancias de esta clase.
    }

    /** Título y subtítulo que encabezan cada pantalla. */
    public static JPanel header(String title, String subtitulo) {

        JLabel tituloLabel = new JLabel(title);
        tituloLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        tituloLabel.setForeground(TEXT);

        JLabel subtituloLabel = new JLabel(subtitulo);
        subtituloLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtituloLabel.setForeground(MUTED);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(tituloLabel);
        header.add(Box.createVerticalStrut(7));
        header.add(subtituloLabel);

        return header;
    }

    /** Botón del menú lateral. */
    public static JButton navigationButton(String text) {

        JButton boton = new JButton(text);
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        boton.setPreferredSize(new Dimension(208, 43));
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        boton.setForeground(Color.WHITE);
        boton.setBackground(MEDIUM_BLUE);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.putClientProperty("JButton.buttonType", "roundRect");

        return boton;
    }

    /** Botón de acción dentro del contenido. */
    public static JButton actionButton(String text) {

        JButton boton = new JButton(text);
        boton.setFont(new Font("SansSerif", Font.BOLD, 14));
        Dimension naturalSize = boton.getPreferredSize();

        // Conserva un ancho suficiente para mostrar el texto.
        boton.setPreferredSize(new Dimension(
                naturalSize.width + 16,
                Math.max(43, naturalSize.height)
        ));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.putClientProperty("JButton.buttonType", "roundRect");

        return boton;
    }

    /** Borde de las tarjetas blancas: línea fina más margen interno. */
    public static Border cardBorder(int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(vertical, horizontal, vertical, horizontal)
        );
    }

    /** Etiqueta numérica grande para los indicadores. */
    public static JLabel valueLabel() {

        JLabel etiqueta = new JLabel("—");
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 36));
        etiqueta.setForeground(ACCENT);

        return etiqueta;
    }

    /** Tarjeta con un título y un valor. */
    public static JPanel card(String title, JLabel value) {

        JLabel tituloLabel = new JLabel(title);
        tituloLabel.setForeground(MUTED);
        tituloLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(cardBorder(22, 24));
        card.add(tituloLabel, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }
}