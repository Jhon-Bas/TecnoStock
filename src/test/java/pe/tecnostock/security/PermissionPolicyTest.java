package pe.tecnostock.security;

import org.junit.jupiter.api.Test;

import pe.tecnostock.model.Role;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba qué puede hacer cada rol. No usa base de datos.
 */
public class PermissionPolicyTest {

    /** Verifica el acceso completo del administrador. */
    @Test
    void shouldGrantAdminAllPermissions() {

        for (Permission permission : Permission.values()) {
            assertTrue(PermissionPolicy.has(Role.ADMIN, permission),
                    "ADMIN debería tener " + permission);
        }
    }

    /** Verifica los permisos operativos y restricciones del gerente. */
    @Test
    void shouldAllowManagerBusinessOperationsOnly() {

        assertTrue(PermissionPolicy.has(
                Role.GERENTE, Permission.MANAGE_PRODUCTS));
        assertTrue(PermissionPolicy.has(
                Role.GERENTE, Permission.MANAGE_INVENTORY));
        assertTrue(PermissionPolicy.has(
                Role.GERENTE, Permission.APPLY_DISCOUNT));
        assertTrue(PermissionPolicy.has(
                Role.GERENTE, Permission.VIEW_REPORTS));

        assertFalse(PermissionPolicy.has(
                Role.GERENTE, Permission.MANAGE_USERS));
        assertFalse(PermissionPolicy.has(
                Role.GERENTE, Permission.VIEW_AUDIT));
    }

    /** Verifica el acceso limitado del empleado. */
    @Test
    void shouldAllowEmployeeCustomerAndSalesOperations() {

        assertTrue(PermissionPolicy.has(
                Role.EMPLEADO, Permission.REGISTER_SALES));
        assertTrue(PermissionPolicy.has(
                Role.EMPLEADO, Permission.VIEW_PRODUCTS));
        assertTrue(PermissionPolicy.has(
                Role.EMPLEADO, Permission.REGISTER_CUSTOMERS));

        assertFalse(PermissionPolicy.has(
                Role.EMPLEADO, Permission.VIEW_REPORTS));
        assertFalse(PermissionPolicy.has(
                Role.EMPLEADO, Permission.MANAGE_PRODUCTS));
        assertFalse(PermissionPolicy.has(
                Role.EMPLEADO, Permission.MANAGE_INVENTORY));
        assertFalse(PermissionPolicy.has(
                Role.EMPLEADO, Permission.APPLY_DISCOUNT));
        assertFalse(PermissionPolicy.has(
                Role.EMPLEADO, Permission.VIEW_ALL_SALES));
    }

    /** Comprueba que los valores nulos no concedan permisos. */
    @Test
    void shouldRejectNullRoleOrPermission() {

        assertFalse(PermissionPolicy.has(null, Permission.VIEW_REPORTS));
        assertFalse(PermissionPolicy.has(Role.ADMIN, null));
    }

    /** Impide alterar los permisos mediante el conjunto devuelto. */
    @Test
    void shouldExposeImmutablePermissions() {

        assertThrows(
                UnsupportedOperationException.class,
                () -> PermissionPolicy.permissionsFor(Role.EMPLEADO)
                        .add(Permission.VIEW_REPORTS)
        );
    }
}
