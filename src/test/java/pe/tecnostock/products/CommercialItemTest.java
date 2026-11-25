package pe.tecnostock.products;

import org.junit.jupiter.api.Test;

import pe.tecnostock.model.Category;
import pe.tecnostock.model.Product;
import pe.tecnostock.model.ServiceItem;
import pe.tecnostock.model.ItemType;
import pe.tecnostock.model.Sellable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba las reglas de Producto y Servicio. No usa base de datos.
 */
public class CommercialItemTest {

    private static final Category LAPTOPS = new Category(1, "Laptops");

    private static Product product(String code, String sale, String purchase) {
        return new Product(
                0, code, "Laptop Lenovo", null,
                new BigDecimal(sale), new BigDecimal(purchase),
                0, 2, LAPTOPS, true
        );
    }

    private static ServiceItem service(String sale) {
        return new ServiceItem(
                0, "SER-001", "Mantenimiento", null,
                new BigDecimal(sale), true
        );
    }

    @Test
    void normalizeCodeAndPrices() {

        Product product = product(" lap-001 ", "129.9", "100");

        assertEquals("LAP-001", product.getCode());
        assertEquals("129.90", product.getSalePrice().toString());
        assertEquals("100.00", product.getPurchasePrice().toString());
    }

    @Test
    void rejectInvalidCodes() {

        for (String invalido : new String[] {
                "AB", "CODIGO CON ESPACIOS", "LAP_001", "", null,
                "A".repeat(21)}) {

            assertThrows(IllegalArgumentException.class,
                    () -> product(invalido, "10", "5"),
                    "Debería rechazar: " + invalido);
        }
    }

    @Test
    void rejectInvalidSalePrices() {

        for (String invalido : new String[] {
                "0", "-1", "10.999", "1000000"}) {

            assertThrows(IllegalArgumentException.class,
                    () -> product("LAP-001", invalido, "5"),
                    "Debería rechazar: " + invalido);
        }

        assertThrows(IllegalArgumentException.class,
                () -> new ServiceItem(0, "SER-001", "Soporte",
                        null, null, true));
    }

    @Test
    void allowZeroPurchasePrice() {

        assertDoesNotThrow(() -> product("LAP-001", "10", "0"));

        assertThrows(IllegalArgumentException.class,
                () -> product("LAP-001", "10", "-1"));
    }

    @Test
    void rejectInvalidStockAndMissingCategory() {

        assertThrows(IllegalArgumentException.class,
                () -> new Product(0, "LAP-001", "Laptop", null,
                        new BigDecimal("10"), new BigDecimal("5"),
                        -1, 0, LAPTOPS, true));

        assertThrows(IllegalArgumentException.class,
                () -> new Product(0, "LAP-001", "Laptop", null,
                        new BigDecimal("10"), new BigDecimal("5"),
                        0, -1, LAPTOPS, true));

        assertThrows(IllegalArgumentException.class,
                () -> new Product(0, "LAP-001", "Laptop", null,
                        new BigDecimal("10"), new BigDecimal("5"),
                        0, 0, null, true));
    }

    @Test
    void onlyProductAffectsInventory() {

        Product product = product("LAP-001", "10", "5");
        ServiceItem service = service("50");

        assertTrue(product.affectsInventory());
        assertFalse(service.affectsInventory());

        assertEquals(ItemType.PRODUCT, product.getType());
        assertEquals(ItemType.SERVICE, service.getType());
    }

    @Test
    void bothTypesAreSellable() {

        List<Sellable> items = List.of(
                product("LAP-001", "129.90", "100"),
                service("50.00")
        );

        BigDecimal total = BigDecimal.ZERO;
        for (Sellable item : items) {
            total = total.add(item.getSalePrice());
        }

        // Con BigDecimal la suma es exacta, sin errores de redondeo.
        assertEquals("179.90", total.toString());
    }

    @Test
    void rejectCategoryWithoutName() {

        assertThrows(IllegalArgumentException.class,
                () -> new Category(1, "  "));
    }
}