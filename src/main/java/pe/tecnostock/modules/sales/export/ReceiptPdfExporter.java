package pe.tecnostock.modules.sales.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import pe.tecnostock.model.SaleDetail;
import pe.tecnostock.model.Sale;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;

/** Adaptador de salida PDF para una boleta persistida. */
public final class ReceiptPdfExporter {
    private static final Font TITLE = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10);
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"));

    public Path export(Sale sale, Path destination) throws IOException, DocumentException {
        if (sale == null || destination == null) throw new IllegalArgumentException("Venta y destino son obligatorios.");
        Path archivo = destination.toAbsolutePath();
        if (archivo.getParent() != null) Files.createDirectories(archivo.getParent());
        Document documento = new Document(PageSize.A5, 36, 36, 36, 36);
        try (var salida = Files.newOutputStream(archivo)) {
            try {
                PdfWriter.getInstance(documento, salida);
                documento.open();
                Paragraph title = new Paragraph("TECNOSTOCK", TITLE);
                title.setAlignment(Element.ALIGN_CENTER);
                documento.add(title);
                Paragraph subtitulo = new Paragraph("BOLETA DE VENTA", NORMAL);
                subtitulo.setAlignment(Element.ALIGN_CENTER);
                documento.add(subtitulo);
                documento.add(new Paragraph("N.º: " + sale.getReceiptNumber(), NORMAL));
                documento.add(new Paragraph("Fecha: " + sale.getDate(), NORMAL));
                documento.add(new Paragraph("Cliente: " + sale.getCustomerName(), NORMAL));
                documento.add(new Paragraph("Documento: " + sale.getCustomerDocument(), NORMAL));
                documento.add(Chunk.NEWLINE);

                PdfPTable table = new PdfPTable(new float[]{5, 1, 2, 2});
                table.setWidthPercentage(100);
                for (String header : new String[]{"Descripción", "Cant.", "P. unit.", "Importe"}) {
                    PdfPCell celda = new PdfPCell(new Phrase(header));
                    celda.setBackgroundColor(new java.awt.Color(230, 235, 242));
                    table.addCell(celda);
                }
                for (SaleDetail detail : sale.getDetails()) {
                    table.addCell(new Phrase(detail.getItemName(), NORMAL));
                    table.addCell(new Phrase(String.valueOf(detail.getQuantity()), NORMAL));
                    table.addCell(new Phrase(money(detail.getUnitPrice()), NORMAL));
                    table.addCell(new Phrase(money(detail.getSubtotal()), NORMAL));
                }
                documento.add(table);
                documento.add(Chunk.NEWLINE);
                documento.add(new Paragraph("Subtotal: " + money(sale.getSubtotal()), NORMAL));
                documento.add(new Paragraph("Descuento: " + money(sale.getDiscount()), NORMAL));
                Paragraph total = new Paragraph("TOTAL: " + money(sale.getTotal()), new Font(Font.HELVETICA, 12, Font.BOLD));
                total.setAlignment(Element.ALIGN_RIGHT);
                documento.add(total);
                documento.add(Chunk.NEWLINE);
                Paragraph pie = new Paragraph("Gracias por su compra", NORMAL);
                pie.setAlignment(Element.ALIGN_CENTER);
                documento.add(pie);
            } finally {
                if (documento.isOpen()) documento.close();
            }
        }
        return archivo;
    }

    private static String money(BigDecimal value) {
        return CURRENCY.format(value == null ? BigDecimal.ZERO : value);
    }
}