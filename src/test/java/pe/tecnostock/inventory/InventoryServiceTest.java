package pe.tecnostock.inventory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import pe.tecnostock.config.TemporaryDatabase;
import pe.tecnostock.config.Database;
import pe.tecnostock.config.Transaction;
import pe.tecnostock.model.Category;
import pe.tecnostock.model.InventoryMovement;
import pe.tecnostock.model.Product;
import pe.tecnostock.model.ItemType;
import pe.tecnostock.model.MovementType;
import pe.tecnostock.modules.inventory.repository.InventoryRepository;
import pe.tecnostock.modules.inventory.service.InventoryService;
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
 * Comprueba entradas, salidas, ajustes, historial, permisos y
 * transacciones del inventario con una base SQLite temporal.
 */
@ResourceLock("tecnostock.db.path")
public class InventoryServiceTest {

    @TempDir
    Path tempDirectory;

    private String previousPath;
    private TestUsers users;
    private CatalogService catalog;
    private InventoryService inventory;
    private int productId;

    @BeforeEach
    void setUp() throws SQLException {

        previousPath = TemporaryDatabase.use(tempDirectory);
        users = TestUsers.create();

        ProductRepository products = new ProductRepository();
        AuthorizationService authorization = new AuthorizationService();

        catalog = new CatalogService(products, new ServiceItemRepository(),
                new CommercialItemFactory(), authorization);
        inventory = new InventoryService(
                new InventoryRepository(), products, authorization);

        Category laptops = catalog.listCategories(users.manager)
                .get(0);

        // Stock mínimo 2; el stock inicial siempre es 0.
        catalog.register(users.manager, ItemType.PRODUCT,
                new ItemData(0, "LAP-001", "Laptop Lenovo", null,
                        new BigDecimal("2500"), new BigDecimal("2000"),
                        2, laptops));

        productId = product().getId();
    }

    @AfterEach
    void restore() {
        TemporaryDatabase.restore(previousPath);
    }

    private Product product() throws SQLException {
        return inventory.searchProducts(users.manager, "LAP", false)
                .get(0);
    }

    private int stock() throws SQLException {
        return product().getCurrentStock();
    }

    private List<InventoryMovement> movements() throws SQLException {
        return inventory.listMovements(users.manager, productId);
    }

    @Test
    void entryShouldIncreaseStockAndBeRecorded()
            throws SQLException {

        inventory.register(users.manager, productId,
                MovementType.ENTRY, 10, "Compra a proveedor");

        assertEquals(10, stock());

        InventoryMovement movimiento = movements().get(0);
        assertEquals(MovementType.ENTRY, movimiento.type());
        assertEquals(10, movimiento.quantity());
        assertEquals(0, movimiento.previousStock());
        assertEquals(10, movimiento.resultingStock());
        assertEquals("gerente01", movimiento.username());
        assertEquals("Compra a proveedor", movimiento.reason());
    }

    @Test
    void exitShouldDecreaseStock() throws SQLException {

        inventory.register(users.manager, productId,
                MovementType.ENTRY, 10, "Compra");
        inventory.register(users.manager, productId,
                MovementType.EXIT, 4, "Merma por daño");

        assertEquals(6, stock());
        assertEquals(2, movements().size());
    }

    @Test
    void excessiveExitShouldNotChangeStock()
            throws SQLException {

        inventory.register(users.manager, productId,
                MovementType.ENTRY, 3, "Compra");

        assertThrows(IllegalArgumentException.class,
                () -> inventory.register(users.manager, productId,
                        MovementType.EXIT, 4, "Intento excesivo"));

        assertEquals(3, stock());
        assertEquals(1, movements().size());
    }

    @Test
    void adjustmentShouldRecordDifference()
            throws SQLException {

        inventory.register(users.manager, productId,
                MovementType.ENTRY, 10, "Compra");
        inventory.register(users.manager, productId,
                MovementType.ADJUSTMENT, 7, "Conteo físico");

        assertEquals(7, stock());

        InventoryMovement adjustment = movements().get(0);
        assertEquals(MovementType.ADJUSTMENT, adjustment.type());
        assertEquals(3, adjustment.quantity());
        assertEquals(10, adjustment.previousStock());
        assertEquals(7, adjustment.resultingStock());
    }

    @Test
    void reasonShouldBeRequired() throws SQLException {

        for (String invalid : new String[] {"", "   ", null}) {

            assertThrows(IllegalArgumentException.class,
                    () -> inventory.register(users.manager,
                            productId, MovementType.ENTRY, 5,
                            invalid));
        }

        assertEquals(0, stock());
        assertEquals(0, movements().size());
    }

    @Test
    void shouldRejectMissingProduct() {

        assertThrows(IllegalArgumentException.class,
                () -> inventory.register(users.manager, 9999,
                        MovementType.ENTRY, 5, "Compra"));
    }

    @Test
    void employeeShouldNotManageInventory()
            throws SQLException {

        assertThrows(SecurityException.class,
                () -> inventory.register(users.employee, productId,
                        MovementType.ENTRY, 5, "Compra"));
        assertThrows(SecurityException.class,
                () -> inventory.searchProducts(
                        users.employee, "", false));
        assertThrows(SecurityException.class,
                () -> inventory.listMovements(users.employee, 0));

        assertEquals(0, stock());
    }

    @Test
    void shouldIdentifyLowStock() throws SQLException {

        // Stock 0 con mínimo 2: está bajo.
        assertEquals(1, inventory.searchProducts(
                users.manager, "", true).size());

        inventory.register(users.manager, productId,
                MovementType.ENTRY, 5, "Compra");

        assertEquals(0, inventory.searchProducts(
                users.manager, "", true).size());
    }

    @Test
    void failedMovementShouldRollbackStock()
            throws SQLException {

        // El usuario 9999 no existe: el INSERT del movimiento falla
        // después de haberse actualizado el stock.
        assertThrows(SQLException.class,
                () -> Transaction.executeWithoutResult(connection ->
                        inventory.applyMovement(connection, 9999,
                                productId, MovementType.ENTRY, 8,
                                "Compra")));

        assertEquals(0, stock());
        assertEquals(0, movements().size());
    }

    @Test
    void historyShouldBeImmutable()
            throws SQLException {

        inventory.register(users.manager, productId,
                MovementType.ENTRY, 5, "Compra");

        try (Connection connection = Database.getConnection()) {

            assertThrows(SQLException.class, () ->
                    TemporaryDatabase.execute(connection,
                            "UPDATE movimientos_inventario "
                                    + "SET motivo = 'Otro'"));
            assertThrows(SQLException.class, () ->
                    TemporaryDatabase.execute(connection,
                            "DELETE FROM movimientos_inventario"));
        }

        assertEquals(1, movements().size());
    }
}