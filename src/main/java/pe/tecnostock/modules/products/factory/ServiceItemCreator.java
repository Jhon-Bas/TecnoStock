package pe.tecnostock.modules.products.factory;

import pe.tecnostock.model.ServiceItem;
import pe.tecnostock.model.CommercialItem;
import pe.tecnostock.modules.products.factory.CommercialItemFactory.ItemData;

/** Creador concreto de servicios comerciales. */
public final class ServiceItemCreator extends ItemCreator {
    /** Construye el tipo correspondiente conservando las reglas del catálogo. */
    @Override
    protected CommercialItem createItem(ItemData data) {
        return new ServiceItem(data.id(), data.code(), data.name(), data.description(),
                data.salePrice(), true);
    }
}