package pe.tecnostock.model;

import java.math.BigDecimal;
import java.util.List;

/** Representa una venta registrada y sus detalles comerciales. */
public class Sale {

    private final int id;
    private final String receiptNumber;
    private final int customerId;
    private final String customerName;
    private final String customerDocument;
    private final int userId;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final String status;
    private final String date;
    private final List<SaleDetail> details;

    /** Construye una venta con importes no nulos y una copia inmutable de sus detalles. */
    public Sale(
            int id,
            String receiptNumber,
            int customerId,
            String customerName,
            String customerDocument,
            int userId,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            String status,
            String date,
            List<SaleDetail> details
    ) {
        this.id = id;
        this.receiptNumber = receiptNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerDocument = customerDocument;
        this.userId = userId;
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.total = total != null ? total : BigDecimal.ZERO;
        this.status = status;
        this.date = date;
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
        return customerName;
    }

    public String getCustomerDocument() {
        return customerDocument;
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
        return status;
    }

    public String getDate() {
        return date;
    }

    public List<SaleDetail> getDetails() {
        return details;
    }
}