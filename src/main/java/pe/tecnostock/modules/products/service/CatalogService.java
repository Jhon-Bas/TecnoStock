package pe.tecnostock.modules.products.service;

import pe.tecnostock.model.Category;
import pe.tecnostock.model.CommercialItem;
import pe.tecnostock.model.Product;
import pe.tecnostock.model.ServiceItem;
import pe.tecnostock.model.ItemType;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.products.factory.CommercialItemFactory;
import pe.tecnostock.modules.products.factory.CommercialItemFactory.ItemData;
import pe.tecnostock.modules.products.repository.ProductRepository;
import pe.tecnostock.modules.products.repository.ServiceItemRepository;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.security.Permission;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reglas de negocio del catálogo de productos y servicios.
 *
 * Comprueba el permiso del usuario en cada operación, pide a la
 * fábrica que cree el tipo correcto, garantiza que el código no se
 * repita (ni entre productos y servicios) y delega el SQL en los
 * repositorios.
 */
public class CatalogService {

    /** Máximo de filas por tabla que devuelve una búsqueda. */
    private static final int SEARCH_LIMIT = 200;

    private final ProductRepository products;
    private final ServiceItemRepository services;
    private final CommercialItemFactory factory;
    private final AuthorizationService authorization;

    public CatalogService(
            ProductRepository products,
            ServiceItemRepository services,
            CommercialItemFactory factory,
            AuthorizationService authorization
    ) {
        this.products = products;
        this.services = services;
        this.factory = factory;
        this.authorization = authorization;
    }

    /** Productos y servicios juntos, ordenados por nombre. */
    public List<CommercialItem> search(
            User currentUser,
            String text,
            boolean includeInactive
    ) throws SQLException {

        authorization.requirePermission(currentUser, Permission.VIEW_PRODUCTS);

        String filter = text == null ? "" : text.trim();

        List<CommercialItem> result = new ArrayList<>(
                products.search(filter, includeInactive, false, SEARCH_LIMIT));
        result.addAll(
                services.search(filter, includeInactive, SEARCH_LIMIT));

        result.sort(Comparator.comparing(
                CommercialItem::getName, String.CASE_INSENSITIVE_ORDER));

        return result;
    }

    public List<Category> listCategories(User currentUser)
            throws SQLException {

        authorization.requirePermission(currentUser, Permission.VIEW_PRODUCTS);

        return products.listCategories();
    }

    public void register(User currentUser, ItemType type, ItemData data)
            throws SQLException {

        authorization.requirePermission(currentUser, Permission.MANAGE_PRODUCTS);

        CommercialItem item = factory.create(type, data);

        validateUniqueCode(item);

        switch (item) {
            case Product product -> products.insert(product);
            case ServiceItem service -> services.insert(service);
        }
    }

    /**
     * Actualiza los datos del elemento. No modifica el estado ni las
     * existencias de un producto.
     */
    public void update(User currentUser, ItemType type, ItemData data)
            throws SQLException {

        authorization.requirePermission(currentUser, Permission.MANAGE_PRODUCTS);

        if (data.id() <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un elemento válido."
            );
        }

        CommercialItem item = factory.create(type, data);

        validateUniqueCode(item);

        boolean exists = switch (item) {
            case Product product -> products.update(product);
            case ServiceItem service -> services.update(service);
        };

        requireExisting(exists);
    }

    /** Desactiva o reactiva un elemento sin borrar su historial. */
    public void changeStatus(
            User currentUser,
            CommercialItem item,
            boolean active
    ) throws SQLException {

        authorization.requirePermission(currentUser, Permission.MANAGE_PRODUCTS);

        boolean exists = switch (item) {
            case Product product ->
                    products.changeStatus(product.getId(), active);
            case ServiceItem service ->
                    services.changeStatus(service.getId(), active);
        };

        requireExisting(exists);
    }

    /**
     * El código identifica al elemento en la boleta, por eso no puede
     * repetirse ni entre un producto y un servicio.
     */
    private void validateUniqueCode(CommercialItem item)
            throws SQLException {

        int excludedProduct = item instanceof Product ? item.getId() : 0;
        int excludedService = item instanceof ServiceItem ? item.getId() : 0;

        if (products.codeExists(item.getCode(), excludedProduct)
                || services.codeExists(
                        item.getCode(), excludedService)) {

            throw new IllegalArgumentException(
                    "Ya existe un producto o servicio con ese código."
            );
        }
    }

    private static void requireExisting(boolean exists) {

        if (!exists) {
            throw new IllegalArgumentException(
                    "El elemento ya no existe."
            );
        }
    }
}