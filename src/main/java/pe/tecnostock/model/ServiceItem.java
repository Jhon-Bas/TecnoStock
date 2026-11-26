package pe.tecnostock.model;

import java.math.BigDecimal;

/**
 * Trabajo que TecnoStock ofrece, como el mantenimiento técnico.
 *
 * No tiene stock ni categoría, y venderlo nunca modifica el
 * inventario de productos.
 */
public final class ServiceItem extends CommercialItem {

    public ServiceItem(
            int id,
            String code,
            String name,
            String description,
            BigDecimal salePrice,
            boolean active
    ) {
        super(id, code, name, description, salePrice, active);
    }

    @Override
    public ItemType getType() {
        return ItemType.SERVICE;
    }

    @Override
    public boolean affectsInventory() {
        return false;
    }
}