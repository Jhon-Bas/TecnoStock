package pe.tecnostock.modules.products.factory;

import pe.tecnostock.model.CommercialItem;
import pe.tecnostock.modules.products.factory.CommercialItemFactory.ItemData;
import java.util.Objects;

/** Creador abstracto que define el proceso común de creación comercial. */
public abstract class ItemCreator {
    /** Valida los datos y delega la construcción al método de fábrica. */
    public final CommercialItem create(ItemData data) {
        return createItem(Objects.requireNonNull(data, "Los datos son obligatorios."));
    }

    /** Método de fábrica implementado por cada creador concreto. */
    protected abstract CommercialItem createItem(ItemData data);
}