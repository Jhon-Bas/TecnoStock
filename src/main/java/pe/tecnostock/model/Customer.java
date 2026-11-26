package pe.tecnostock.model;

import java.util.regex.Pattern;

/**
 * Comprador de TecnoStock.
 *
 * Valida sus propios datos: un cliente con documento, nombre,
 * teléfono o correo inválidos no puede llegar a existir. El
 * documento es un DNI (8 dígitos) o un RUC (11 dígitos).
 */
public class Customer {

    private static final Pattern DOCUMENT = Pattern.compile("\\d{8}|\\d{11}");
    private static final Pattern PHONE = Pattern.compile("[0-9+\\- ]{6,15}");
    private static final Pattern EMAIL =
            Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final int id;
    private String document;
    private String name;
    private String phone;
    private String email;
    private String address;
    private boolean active;

    /**
     * @param id 0 si todavía no se guardó en la base de datos
     * @throws IllegalArgumentException si algún dato es inválido
     */
    public Customer(
            int id,
            String document,
            String name,
            String phone,
            String email,
            String address,
            boolean active
    ) {
        this.id = id;
        setDocument(document);
        setName(name);
        setPhone(phone);
        setEmail(email);
        setAddress(address);
        this.active = active;
    }

    /** Cliente nuevo: sin id todavía y activo. */
    public static Customer create(
            String document,
            String name,
            String phone,
            String email,
            String address
    ) {
        return new Customer(
                0, document, name, phone, email, address, true
        );
    }

    public int getId() {
        return id;
    }

    public String getDocument() {
        return document;
    }

    /** Valida y asigna el documento de identidad. */
    public void setDocument(String document) {

        String value = document == null ? "" : document.trim();

        if (!DOCUMENT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "El documento debe ser un DNI de 8 dígitos "
                    + "o un RUC de 11 dígitos."
            );
        }

        this.document = value;
    }

    /** "DNI" o "RUC", según la longitud del documento. */
    public String getDocumentType() {
        return document.length() == 8 ? "DNI" : "RUC";
    }

    public String getName() {
        return name;
    }

    /** Valida y asigna el nombre o razón social. */
    public void setName(String name) {

        String value = name == null ? "" : name.trim();

        if (value.isEmpty() || value.length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre o razón social es obligatorio "
                    + "(máximo 120 caracteres)."
            );
        }

        this.name = value;
    }

    public String getPhone() {
        return phone;
    }

    /** Valida y asigna el teléfono opcional. */
    public void setPhone(String phone) {

        String value = blankToNull(phone);

        if (value != null && !PHONE.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "El teléfono debe tener entre 6 y 15 caracteres: "
                    + "dígitos, espacios, + o guion."
            );
        }

        this.phone = value;
    }

    public String getEmail() {
        return email;
    }

    /** Valida y asigna el correo opcional. */
    public void setEmail(String email) {

        String value = blankToNull(email);

        if (value != null
                && (value.length() > 100
                    || !EMAIL.matcher(value).matches())) {
            throw new IllegalArgumentException(
                    "El correo electrónico no es válido."
            );
        }

        this.email = value;
    }

    public String getAddress() {
        return address;
    }

    /** Valida y asigna la dirección opcional. */
    public void setAddress(String address) {

        String value = blankToNull(address);

        if (value != null && value.length() > 150) {
            throw new IllegalArgumentException(
                    "La dirección no puede superar 150 caracteres."
            );
        }

        this.address = value;
    }

    public boolean isActive() {
        return active;
    }

    /** Actualiza el estado del cliente. */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** Los campos opcionales vacíos se guardan como null. */
    private static String blankToNull(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}