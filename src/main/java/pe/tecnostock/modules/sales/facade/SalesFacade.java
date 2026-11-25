package pe.tecnostock.modules.sales.facade;

import pe.tecnostock.model.*;
import pe.tecnostock.modules.customers.repository.CustomerRepository;
import pe.tecnostock.modules.customers.service.CustomerService;
import pe.tecnostock.modules.inventory.repository.InventoryRepository;
import pe.tecnostock.modules.inventory.service.InventoryService;
import pe.tecnostock.modules.products.factory.CommercialItemFactory;
import pe.tecnostock.modules.products.repository.ProductRepository;
import pe.tecnostock.modules.products.repository.ServiceItemRepository;
import pe.tecnostock.modules.products.service.CatalogService;
import pe.tecnostock.modules.sales.repository.SaleRepository;
import pe.tecnostock.modules.sales.service.SaleService;
import pe.tecnostock.service.AuthorizationService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** Simplifica el acceso de la interfaz a los subsistemas comerciales. */
public final class SalesFacade {

    /** Agrupa los datos necesarios para iniciar una venta. */
    public record SaleData(List<Customer> customers, List<CommercialItem> catalog) {
        public SaleData {
            customers = List.copyOf(customers);
            catalog = List.copyOf(catalog);
        }
    }

    private final CustomerService customers;
    private final CatalogService catalog;
    private final SaleService sales;

    /** Construye los servicios compartiendo sus dependencias. */
    public SalesFacade() {
        AuthorizationService authorization = new AuthorizationService();
        CustomerRepository customerRepository = new CustomerRepository();
        ProductRepository products = new ProductRepository();
        ServiceItemRepository services = new ServiceItemRepository();
        customers = new CustomerService(customerRepository, authorization);
        catalog = new CatalogService(products, services,
                new CommercialItemFactory(), authorization);
        sales = new SaleService(new SaleRepository(), customerRepository,
                products, services, new InventoryService(new InventoryRepository(),
                products, authorization), authorization);
    }

    /** Consulta conjuntamente los clientes activos y el catálogo disponible. */
    public SaleData prepareSale(User currentUser) throws SQLException {
        return new SaleData(customers.search(currentUser, "", false),
                catalog.search(currentUser, "", false));
    }

    /** Busca artículos activos utilizando las reglas del catálogo. */
    public List<CommercialItem> searchCatalog(User currentUser, String text)
            throws SQLException {
        return catalog.search(currentUser, text, false);
    }

    /** Delega el registro atómico y las validaciones al servicio de ventas. */
    public Sale registerSale(User currentUser, int customerId,
            List<SaleService.RequestedLine> lines, BigDecimal discount)
            throws SQLException {
        return sales.registerSale(currentUser, customerId, lines, discount);
    }

    /** Consulta el historial respetando los permisos y el alcance del rol. */
    public List<Sale> listSales(User currentUser) throws SQLException {
        return sales.listSales(currentUser);
    }
}