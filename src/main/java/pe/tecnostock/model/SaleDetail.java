/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.tecnostock.model;

import java.math.BigDecimal;

/** Entidad del registro comercial persistido. */
public class SaleDetail {

    private final int id;
    private final int saleId;
    private final ItemType itemType;
    private final int itemId;
    private final String itemCode;
    private final String itemName;
    private final int quantity;
    private final BigDecimal precioUnitario;
    private final BigDecimal subtotal;

    public SaleDetail(
            int id,
            int saleId,
            ItemType itemType,
            int itemId,
            String itemCode,
            String itemName,
            int quantity,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
        this.id = id;
        this.saleId = saleId;
        this.itemType = itemType;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantity = quantity;
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
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
        return precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}