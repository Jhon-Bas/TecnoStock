package pe.tecnostock.model;

import java.math.BigDecimal;

/**
 * Todo lo que puede agregarse a una venta.
 *
 * El carrito, el detalle de venta y la boleta trabajan solo con
 * esta interfaz, sin conocer si es un producto o un servicio.
 * afectaInventario() les indica si deben descontar existencias.
 */
public interface Sellable {

    int getId();

    String getName();

    BigDecimal getSalePrice();

    ItemType getType();

    /** true si al vender hay que descontar stock. */
    boolean affectsInventory();
}