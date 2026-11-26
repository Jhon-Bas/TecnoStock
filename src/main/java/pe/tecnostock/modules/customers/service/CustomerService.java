package pe.tecnostock.modules.customers.service;

import pe.tecnostock.model.Customer;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.customers.repository.CustomerRepository;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.security.Permission;

import java.sql.SQLException;
import java.util.List;

/**
 * Reglas de negocio de los clientes.
 *
 * Comprueba el permiso del usuario en cada operación, evita
 * documentos duplicados y delega el SQL en ClienteRepository.
 * Los datos de cada cliente los valida la clase Cliente.
 */
public class CustomerService {

    /** Máximo de filas que devuelve una búsqueda. */
    private static final int SEARCH_LIMIT = 200;

    private final CustomerRepository repository;
    private final AuthorizationService authorization;

    /** Inicializa las dependencias del servicio. */
    public CustomerService(
            CustomerRepository repository,
            AuthorizationService authorization
    ) {
        this.repository = repository;
        this.authorization = authorization;
    }

    /** Busca registros según el texto y el estado solicitado. */
    public List<Customer> search(
            User currentUser,
            String text,
            boolean includeInactive
    ) throws SQLException {

        authorization.requirePermission(currentUser, Permission.VIEW_CUSTOMERS);

        return repository.search(
                text == null ? "" : text.trim(),
                includeInactive,
                SEARCH_LIMIT
        );
    }

    /** Registra un cliente tras comprobar los permisos y sus datos. */
    public void register(User currentUser, Customer customer)
            throws SQLException {

        authorization.requirePermission(currentUser, Permission.REGISTER_CUSTOMERS);

        validateUniqueDocument(customer);
        repository.insert(customer);
    }

    /** Actualiza los datos del cliente seleccionado. */
    public void update(User currentUser, Customer customer)
            throws SQLException {

        authorization.requirePermission(currentUser, Permission.MANAGE_CUSTOMERS);

        if (customer.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un cliente válido."
            );
        }

        validateUniqueDocument(customer);

        if (!repository.update(customer)) {
            throw new IllegalArgumentException(
                    "El cliente ya no existe."
            );
        }
    }

    /** Desactiva o reactiva un cliente sin borrar su historial. */
    public void changeStatus(User currentUser, int id, boolean active)
            throws SQLException {

        authorization.requirePermission(currentUser, Permission.MANAGE_CUSTOMERS);

        if (!repository.changeStatus(id, active)) {
            throw new IllegalArgumentException(
                    "El cliente ya no existe."
            );
        }
    }

    /** Impide duplicar documentos de identidad. */
    private void validateUniqueDocument(Customer customer)
            throws SQLException {

        if (repository.documentExists(
                customer.getDocument(), customer.getId())) {

            throw new IllegalArgumentException(
                    "Ya existe un cliente con ese documento."
            );
        }
    }
}