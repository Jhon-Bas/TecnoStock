package pe.tecnostock.modules.reports.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import pe.tecnostock.modules.reports.model.SalesReport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;

/** Exportación de reportes. Formatos independientes del servicio de negocio. */
public final class ReportExporter {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"));

    /** Genera un reporte en formato PDF. */
    public Path exportPdf(SalesReport report, Path destination) throws IOException, DocumentException {
        if (report == null) throw new IllegalArgumentException("El reporte es obligatorio.");
        Path file = prepare(destination);
        Document document = new Document(PageSize.A4, 36, 36, 42, 36);
        try (var output = Files.newOutputStream(file)) {
            try {
                PdfWriter.getInstance(document, output);
                document.open();
                Paragraph title = new Paragraph("TECNOSTOCK - REPORTE DE VENTAS", new Font(Font.HELVETICA, 16, Font.BOLD));
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);
                document.add(new Paragraph("Periodo: " + report.fromDate() + " al " + report.toDate()));
                document.add(new Paragraph("Ventas emitidas: " + report.saleCount()));
                document.add(new Paragraph("Subtotal: " + currency(report.subtotal()) + "    Descuentos: " + currency(report.discounts())));
                document.add(new Paragraph("TOTAL: " + currency(report.total()), new Font(Font.HELVETICA, 13, Font.BOLD)));
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("VENTAS POR DÍA", new Font(Font.HELVETICA, 12, Font.BOLD)));
                PdfPTable days = new PdfPTable(new float[]{3, 2, 3});
                days.setWidthPercentage(100);
                for (String heading : new String[]{"Fecha", "Cantidad", "Total"}) days.addCell(heading);
                for (var day : report.dailySales()) {
                    days.addCell(day.date().toString());
                    days.addCell(String.valueOf(day.quantity()));
                    days.addCell(currency(day.total()));
                }
                document.add(days);
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("10 PRODUCTOS / SERVICIOS MÁS VENDIDOS", new Font(Font.HELVETICA, 12, Font.BOLD)));
                PdfPTable items = new PdfPTable(new float[]{5, 2, 3});
                items.setWidthPercentage(100);
                for (String heading : new String[]{"Descripción", "Unidades", "Importe"}) items.addCell(heading);
                for (var item : report.topItems()) {
                    items.addCell(item.name());
                    items.addCell(String.valueOf(item.quantity()));
                    items.addCell(currency(item.amount()));
                }
                document.add(items);
            } finally {
                if (document.isOpen()) document.close();
            }
        }
        return file;
    }

    /** Genera un archivo CSV con los indicadores. */
    public Path exportCsv(SalesReport report, Path destination) throws IOException {
        if (report == null) throw new IllegalArgumentException("El reporte es obligatorio.");
        Path file = prepare(destination);
        StringBuilder csv = new StringBuilder("\uFEFFSección,Fecha,Concepto,Cantidad,Importe\r\n");
        csv.append("Resumen,,Ventas,").append(report.saleCount()).append(',').append(report.total()).append("\r\n");
        csv.append("Resumen,,Subtotal,,").append(report.subtotal()).append("\r\n");
        csv.append("Resumen,,Descuentos,,").append(report.discounts()).append("\r\n");
        for (var day : report.dailySales()) {
            csv.append("Diario,").append(day.date()).append(",Ventas,").append(day.quantity()).append(',').append(day.total()).append("\r\n");
        }
        for (var item : report.topItems()) {
            csv.append("Productos,,").append(escape(item.name())).append(',').append(item.quantity()).append(',').append(item.amount()).append("\r\n");
        }
        Files.writeString(file, csv.toString(), StandardCharsets.UTF_8);
        return file;
    }

    /** Prepara la ruta de destino. */
    private static Path prepare(Path destination) throws IOException {
        if (destination == null) throw new IllegalArgumentException("El destino es obligatorio.");
        Path file = destination.toAbsolutePath();
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        return file;
    }

    /** Formatea el importe en soles. */
    private static String currency(java.math.BigDecimal value) {
        return CURRENCY.format(value == null ? java.math.BigDecimal.ZERO : value);
    }

    /** Evita que un nombre importado se interprete como fórmula al abrir el CSV. */
    private static String escape(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}