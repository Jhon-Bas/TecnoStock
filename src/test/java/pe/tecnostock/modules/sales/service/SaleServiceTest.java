package pe.tecnostock.modules.sales.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import pe.tecnostock.config.TemporaryDatabase;
import pe.tecnostock.model.*;
import pe.tecnostock.modules.customers.repository.CustomerRepository;
import pe.tecnostock.modules.customers.service.CustomerService;
import pe.tecnostock.modules.inventory.repository.InventoryRepository;
import pe.tecnostock.modules.inventory.service.InventoryService;
import pe.tecnostock.modules.products.factory.CommercialItemFactory;
import pe.tecnostock.modules.products.factory.CommercialItemFactory.ItemData;
import pe.tecnostock.modules.products.repository.*;
import pe.tecnostock.modules.products.service.CatalogService;
import pe.tecnostock.modules.sales.repository.SaleRepository;
import pe.tecnostock.security.TestUsers;
import pe.tecnostock.service.AuthorizationService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica ventas, inventario y permisos sobre una base temporal aislada. */
@ResourceLock("tecnostock.db.path")
class SaleServiceTest {
    @TempDir Path directory;
    private String previousPath;
    private TestUsers users;
    private InventoryService inventory;
    private SaleService sales;
    private int customerId;
    private int productId;

    /** Prepara un cliente y un producto con cinco unidades disponibles. */
    @BeforeEach void setUp() throws SQLException {
        previousPath = TemporaryDatabase.use(directory);
        users = TestUsers.create();
        var authorization = new AuthorizationService();
        var products = new ProductRepository();
        var services = new ServiceItemRepository();
        var customers = new CustomerRepository();
        var customerService = new CustomerService(customers, authorization);
        customerService.register(users.manager, Customer.create("12345678", "Ana Torres", null, null, null));
        customerId = customerService.search(users.manager, "12345678", false).get(0).getId();
        var catalog = new CatalogService(products, services, new CommercialItemFactory(), authorization);
        var category = catalog.listCategories(users.manager).get(0);
        catalog.register(users.manager, ItemType.PRODUCT, new ItemData(0, "LAP-001", "Laptop Lenovo", null,
                new BigDecimal("100.00"), new BigDecimal("80.00"), 2, category));
        inventory = new InventoryService(new InventoryRepository(), products, authorization);
        productId = product().getId();
        inventory.register(users.manager, productId, MovementType.ENTRY, 5, "Stock de prueba");
        sales = new SaleService(new SaleRepository(), customers, products, services, inventory, authorization);
    }

    /** Restaura la configuración original de la base de datos. */
    @AfterEach void restore() { TemporaryDatabase.restore(previousPath); }

    /** Consulta el producto con su stock actual. */
    private Product product() throws SQLException {
        return inventory.searchProducts(users.manager, "LAP-001", false).get(0);
    }

    /** Registra una venta con una sola línea. */
    private Sale sell(User user, int quantity, String discount) throws SQLException {
        return sales.registerSale(user, customerId, List.of(new SaleService.RequestedLine(product(), quantity)), new BigDecimal(discount));
    }

    /** Verifica importes, detalle persistido y reducción de existencias. */
    @Test void saleUpdatesStockAndTotals() throws SQLException {
        Sale sale = sell(users.manager, 2, "10.00");
        assertEquals(new BigDecimal("200.00"), sale.getSubtotal());
        assertEquals(new BigDecimal("10.00"), sale.getDiscount());
        assertEquals(new BigDecimal("190.00"), sale.getTotal());
        assertEquals(1, sale.getDetails().size());
        assertEquals(3, product().getCurrentStock());
        assertEquals(1, sales.listSales(users.manager).size());
    }

    /** Comprueba que una segunda línea fallida revierte también la primera. */
    @Test void insufficientStockRollsBackEntireSale() throws SQLException {
        Product item = product();
        assertThrows(IllegalArgumentException.class, () -> sales.registerSale(users.manager, customerId,
                List.of(new SaleService.RequestedLine(item, 3), new SaleService.RequestedLine(item, 3)), BigDecimal.ZERO));
        assertEquals(5, product().getCurrentStock());
        assertTrue(sales.listSales(users.manager).isEmpty());
        assertEquals(1, inventory.listMovements(users.manager, productId).size());
    }

    /** Impide descuentos sin autorización. */
    @Test void employeeCannotApplyDiscount() throws SQLException {
        assertThrows(SecurityException.class, () -> sell(users.employee, 1, "1.00"));
        assertEquals(5, product().getCurrentStock());
        assertTrue(sales.listSales(users.manager).isEmpty());
    }

    /** Impide que el descuento supere el subtotal. */
    @Test void excessiveDiscountIsRejected() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> sell(users.manager, 1, "101.00"));
        assertEquals(5, product().getCurrentStock());
        assertTrue(sales.listSales(users.manager).isEmpty());
    }

    /** Comprueba que el empleado solamente consulta sus propias ventas. */
    @Test void employeeOnlySeesOwnSales() throws SQLException {
        sell(users.manager, 1, "0.00");
        Sale own = sell(users.employee, 1, "0.00");
        assertEquals(2, sales.listSales(users.manager).size());
        var visible = sales.listSales(users.employee);
        assertEquals(1, visible.size());
        assertEquals(own.getId(), visible.get(0).getId());
    }

    /** Rechaza ventas sin artículos. */
    @Test void emptyCartIsRejected() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> sales.registerSale(users.manager, customerId, List.of(), BigDecimal.ZERO));
        assertTrue(sales.listSales(users.manager).isEmpty());
    }
}