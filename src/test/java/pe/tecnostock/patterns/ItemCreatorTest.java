package pe.tecnostock.patterns;

import org.junit.jupiter.api.Test;
import pe.tecnostock.model.*;
import pe.tecnostock.modules.products.factory.*;
import pe.tecnostock.modules.products.factory.CommercialItemFactory.ItemData;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica la creación polimórfica mediante Factory Method. */
class ItemCreatorTest {
    /** Comprueba que el creador de productos conserve las reglas de inventario. */
    @Test void createsProduct() {
        ItemCreator creator = new ProductCreator();
        ItemData data = new ItemData(0, "P-001", "Producto", null,
                new BigDecimal("100"), new BigDecimal("50"), 3, new Category(1, "Laptops"));
        Product product = assertInstanceOf(Product.class, creator.create(data));
        assertEquals(0, product.getCurrentStock());
        assertEquals(3, product.getMinimumStock());
    }

    /** Comprueba la creación de servicios sin afectar inventario. */
    @Test void createsService() {
        ItemCreator creator = new ServiceItemCreator();
        ItemData data = new ItemData(7, "S-001", "Servicio", null,
                new BigDecimal("100"), null, 0, null);
        CommercialItem item = creator.create(data);
        assertInstanceOf(ServiceItem.class, item);
        assertEquals(7, item.getId());
        assertFalse(item.affectsInventory());
    }

    /** Comprueba la validación común de los creadores. */
    @Test void rejectsNullData() {
        assertThrows(NullPointerException.class, () -> new ProductCreator().create(null));
        assertThrows(NullPointerException.class, () -> new ServiceItemCreator().create(null));
    }
}