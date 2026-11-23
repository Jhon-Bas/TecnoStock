
package pe.tecnostock.model;

/**
 * Representa una cuenta de acceso de TecnoStock.
 *
 * Contiene la identidad, el rol, el estado y la
 * versión de sesión del usuario autenticado.
 */
public class User {

    private final int id;
    private final int employeeId;
    private final String username;

    private Role role;
    private boolean active;
    private final int sessionVersion;

    /**
     * Construye un usuario sin versión explícita.
     *
     * Se conserva este constructor para mantener
     * compatibilidad con el código existente.
     */
    public User(
            int id,
            int employeeId,
            String username,
            Role role,
            boolean active
    ) {
        this(id, employeeId, username, role, active, 0);
    }

    /**
     * Construye un usuario con la versión de sesión
     * registrada en la base de datos.
     */
    public User(
            int id,
            int employeeId,
            String username,
            Role role,
            boolean active,
            int sessionVersion
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.username = username;
        this.role = role;
        this.active = active;
        this.sessionVersion = sessionVersion;
    }

    /** Obtiene el identificador de la cuenta. */
    public int getId() {
        return id;
    }

    /** Obtiene el identificador del empleado. */
    public int getEmployeeId() {
        return employeeId;
    }

    /** Obtiene el nombre de acceso. */
    public String getUsername() {
        return username;
    }

    /** Obtiene el rol del usuario. */
    public Role getRole() {
        return role;
    }

    /** Indica si la cuenta se encuentra activa. */
    public boolean isActive() {
        return active;
    }

    /** Obtiene la versión capturada al iniciar sesión. */
    public int getSessionVersion() {
        return sessionVersion;
    }

    /** Actualiza el rol del objeto en memoria. */
    public void setRole(Role role) {
        this.role = role;
    }

    /** Actualiza el estado del objeto en memoria. */
    public void setActive(boolean active) {
        this.active = active;
    }
}
