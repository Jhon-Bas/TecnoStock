package pe.tecnostock.customers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import pe.tecnostock.config.TemporaryDatabase;
import pe.tecnostock.model.Customer;
import pe.tecnostock.modules.customers.repository.CustomerRepository;
import pe.tecnostock.modules.customers.service.CustomerService;
import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.security.TestUsers;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba permisos, duplicados y búsqueda de clientes
 * con una base SQLite temporal.
 */
@ResourceLock("tecnostock.db.path")
public class CustomerServiceTest {

    @TempDir
    Path tempDirectory;

    private String previousPath;
    private TestUsers users;
    private CustomerRepository repository;
    private CustomerService service;

    @BeforeEach
    void setUp() throws SQLException {

        previousPath = TemporaryDatabase.use(tempDirectory);
        users = TestUsers.create();

        repository = new CustomerRepository();
        service = new CustomerService(
                repository, new AuthorizationService()
        );
    }

    @AfterEach
    void restore() {
        TemporaryDatabase.restore(previousPath);
    }

    private static Customer customer(String document, String name) {
        return Customer.create(document, name, null, null, null);
    }

    private Customer firstResult(String text) throws SQLException {
        List<Customer> results =
                service.search(users.manager, text, true);
        assertEquals(1, results.size());
        return results.get(0);
    }

    @Test
    void shouldAllowEmployeeToRegisterAndSearch()
            throws SQLException {

        service.register(users.employee,
                customer("12345678", "Ana Torres"));

        List<Customer> results =
                service.search(users.employee, "", false);

        assertEquals(1, results.size());
        assertEquals("Ana Torres", results.get(0).getName());
    }

    @Test
    void shouldRejectDuplicateDocument() throws SQLException {

        service.register(users.manager,
                customer("12345678", "Ana Torres"));

        assertThrows(IllegalArgumentException.class,
                () -> service.register(users.manager,
                        customer("12345678", "Otra Persona")));
    }

    @Test
    void shouldEnforceDatabaseUniqueness()
            throws SQLException {

        repository.insert(customer("12345678", "Ana Torres"));

        assertThrows(SQLException.class,
                () -> repository.insert(
                        customer("12345678", "Otra Persona")));
    }

    @Test
    void shouldRejectEmployeeUpdates()
            throws SQLException {

        service.register(users.employee,
                customer("12345678", "Ana Torres"));
        Customer saved = firstResult("Ana");

        assertThrows(SecurityException.class,
                () -> service.update(users.employee, saved));
        assertThrows(SecurityException.class,
                () -> service.changeStatus(
                        users.employee, saved.getId(), false));
    }

    @Test
    void shouldAllowManagerUpdates() throws SQLException {

        service.register(users.manager,
                customer("12345678", "Ana Torres"));
        Customer saved = firstResult("Ana");

        service.update(users.manager, new Customer(
                saved.getId(), "12345678", "Ana Torres Díaz",
                "987654321", "ana@correo.com", "Av. Perú 123", true));

        Customer updated = firstResult("Díaz");
        assertEquals("987654321", updated.getPhone());
        assertEquals("ana@correo.com", updated.getEmail());
    }

    @Test
    void shouldRejectDuplicateDocumentOnUpdate()
            throws SQLException {

        service.register(users.manager,
                customer("11111111", "Ana"));
        service.register(users.manager,
                customer("22222222", "Beto"));
        Customer beto = firstResult("Beto");

        assertThrows(IllegalArgumentException.class,
                () -> service.update(users.manager, new Customer(
                        beto.getId(), "11111111", "Beto",
                        null, null, null, true)));
    }

    @Test
    void shouldHideInactiveCustomers()
            throws SQLException {

        service.register(users.manager,
                customer("12345678", "Ana Torres"));
        Customer saved = firstResult("Ana");

        service.changeStatus(users.manager, saved.getId(), false);

        assertEquals(0,
                service.search(users.manager, "", false).size());
        assertEquals(1,
                service.search(users.manager, "", true).size());
    }

    @Test
    void shouldSearchByNameOrDocument() throws SQLException {

        service.register(users.manager,
                customer("11111111", "Ana Torres"));
        service.register(users.manager,
                customer("20123456789", "Distribuidora Sol SAC"));

        assertEquals("Ana Torres", firstResult("ana").getName());
        assertEquals("Distribuidora Sol SAC",
                firstResult("20123").getName());
    }

    @Test
    void shouldEscapeLikeWildcards()
            throws SQLException {

        service.register(users.manager,
                customer("11111111", "Ana Torres"));

        assertEquals(0,
                service.search(users.manager, "%", true).size());
        assertEquals(0,
                service.search(users.manager, "_", true).size());
    }

    @Test
    void shouldRejectMissingSession() {

        assertThrows(SecurityException.class,
                () -> service.search(null, "", false));
    }
}