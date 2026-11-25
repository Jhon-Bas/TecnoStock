package pe.tecnostock.modules.sales.view;

import org.junit.jupiter.api.Test;
import pe.tecnostock.model.SaleDetail;
import pe.tecnostock.model.ItemType;
import pe.tecnostock.model.Sale;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReceiptDialogTest {

    @Test
    void formatoIncluyeDatosYTotalesDeLaVenta() {
        SaleDetail detail = new SaleDetail(
                1, 7, ItemType.PRODUCT, 3, "TEC-01",
                "Teclado", 2, new BigDecimal("25.00"),
                new BigDecimal("50.00"));
        Sale sale = new Sale(
                7, "B001-000007", 4, "Cliente de prueba", "12345678",
                2, new BigDecimal("50.00"), new BigDecimal("5.00"),
                new BigDecimal("45.00"), "EMITIDA", "2026-11-24 10:00:00",
                List.of(detail));

        String receipt = ReceiptDialog.format(sale);

        assertAll(
                () -> assertTrue(receipt.contains("B001-000007")),
                () -> assertTrue(receipt.contains("Cliente de prueba")),
                () -> assertTrue(receipt.contains("12345678")),
                () -> assertTrue(receipt.contains("Teclado")),
                () -> assertTrue(receipt.contains("Subtotal:")),
                () -> assertTrue(receipt.contains("Descuento:")),
                () -> assertTrue(receipt.contains("TOTAL:")),
                () -> assertTrue(receipt.contains("EMITIDA"))
        );
    }
}