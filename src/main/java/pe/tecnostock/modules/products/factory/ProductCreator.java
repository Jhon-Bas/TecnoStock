package pe.tecnostock.modules.products.factory;

import pe.tecnostock.model.Product;
import pe.tecnostock.model.CommercialItem;
import pe.tecnostock.modules.products.factory.CommercialItemFactory.ItemData;

/** Creador concreto de productos comerciales. */
public final class ProductCreator extends ItemCreator {
    /** Construye el tipo correspondiente conservando las reglas del catálogo. */
    @Override
    protected CommercialItem createItem(ItemData data) {
        return new Product(data.id(), data.code(), data.name(), data.description(),
                data.salePrice(), data.purchasePrice(), 0, data.minimumStock(),
                data.category(), true);
    }
}