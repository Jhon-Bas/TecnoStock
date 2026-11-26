package pe.tecnostock.products;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import pe.tecnostock.config.TemporaryDatabase;
import pe.tecnostock.config.Database;
import pe.tecnostock.model.Category;
import pe.tecnostock.model.CommercialItem;
import pe.tecnostock.model.Product;
import pe.tecnostock.model.ServiceItem;
import pe.tecnostock.model.ItemType;
import pe.tecnostock.modules.products.factory.CommercialItemFactory;
import pe.tecnostock.modules.products.factory.CommercialItemFactory.ItemData;
import pe.tecnostock.modules.products.repository.ProductRepository;
import pe.tecnostock.modules.products.repository.ServiceItemRepository;
import pe.tecnostock.modules.products.service.CatalogService;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.security.TestUsers;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba permisos, códigos únicos, precios y búsqueda del
 * catálogo con una base SQLite temporal.
 */
@ResourceLock("tecnostock.db.path")
public class CatalogServiceTest {

    @TempDir
    Path tempDirectory;

    private String previousPath;
    private TestUsers users;
    private CatalogService service;
    private Category laptops;

    @BeforeEach
    void setUp() throws SQLException {

        previousPath = TemporaryDatabase.use(tempDirectory);
        users = TestUsers.create();

        service = new CatalogService(
                new ProductRepository(),
                new ServiceItemRepository(),
                new CommercialItemFactory(),
                new AuthorizationService()
        );

        laptops = service.listCategories(users.manager).stream()
                .filter(c -> c.name().equals("Laptops"))
                .findFirst().orElseThrow();
    }

    @AfterEach
    void restore() {
        TemporaryDatabase.restore(previousPath);
    }

    private ItemData data(int id, String code, String name,
                        String sale, Category category) {
        return new ItemData(id, code, name, null,
                new BigDecimal(sale), new BigDecimal("50"),
                2, category);
    }

    private CommercialItem single(String text) throws SQLException {
        List<CommercialItem> found =
                service.search(users.manager, text, true);
        assertEquals(1, found.size());
        return found.get(0);
    }

    @Test
    void managerCanRegisterProductsAndServices()
            throws SQLException {

        service.register(users.manager, ItemType.PRODUCT,
                data(0, "LAP-001", "Laptop Lenovo", "2500", laptops));
        service.register(users.manager, ItemType.SERVICE,
                data(0, "SER-001", "Mantenimiento", "80", null));

        List<CommercialItem> items =
                service.search(users.manager, "", false);

        assertEquals(2, items.size());
        assertInstanceOf(Product.class, items.get(0));  // Laptop
        assertInstanceOf(ServiceItem.class, items.get(1));  // Mantenimiento
    }

    @Test
    void employeeCanViewButNotEdit()
            throws SQLException {

        service.register(users.manager, ItemType.PRODUCT,
                data(0, "LAP-001", "Laptop Lenovo", "2500", laptops));
        CommercialItem laptop = single("LAP");

        assertEquals(1,
                service.search(users.employee, "", false).size());

        assertThrows(SecurityException.class,
                () -> service.register(users.employee,
                        ItemType.SERVICE,
                        data(0, "SER-001", "Soporte", "50", null)));
        assertThrows(SecurityException.class,
                () -> service.update(users.employee,
                        ItemType.PRODUCT,
                        data(laptop.getId(), "LAP-001", "Otro",
                                "10", laptops)));
        assertThrows(SecurityException.class,
                () -> service.changeStatus(
                        users.employee, laptop, false));
    }

    @Test
    void rejectDuplicateCrossTypeCode()
            throws SQLException {

        service.register(users.manager, ItemType.PRODUCT,
                data(0, "ABC-001", "Laptop", "2500", laptops));

        assertThrows(IllegalArgumentException.class,
                () -> service.register(users.manager,
                        ItemType.PRODUCT,
                        data(0, "abc-001", "Otra laptop", "10", laptops)));

        assertThrows(IllegalArgumentException.class,
                () -> service.register(users.manager,
                        ItemType.SERVICE,
                        data(0, "ABC-001", "Servicio", "10", null)));
    }

    @Test
    void preserveExactPrices() throws SQLException {

        service.register(users.manager, ItemType.PRODUCT,
                data(0, "LAP-001", "Laptop", "1234.56", laptops));

        Product guardado = (Product) single("LAP");

        assertEquals("1234.56", guardado.getSalePrice().toString());
        assertEquals("50.00", guardado.getPurchasePrice().toString());
        assertEquals("Laptops", guardado.getCategory().name());
    }

    @Test
    void updatePreservesStock() throws SQLException {

        service.register(users.manager, ItemType.PRODUCT,
                data(0, "LAP-001", "Laptop", "2500", laptops));
        Product original = (Product) single("LAP");

        // Simula existencias que entraron por inventario.
        try (Connection connection = Database.getConnection()) {
            TemporaryDatabase.execute(connection,
                    "UPDATE productos SET stock_actual = 5");
        }

        service.update(users.manager, ItemType.PRODUCT,
                data(original.getId(), "LAP-001",
                        "Laptop Lenovo IdeaPad", "2600", laptops));

        Product actualizado = (Product) single("IdeaPad");
        assertEquals(5, actualizado.getCurrentStock());
        assertEquals("2600.00", actualizado.getSalePrice().toString());
    }

    @Test
    void rejectDuplicateCodeOnUpdate()
            throws SQLException {

        service.register(users.manager, ItemType.PRODUCT,
                data(0, "LAP-001", "Laptop", "2500", laptops));
        service.register(users.manager, ItemType.SERVICE,
                data(0, "SER-001", "Soporte", "50", null));
        CommercialItem soporte = single("Soporte");

        assertThrows(IllegalArgumentException.class,
                () -> service.update(users.manager,
                        ItemType.SERVICE,
                        data(soporte.getId(), "LAP-001",
                                "Soporte", "50", null)));
    }

    @Test
    void rejectUpdateWithoutId() {

        assertThrows(IllegalArgumentException.class,
                () -> service.update(users.manager,
                        ItemType.SERVICE,
                        data(0, "SER-001", "Soporte", "50", null)));
    }

    @Test
    void inactiveItemHiddenFromDefaultSearch()
            throws SQLException {

        service.register(users.manager, ItemType.SERVICE,
                data(0, "SER-001", "Soporte", "50", null));
        CommercialItem soporte = single("Soporte");

        service.changeStatus(users.manager, soporte, false);

        assertEquals(0,
                service.search(users.manager, "", false).size());
        assertEquals(1,
                service.search(users.manager, "", true).size());
    }

    @Test
    void searchTreatsWildcardsLiterally()
            throws SQLException {

        service.register(users.manager, ItemType.PRODUCT,
                data(0, "LAP-001", "Laptop Lenovo", "2500", laptops));

        assertEquals("Laptop Lenovo", single("lenovo").getName());
        assertEquals("Laptop Lenovo", single("LAP-0").getName());

        assertEquals(0,
                service.search(users.manager, "%", true).size());
        assertEquals(0,
                service.search(users.manager, "_", true).size());
    }

    @Test
    void rejectUnknownCategory() {

        Category fantasma = new Category(999, "Fantasma");

        assertThrows(SQLException.class,
                () -> service.register(users.manager,
                        ItemType.PRODUCT,
                        data(0, "LAP-001", "Laptop", "2500", fantasma)));
    }

    @Test
    void provideInitialCategories() throws SQLException {

        assertEquals(4,
                service.listCategories(users.employee).size());
    }

    @Test
    void rejectMissingSession() {

        assertThrows(SecurityException.class,
                () -> service.search(null, "", false));
    }
}