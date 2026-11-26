package pe.tecnostock.modules.products.factory;

import pe.tecnostock.model.Category;
import pe.tecnostock.model.CommercialItem;
import pe.tecnostock.model.ItemType;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Selector de creadores concretos del patrón Factory Method.
 *
 * El formulario y el servicio entregan el tipo elegido y los datos;
 * esta clase selecciona el creador; cada creador instancia su tipo.
 * Así, el resto del sistema no depende de las clases concretas ni
 * repite las reglas de creación.
 */
public class CommercialItemFactory {

    /**
     * Datos de entrada. Cada tipo usa solo los que le corresponden:
     * un servicio ignora precioCompra, stockMinimo y categoria.
     *
     * @param id 0 para un elemento nuevo; el id real al actualizar
     */
    public record ItemData(
            int id,
            String code,
            String name,
            String description,
            BigDecimal salePrice,
            BigDecimal purchasePrice,
            int minimumStock,
            Category category
    ) {}

    /**
     * Crea un elemento activo, con las existencias de un producto
     * en cero (el stock entra por movimientos de inventario).
     *
     * @throws IllegalArgumentException si los datos no son válidos
     */
    public CommercialItem create(ItemType type, ItemData data) {

        Objects.requireNonNull(type, "El tipo es obligatorio.");
        Objects.requireNonNull(data, "Los datos son obligatorios.");

        ItemCreator creator = switch (type) {
            case PRODUCT -> new ProductCreator();
            case SERVICE -> new ServiceItemCreator();
        };
        return creator.create(data);
    }
}