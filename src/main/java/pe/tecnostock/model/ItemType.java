package pe.tecnostock.model;

/**
 * Clases de elementos que TecnoStock puede vender.
 */
public enum ItemType {

    PRODUCT("Producto"),
    SERVICE("Servicio");

    private final String label;

    ItemType(String label) {
        this.label = label;
    }

    /** Texto que se muestra al usuario (listas y tablas). */
    @Override
    public String toString() {
        return label;
    }
}