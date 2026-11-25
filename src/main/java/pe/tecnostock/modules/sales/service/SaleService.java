package pe.tecnostock.modules.sales.service;

import pe.tecnostock.config.Transaction;
import pe.tecnostock.model.*;
import pe.tecnostock.modules.customers.repository.CustomerRepository;
import pe.tecnostock.modules.inventory.service.InventoryService;
import pe.tecnostock.modules.products.repository.ProductRepository;
import pe.tecnostock.modules.products.repository.ServiceItemRepository;
import pe.tecnostock.modules.sales.repository.SaleRepository;
import pe.tecnostock.security.*;
import pe.tecnostock.service.AuthorizationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.*;

/** Coordina el registro transaccional y la consulta de ventas. */
public class SaleService {

    public record RequestedLine(CommercialItem item, int quantity) {}

    private static final int SALES_LIMIT = 200;
    private static final int CATALOG_LIMIT = 300;

    private final SaleRepository sales;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final ServiceItemRepository services;
    private final InventoryService inventory;
    private final AuthorizationService authorization;

    public SaleService(SaleRepository sales, CustomerRepository customers,
            ProductRepository products, ServiceItemRepository services,
            InventoryService inventory, AuthorizationService authorization) {
        this.sales = Objects.requireNonNull(sales);
        this.customers = Objects.requireNonNull(customers);
        this.products = Objects.requireNonNull(products);
        this.services = Objects.requireNonNull(services);
        this.inventory = Objects.requireNonNull(inventory);
        this.authorization = Objects.requireNonNull(authorization);
    }

    public List<Customer> listCustomers(User currentUser) throws SQLException {
        authorization.requirePermission(currentUser, Permission.VIEW_CUSTOMERS);
        return customers.search("", false, CATALOG_LIMIT);
    }

    public List<CommercialItem> listCatalog(User currentUser, String text)
            throws SQLException {
        authorization.requirePermission(currentUser, Permission.VIEW_PRODUCTS);
        String filter = text == null ? "" : text.trim();
        List<CommercialItem> result = new ArrayList<>();
        result.addAll(products.search(filter, false, false, CATALOG_LIMIT));
        result.addAll(services.search(filter, false, CATALOG_LIMIT));
        result.sort(Comparator.comparing(CommercialItem::getName,
                String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public Sale registerSale(User currentUser, int customerId,
            List<RequestedLine> lines, BigDecimal discount) throws SQLException {

        authorization.requirePermission(currentUser, Permission.REGISTER_SALES);
        BigDecimal safeDiscount = normalizeAmount(
                discount == null ? BigDecimal.ZERO : discount, "El descuento");

        if (safeDiscount.signum() > 0) {
            authorization.requirePermission(currentUser, Permission.APPLY_DISCOUNT);
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un producto o servicio.");
        }

        Customer customer = customers.findById(customerId)
                .filter(Customer::isActive)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El cliente no existe o está inactivo."));

        // Copia defensiva: evita que la lista de la interfaz cambie durante el proceso.
        List<RequestedLine> request = List.copyOf(lines);

        return Transaction.execute(cn -> {
            authorization.requirePermission(cn, currentUser, Permission.REGISTER_SALES);
            if (safeDiscount.signum() > 0) authorization.requirePermission(cn, currentUser, Permission.APPLY_DISCOUNT);
            BigDecimal subtotal = BigDecimal.ZERO;
            for (RequestedLine line : request) {
                if (line == null || line.item() == null || line.quantity() <= 0) {
                    throw new IllegalArgumentException(
                            "Cada línea debe tener un ítem y una cantidad mayor a cero.");
                }
                verifyCurrentItem(cn, line.item());
                BigDecimal price = normalizeAmount(
                        line.item().getSalePrice(), "El precio del ítem");
                if (price.signum() <= 0) {
                    throw new IllegalArgumentException("El precio debe ser mayor a cero.");
                }
                subtotal = subtotal.add(price.multiply(
                        BigDecimal.valueOf(line.quantity())));
            }
            subtotal = subtotal.setScale(2, RoundingMode.UNNECESSARY);

            if (safeDiscount.compareTo(subtotal) > 0) {
                throw new IllegalArgumentException(
                        "El descuento no puede superar el subtotal.");
            }
            BigDecimal total = subtotal.subtract(safeDiscount)
                    .setScale(2, RoundingMode.UNNECESSARY);

            int sequence = sales.nextSequence(cn);
            String receipt = String.format(Locale.ROOT, "B001-%06d", sequence);

            int saleId = sales.insertSale(cn, receipt, customer.getId(),
                    currentUser.getId(), toCents(subtotal), toCents(safeDiscount),
                    toCents(total));

            for (RequestedLine line : request) {
                CommercialItem item = line.item();
                BigDecimal price = item.getSalePrice().setScale(2, RoundingMode.UNNECESSARY);
                BigDecimal lineSubtotal = price.multiply(
                        BigDecimal.valueOf(line.quantity()))
                        .setScale(2, RoundingMode.UNNECESSARY);
                ItemType type = item.getType();

                if (item.affectsInventory()) {
                    inventory.applyMovement(cn, currentUser.getId(), item.getId(),
                            MovementType.EXIT, line.quantity(),
                            "Venta " + receipt);
                }

                sales.insertDetail(cn, saleId, type, item.getId(),
                        item.getCode(), item.getName(), line.quantity(),
                        toCents(price), toCents(lineSubtotal));
            }

            return sales.findById(cn, saleId).orElseThrow(
                    () -> new SQLException("La venta se registró, pero no se pudo recuperar."));
        });
    }

    public List<Sale> listSales(User currentUser) throws SQLException {
        if (currentUser != null && PermissionPolicy.has(
                currentUser.getRole(), Permission.VIEW_ALL_SALES)) {
            authorization.requirePermission(currentUser, Permission.VIEW_ALL_SALES);
            return sales.list(null, SALES_LIMIT);
        }
        authorization.requirePermission(currentUser, Permission.VIEW_OWN_SALES);
        return sales.list(currentUser.getId(), SALES_LIMIT);
    }

    /** Rechaza artículos inactivos o precios modificados desde la selección. */
    private void verifyCurrentItem(java.sql.Connection connection, CommercialItem item) throws SQLException {
        String table = item.getType() == ItemType.PRODUCT ? "productos" : "servicios";
        try (var statement = connection.prepareStatement("SELECT codigo, nombre, precio_venta_centimos FROM " + table + " WHERE id = ? AND activo = 1")) {
            statement.setInt(1, item.getId());
            try (var row = statement.executeQuery()) {
                if (!row.next() || !Objects.equals(row.getString(1), item.getCode())
                        || !Objects.equals(row.getString(2), item.getName())
                        || BigDecimal.valueOf(row.getLong(3), 2).compareTo(item.getSalePrice()) != 0) {
                    throw new IllegalArgumentException("El artículo cambió o está inactivo. Actualiza el catálogo.");
                }
            }
        }
    }

    private static BigDecimal normalizeAmount(BigDecimal value, String field) {
        if (value == null || value.signum() < 0
                || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    field + " debe ser positivo o cero y tener máximo 2 decimales.");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static long toCents(BigDecimal toSoles) {
        return toSoles.movePointRight(2).longValueExact();
    }
}