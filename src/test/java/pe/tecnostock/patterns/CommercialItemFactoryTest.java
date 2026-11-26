package pe.tecnostock.patterns;

import org.junit.jupiter.api.Test;

import pe.tecnostock.model.Category;
import pe.tecnostock.model.CommercialItem;
import pe.tecnostock.model.Product;
import pe.tecnostock.model.ServiceItem;
import pe.tecnostock.model.ItemType;
import pe.tecnostock.modules.products.factory.CommercialItemFactory;
import pe.tecnostock.modules.products.factory.CommercialItemFactory.ItemData;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Patrón Factory: comprueba que se cree el tipo correcto.
 */
public class CommercialItemFactoryTest {

    private final CommercialItemFactory factory = new CommercialItemFactory();

    private static ItemData data(int id, Category category) {
        return new ItemData(
                id, "ABC-001", "Elemento de prueba", null,
                new BigDecimal("99.90"), new BigDecimal("70"),
                3, category
        );
    }

    @Test
    void createProduct() {

        CommercialItem item = factory.create(
                ItemType.PRODUCT, data(0, new Category(1, "Laptops")));

        Product product = assertInstanceOf(Product.class, item);
        assertEquals(0, product.getCurrentStock());
        assertEquals(3, product.getMinimumStock());
        assertTrue(product.isActive());
    }

    @Test
    void createService() {

        // Un servicio ignora la categoría y los datos de compra.
        CommercialItem item = factory.create(
                ItemType.SERVICE, data(0, null));

        assertInstanceOf(ServiceItem.class, item);
        assertFalse(item.affectsInventory());
    }

    @Test
    void rejectProductWithoutCategory() {

        assertThrows(IllegalArgumentException.class,
                () -> factory.create(ItemType.PRODUCT, data(0, null)));
    }

    @Test
    void preserveIdOnUpdate() {

        CommercialItem item = factory.create(
                ItemType.SERVICE, data(7, null));

        assertEquals(7, item.getId());
    }

    @Test
    void requireTypeAndData() {

        assertThrows(NullPointerException.class,
                () -> factory.create(null, data(0, null)));
        assertThrows(NullPointerException.class,
                () -> factory.create(ItemType.SERVICE, null));
    }
}