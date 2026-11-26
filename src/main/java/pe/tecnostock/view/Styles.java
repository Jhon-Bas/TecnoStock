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
    public static JPanel header(String title, String subtitle) {

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(MUTED);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(7));
        header.add(subtitleLabel);

        return header;
    }

    /** Botón del menú lateral. */
    public static JButton navigationButton(String text) {

        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        button.setPreferredSize(new Dimension(208, 43));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(MEDIUM_BLUE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");

        return button;
    }

    /** Botón de acción dentro del contenido. */
    public static JButton actionButton(String text) {

        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        Dimension naturalSize = button.getPreferredSize();

        // Conserva un ancho suficiente para mostrar el texto.
        button.setPreferredSize(new Dimension(
                naturalSize.width + 16,
                Math.max(43, naturalSize.height)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");

        return button;
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

        JLabel label = new JLabel("—");
        label.setFont(new Font("SansSerif", Font.BOLD, 36));
        label.setForeground(ACCENT);

        return label;
    }

    /** Tarjeta con un título y un valor. */
    public static JPanel card(String title, JLabel value) {

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(cardBorder(22, 24));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }
}