package pe.tecnostock.model;

/**
 * Clasificación de los productos (laptops, periféricos, etc.).
 */
public record Category(int id, String name) {

    public Category {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la categoría es obligatorio."
            );
        }
        name = name.trim();
    }

    /** Permite mostrar la categoría directamente en una lista. */
    @Override
    public String toString() {
        return name;
    }
}