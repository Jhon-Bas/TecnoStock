package pe.tecnostock.model;

import java.math.BigDecimal;

/** Representa una línea de venta con cantidad, precio unitario e importe. */
public class SaleDetail {

    private final int id;
    private final int saleId;
    private final ItemType itemType;
    private final int itemId;
    private final String itemCode;
    private final String itemName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    /** Construye el detalle con los datos comerciales registrados en la venta. */
    public SaleDetail(
            int id,
            int saleId,
            ItemType itemType,
            int itemId,
            String itemCode,
            String itemName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
        this.id = id;
        this.saleId = saleId;
        this.itemType = itemType;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public int getId() {
        return id;
    }

    public int getSaleId() {
        return saleId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}