package pe.tecnostock.model;

import java.math.BigDecimal;

/**
 * Artículo físico que se compra, se guarda en inventario y se vende.
 *
 * Las existencias nunca bajan de cero. Las modificará el módulo de
 * inventario mediante movimientos, que dejan registro del motivo.
 */
public final class Product extends CommercialItem {

    private BigDecimal purchasePrice;
    private int currentStock;
    private int minimumStock;
    private Category category;

    public Product(
            int id,
            String code,
            String name,
            String description,
            BigDecimal salePrice,
            BigDecimal purchasePrice,
            int currentStock,
            int minimumStock,
            Category category,
            boolean active
    ) {
        super(id, code, name, description, salePrice, active);

        setPurchasePrice(purchasePrice);
        setMinimumStock(minimumStock);
        setCategory(category);

        if (currentStock < 0) {
            throw new IllegalArgumentException(
                    "El stock no puede ser negativo."
            );
        }
        this.currentStock = currentStock;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = normalizePrice(
                purchasePrice, "El precio de compra", true);
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(int minimumStock) {

        if (minimumStock < 0) {
            throw new IllegalArgumentException(
                    "El stock mínimo no puede ser negativo."
            );
        }

        this.minimumStock = minimumStock;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {

        if (category == null) {
            throw new IllegalArgumentException(
                    "Selecciona una categoría para el producto."
            );
        }

        this.category = category;
    }

    @Override
    public ItemType getType() {
        return ItemType.PRODUCT;
    }

    @Override
    public boolean affectsInventory() {
        return true;
    }
}