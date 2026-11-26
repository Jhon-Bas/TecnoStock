package pe.tecnostock.security;

import pe.tecnostock.model.Role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Define qué permisos tiene cada rol.
 *
 * Es la única tabla de permisos del sistema. Es lógica pura,
 * sin base de datos, por lo que se prueba de forma directa.
 *
 * ADMIN: todos los permisos.
 * GERENTE: operación del negocio y reportes, sin administrar
 *          cuentas ni consultar la auditoría.
 * EMPLEADO: atención al cliente y registro de ventas.
 */
public final class PermissionPolicy {

    private static final Map<Role, Set<Permission>> PERMISSIONS = build();

    private PermissionPolicy() {
        // Impide crear instancias de esta clase.
    }

    /** Construye la matriz explícita de permisos de cada rol. */
    private static Map<Role, Set<Permission>> build() {

        Map<Role, Set<Permission>> map = new EnumMap<>(Role.class);

        map.put(Role.ADMIN, EnumSet.allOf(Permission.class));

        map.put(Role.GERENTE, EnumSet.of(
                Permission.VIEW_CUSTOMERS,
                Permission.REGISTER_CUSTOMERS,
                Permission.MANAGE_CUSTOMERS,
                Permission.VIEW_PRODUCTS,
                Permission.MANAGE_PRODUCTS,
                Permission.MANAGE_INVENTORY,
                Permission.REGISTER_SALES,
                Permission.VIEW_OWN_SALES,
                Permission.VIEW_ALL_SALES,
                Permission.APPLY_DISCOUNT,
                Permission.VIEW_REPORTS
        ));

        map.put(Role.EMPLEADO, EnumSet.of(
                Permission.VIEW_CUSTOMERS,
                Permission.REGISTER_CUSTOMERS,
                Permission.VIEW_PRODUCTS,
                Permission.REGISTER_SALES,
                Permission.VIEW_OWN_SALES
        ));

        return map;
    }

    /** Indica si el rol tiene el permiso. Un rol nulo no tiene ninguno. */
    public static boolean has(Role role, Permission permission) {
        return role != null
                && permission != null
                && PERMISSIONS.getOrDefault(role, Set.of()).contains(permission);
    }

    /** Permisos del rol, en un conjunto que no se puede modificar. */
    public static Set<Permission> permissionsFor(Role role) {
        return Collections.unmodifiableSet(
                PERMISSIONS.getOrDefault(role, Set.of())
        );
    }
}
