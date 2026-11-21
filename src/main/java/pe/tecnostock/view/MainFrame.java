
package pe.tecnostock.view;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MainFrame extends JFrame {

    public MainFrame() {
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setTitle("TecnoStock - Sistema de Ventas");
        setSize(1000, 650);
        setMinimumSize(new java.awt.Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void inicializarComponentes() {

        JPanel panelPrincipal = new JPanel(
                new BorderLayout(20, 20)
        );

        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        JLabel titulo = new JLabel(
                "TEC NOSTOCK".replace(" ", ""),
                SwingConstants.CENTER
        );

        titulo.setFont(
                new Font("SansSerif", Font.BOLD, 32)
        );

        JLabel subtitulo = new JLabel(
                "Sistema de Gestión de Ventas e Inventario",
                SwingConstants.CENTER
        );

        JPanel encabezado = new JPanel(
                new BorderLayout(0, 10)
        );

        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(subtitulo, BorderLayout.CENTER);

        JButton botonClientes = new JButton("Clientes");
        JButton botonProductos = new JButton("Productos");
        JButton botonVentas = new JButton("Ventas");
        JButton botonReportes = new JButton("Reportes");

        JPanel panelBotones = new JPanel(
                new java.awt.GridLayout(2, 2, 20, 20)
        );

        panelBotones.add(botonClientes);
        panelBotones.add(botonProductos);
        panelBotones.add(botonVentas);
        panelBotones.add(botonReportes);

        panelPrincipal.add(
                encabezado,
                BorderLayout.NORTH
        );

        panelPrincipal.add(
                panelBotones,
                BorderLayout.CENTER
        );

        setContentPane(panelPrincipal);
    }
}
