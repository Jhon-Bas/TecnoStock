/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.tecnostock.model;

import java.math.BigDecimal;
import java.util.List;

/** Entidad del registro comercial persistido. */
public class Sale {

    private final int id;
    private final String receiptNumber;
    private final int customerId;
    private final String nombreCliente;
    private final String documentoCliente;
    private final int userId;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final String estado;
    private final String fecha;
    private final List<SaleDetail> details;

    public Sale(
            int id,
            String receiptNumber,
            int customerId,
            String nombreCliente,
            String documentoCliente,
            int userId,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            String estado,
            String fecha,
            List<SaleDetail> details
    ) {
        this.id = id;
        this.receiptNumber = receiptNumber;
        this.customerId = customerId;
        this.nombreCliente = nombreCliente;
        this.documentoCliente = documentoCliente;
        this.userId = userId;
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.total = total != null ? total : BigDecimal.ZERO;
        this.estado = estado;
        this.fecha = fecha;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public int getId() {
        return id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return nombreCliente;
    }

    public String getCustomerDocument() {
        return documentoCliente;
    }

    public int getUserId() {
        return userId;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getStatus() {
        return estado;
    }

    public String getDate() {
        return fecha;
    }

    public List<SaleDetail> getDetails() {
        return details;
    }
}