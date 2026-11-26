package pe.tecnostock.modules.sales.view;

import pe.tecnostock.model.SaleDetail;
import pe.tecnostock.model.Sale;
import pe.tecnostock.modules.sales.export.ReceiptPdfExporter;
import com.lowagie.text.DocumentException;
import javax.swing.JFileChooser;
import java.nio.file.Path;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Presentación e impresión de una boleta registrada.
 * No genera ni modifica datos contables: utiliza la venta persistida.
 */
public final class ReceiptDialog extends JDialog {

    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"));

    private final JTextArea contenido = new JTextArea();
    private final Sale ventaActual;

    public ReceiptDialog(Window owner, Sale sale) {
        super(owner, "Boleta " + Objects.requireNonNull(sale).getReceiptNumber(),
                ModalityType.APPLICATION_MODAL);
        this.ventaActual = Objects.requireNonNull(sale);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        contenido.setEditable(false);
        contenido.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        contenido.setText(format(sale));
        contenido.setCaretPosition(0);
        contenido.setBorder(new EmptyBorder(14, 14, 14, 14));

        add(new JScrollPane(contenido), BorderLayout.CENTER);

        JButton imprimir = new JButton("Imprimir");
        JButton pdf = new JButton("Guardar PDF");
        JButton cerrar = new JButton("Cerrar");
        imprimir.addActionListener(e -> imprimir());
        pdf.addActionListener(e -> exportarPdf());
        cerrar.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(cerrar);
        actions.add(pdf);
        actions.add(imprimir);
        add(actions, BorderLayout.SOUTH);

        setSize(520, 620);
        setMinimumSize(new Dimension(460, 520));
        setLocationRelativeTo(owner);
    }

    static String format(Sale sale) {
        StringBuilder text = new StringBuilder();
        text.append("                 TECNOSTOCK\n")
                .append("             BOLETA DE VENTA\n")
                .append("==============================================\n")
                .append("N.º: ").append(sale.getReceiptNumber()).append('\n')
                .append("Fecha: ").append(sale.getDate()).append('\n')
                .append("----------------------------------------------\n")
                .append("CLIENTE\n")
                .append("Nombre: ").append(sale.getCustomerName()).append('\n')
                .append("Documento: ").append(sale.getCustomerDocument()).append('\n')
                .append("----------------------------------------------\n")
                .append(String.format("%-22s %4s %10s%n", "Descripción", "Cant.", "Importe"))
                .append("----------------------------------------------\n");

        for (SaleDetail detail : sale.getDetails()) {
            text.append(String.format(Locale.ROOT, "%-22.22s %4d %10s%n",
                    detail.getItemName(), detail.getQuantity(),
                    money(detail.getSubtotal())));
        }

        text.append("----------------------------------------------\n")
                .append(String.format("%-28s %12s%n", "Subtotal:", money(sale.getSubtotal())))
                .append(String.format("%-28s %12s%n", "Descuento:", money(sale.getDiscount())))
                .append(String.format("%-28s %12s%n", "TOTAL:", money(sale.getTotal())))
                .append("==============================================\n")
                .append("             Gracias por su compra\n")
                .append("\nEstado: ").append(sale.getStatus()).append('\n');
        return text.toString();
    }


    private void exportarPdf() {
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new java.io.File("boleta-" + ventaActual.getReceiptNumber() + ".pdf"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destination = selector.getSelectedFile().toPath();
        if (!destination.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            destination = destination.resolveSibling(destination.getFileName() + ".pdf");
        }
        try {
            Path archivo = new ReceiptPdfExporter().export(ventaActual, destination);
            JOptionPane.showMessageDialog(this, "Boleta PDF guardada en:\n" + archivo,
                    "Exportación completada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | DocumentException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimir() {
        try {
            boolean aceptado = contenido.print(
                    null, null, true, null, null, true);
            if (aceptado) {
                JOptionPane.showMessageDialog(this,
                        "La boleta se envió a la impresora.",
                        "Impresión", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo imprimir la boleta: " + ex.getMessage(),
                    "Error de impresión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String money(BigDecimal value) {
        return CURRENCY.format(value == null ? BigDecimal.ZERO : value);
    }
}