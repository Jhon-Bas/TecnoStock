package pe.tecnostock.model;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Datos comunes de lo que TecnoStock comercializa.
 *
 * Es la base de Producto y de Servicio: ambos tienen código,
 * nombre, descripción, precio de venta y estado. Valida sus datos,
 * por lo que un elemento inválido no puede llegar a existir.
 *
 * Es sellada: solo Producto y Servicio pueden extenderla. Así, si
 * se agrega un tipo nuevo, el compilador avisa dónde falta tratarlo.
 */
public abstract sealed class CommercialItem implements Sellable
        permits Product, ServiceItem {

    private static final Pattern CODE = Pattern.compile("[A-Z0-9-]{3,20}");
    private static final BigDecimal MAX_PRICE =
            new BigDecimal("999999.99");

    private final int id;
    private String code;
    private String name;
    private String description;
    private BigDecimal salePrice;
    private boolean active;

    /**
     * @param id 0 si todavía no se guardó en la base de datos
     * @throws IllegalArgumentException si algún dato es inválido
     */
    protected CommercialItem(
            int id,
            String code,
            String name,
            String description,
            BigDecimal salePrice,
            boolean active
    ) {
        this.id = id;
        setCode(code);
        setName(name);
        setDescription(description);
        setSalePrice(salePrice);
        this.active = active;
    }

    @Override
    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public final void setCode(String code) {

        String valor = code == null
                ? "" : code.trim().toUpperCase();

        if (!CODE.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                    "El código debe tener entre 3 y 20 caracteres: "
                    + "letras, números o guion."
            );
        }

        this.code = valor;
    }

    @Override
    public String getName() {
        return name;
    }

    public final void setName(String name) {

        String valor = name == null ? "" : name.trim();

        if (valor.isEmpty() || valor.length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio (máximo 120 caracteres)."
            );
        }

        this.name = valor;
    }

    public String getDescription() {
        return description;
    }

    public final void setDescription(String description) {

        String valor = description == null || description.isBlank()
                ? null : description.trim();

        if (valor != null && valor.length() > 200) {
            throw new IllegalArgumentException(
                    "La descripción no puede superar 200 caracteres."
            );
        }

        this.description = valor;
    }

    @Override
    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public final void setSalePrice(BigDecimal salePrice) {
        this.salePrice =
                normalizePrice(salePrice, "El precio de venta", false);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Valida un importe y lo deja con exactamente 2 decimales.
     *
     * @param permiteCero true si 0.00 es un valor aceptable
     */
    protected static BigDecimal normalizePrice(
            BigDecimal valor,
            String field,
            boolean permiteCero
    ) {
        boolean valido = valor != null
                && valor.stripTrailingZeros().scale() <= 2
                && valor.compareTo(MAX_PRICE) <= 0
                && (permiteCero
                    ? valor.signum() >= 0
                    : valor.signum() > 0);

        if (!valido) {
            throw new IllegalArgumentException(
                    field + (permiteCero
                            ? " no puede ser negativo"
                            : " debe ser mayor que cero")
                    + " y tener como máximo 2 decimales."
            );
        }

        return valor.setScale(2);
    }
}