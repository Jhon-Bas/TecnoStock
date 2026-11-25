package pe.tecnostock.model;

/**
 * Clases de movimiento de inventario.
 *
 * Cada tipo sabe calcular el stock que resulta de aplicarlo y rechaza
 * lo que no es vÃ¡lido: cantidades que no son positivas, salidas que
 * superan lo disponible y existencias negativas. Es lÃ³gica pura, sin
 * base de datos.
 */
public enum MovementType {

    /** Ingreso de mercaderÃ­a: suma la cantidad al stock. */
    ENTRY("Entrada", "ENTRADA") {
        @Override
        public int calculateStock(int currentStock, int quantity) {
            requirePositive(quantity);
            return requireWithinMaximum((long) currentStock + quantity);
        }
    },

    /** Salida de mercaderÃ­a: resta la cantidad, sin dejar stock negativo. */
    EXIT("Salida", "SALIDA") {
        @Override
        public int calculateStock(int currentStock, int quantity) {
            requirePositive(quantity);

            if (quantity > currentStock) {
                throw new IllegalArgumentException(
                        "Stock insuficiente: hay " + currentStock
                        + " unidades disponibles."
                );
            }

            return currentStock - quantity;
        }
    },

    /**
     * CorrecciÃ³n tras contar fÃ­sicamente: la cantidad es el stock
     * real contado, que reemplaza al registrado.
     */
    ADJUSTMENT("Ajuste", "AJUSTE") {
        @Override
        public int calculateStock(int currentStock, int quantity) {

            if (quantity < 0) {
                throw new IllegalArgumentException(
                        "El stock contado no puede ser negativo."
                );
            }

            requireWithinMaximum(quantity);

            if (quantity == currentStock) {
                throw new IllegalArgumentException(
                        "El stock contado es igual al actual: "
                        + "no hay nada que ajustar."
                );
            }

            return quantity;
        }
    };

    /** Tope de unidades por producto, para evitar cifras absurdas. */
    public static final int MAX_STOCK = 1_000_000;

    private final String label;

    private final String databaseValue;

    MovementType(String label, String databaseValue) {
        this.databaseValue = databaseValue;
        this.label = label;
    }

    /**
     * Stock que resulta de aplicar este movimiento.
     *
     * @param cantidad unidades; en un AJUSTE, el stock contado
     * @throws IllegalArgumentException si el movimiento no es vÃ¡lido
     */
    public abstract int calculateStock(int currentStock, int quantity);

    /** Devuelve el valor persistido, compatible con el esquema existente. */
    public String databaseValue() { return databaseValue; }

    /** Recupera el tipo a partir del valor almacenado en SQLite. */
    public static MovementType fromDatabase(String value) {
        for (MovementType type : values()) {
            if (type.databaseValue.equals(value)) return type;
        }
        throw new IllegalArgumentException("Tipo de movimiento desconocido: " + value);
    }

    /** Texto que se muestra al usuario (listas y tablas). */
    @Override
    public String toString() {
        return label;
    }

    private static void requirePositive(int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero."
            );
        }
    }

    private static int requireWithinMaximum(long stock) {

        if (stock > MAX_STOCK) {
            throw new IllegalArgumentException(
                    "El stock no puede superar " + MAX_STOCK
                    + " unidades."
            );
        }

        return (int) stock;
    }
}