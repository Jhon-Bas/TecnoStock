package pe.tecnostock.modules.inventory.service;

import pe.tecnostock.config.Transaction;
import pe.tecnostock.model.InventoryMovement;
import pe.tecnostock.model.Product;
import pe.tecnostock.model.MovementType;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.inventory.repository.InventoryRepository;
import pe.tecnostock.modules.products.repository.ProductRepository;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.security.Permission;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Reglas de negocio del inventario.
 *
 * Todo cambio de stock pasa por aquí y ocurre en una transacción: se
 * lee el stock, se calcula el nuevo con las reglas de TipoMovimiento,
 * se actualiza el producto y se registra el movimiento. Si algo falla,
 * no queda ni el cambio de stock ni el registro.
 */
public class InventoryService {

    /** Máximo de filas que devuelve cada consulta. */
    private static final int LIMIT = 200;

    private static final int MAX_REASON_LENGTH = 150;

    private final InventoryRepository repository;
    private final ProductRepository products;
    private final AuthorizationService authorization;

    public InventoryService(
            InventoryRepository repository,
            ProductRepository products,
            AuthorizationService authorization
    ) {
        this.repository = repository;
        this.products = products;
        this.authorization = authorization;
    }

    /** Productos activos con sus existencias. */
    public List<Product> searchProducts(
            User currentUser,
            String text,
            boolean lowStockOnly
    ) throws SQLException {

        authorization.requirePermission(
                currentUser, Permission.MANAGE_INVENTORY);

        return products.search(
                text == null ? "" : text.trim(),
                false, lowStockOnly, LIMIT);
    }

    /** Últimos movimientos; productoId 0 trae todos los productos. */
    public List<InventoryMovement> listMovements(
            User currentUser,
            int productId
    ) throws SQLException {

        authorization.requirePermission(
                currentUser, Permission.MANAGE_INVENTORY);

        return repository.listMovements(productId, LIMIT);
    }

    /**
     * Registra un movimiento hecho por el currentUser desde la pantalla de
     * inventario.
     *
     * @param cantidad unidades; en un AJUSTE, el stock contado
     */
    public void register(
            User currentUser,
            int productId,
            MovementType type,
            int quantity,
            String reason
    ) throws SQLException {

        authorization.requirePermission(
                currentUser, Permission.MANAGE_INVENTORY);

        Transaction.executeWithoutResult(connection ->
                applyMovement(connection, currentUser.getId(),
                        productId, type, quantity, reason));
    }

    /**
     * Aplica un movimiento dentro de la transacción del llamador.
     *
     * Es la pieza que usará el proceso de venta para descontar stock
     * en la misma transacción que registra la venta. No comprueba
     * permisos: quien la llama ya debe haber autorizado la operación.
     *
     * @throws IllegalArgumentException si el movimiento no es válido
     */
    public void applyMovement(
            Connection connection,
            int userId,
            int productId,
            MovementType type,
            int quantity,
            String reason
    ) throws SQLException {

        reason = validateReason(reason);

        int previousStock = repository
                .readActiveStock(connection, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no existe o está inactivo."));

        int newStock = type.calculateStock(previousStock, quantity);

        if (!repository.updateStock(
                connection, productId, previousStock, newStock)) {

            throw new IllegalArgumentException(
                    "El stock cambió mientras se registraba. "
                    + "Intenta de nuevo."
            );
        }

        repository.insertMovement(
                connection, productId, type,
                Math.abs(newStock - previousStock),
                previousStock, newStock, userId, reason);
    }

    /** Todo movimiento debe poder explicarse: el motivo es obligatorio. */
    private static String validateReason(String reason) {

        String value = reason == null ? "" : reason.trim();

        if (value.isEmpty() || value.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException(
                    "El motivo es obligatorio (máximo "
                    + MAX_REASON_LENGTH + " caracteres)."
            );
        }

        return value;
    }
}