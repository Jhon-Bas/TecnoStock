package pe.tecnostock.security;

/**
 * Operaciones del negocio que requieren autorización.
 *
 * Los servicios piden un permiso concreto, no un rol.
 * Así, cambiar qué rol puede hacer qué se resuelve en
 * PermissionPolicy sin tocar el resto del sistema.
 */
public enum Permission {

    // Administración
    MANAGE_USERS,
    VIEW_AUDIT,

    // Clientes
    VIEW_CUSTOMERS,
    REGISTER_CUSTOMERS,
    MANAGE_CUSTOMERS,

    // Productos y servicios
    VIEW_PRODUCTS,
    MANAGE_PRODUCTS,

    // Inventario
    MANAGE_INVENTORY,

    // Ventas
    REGISTER_SALES,
    VIEW_OWN_SALES,
    VIEW_ALL_SALES,
    APPLY_DISCOUNT,

    // Reportes
    VIEW_REPORTS
}
