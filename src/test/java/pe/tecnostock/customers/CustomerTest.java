package pe.tecnostock.customers;

import org.junit.jupiter.api.Test;

import pe.tecnostock.model.Customer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba las reglas de datos de Cliente. No usa base de datos.
 */
public class CustomerTest {

    private static Customer withDocument(String document) {
        return Customer.create(document, "Ana Torres", null, null, null);
    }

    @Test
    void shouldAcceptDniAndRuc() {

        assertEquals("DNI", withDocument("12345678").getDocumentType());
        assertEquals("RUC", withDocument("20123456789").getDocumentType());
    }

    @Test
    void shouldRejectInvalidDocuments() {

        for (String invalid : new String[] {
                "1234567", "123456789", "1234567a", "", "   ", null}) {

            assertThrows(IllegalArgumentException.class,
                    () -> withDocument(invalid),
                    "Debería rechazar: " + invalid);
        }
    }

    @Test
    void shouldRequireName() {

        assertThrows(IllegalArgumentException.class,
                () -> Customer.create("12345678", "  ", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> Customer.create("12345678", null, null, null, null));
    }

    @Test
    void shouldValidateOptionalContact() {

        assertDoesNotThrow(() -> Customer.create(
                "12345678", "Ana", "987 654 321", "ana@correo.com", null));

        assertThrows(IllegalArgumentException.class,
                () -> Customer.create("12345678", "Ana", null, "sin-arroba", null));
        assertThrows(IllegalArgumentException.class,
                () -> Customer.create("12345678", "Ana", "abc", null, null));
    }

    @Test
    void shouldNormalizeOptionalFields() {

        Customer customer = Customer.create(
                " 12345678 ", "  Ana Torres ", "   ", "", " ");

        assertEquals("12345678", customer.getDocument());
        assertEquals("Ana Torres", customer.getName());
        assertNull(customer.getPhone());
        assertNull(customer.getEmail());
        assertNull(customer.getAddress());
    }

    @Test
    void shouldCreateActiveCustomerWithoutId() {

        Customer customer = withDocument("12345678");

        assertTrue(customer.isActive());
        assertEquals(0, customer.getId());
    }
}