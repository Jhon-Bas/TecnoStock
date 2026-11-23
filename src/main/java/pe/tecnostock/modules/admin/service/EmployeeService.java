
package pe.tecnostock.modules.admin.service;

import pe.tecnostock.service.AuthorizationService;

import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.admin.repository.EmployeeRepository;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Gestiona las reglas de negocio de empleados.
 *
 * Permite crear, consultar, actualizar y cambiar
 * el estado de cuentas administrativas bajo
 * autorización de un usuario ADMIN.
 */
public class EmployeeService {

    private final EmployeeRepository repository;

    private final AuthorizationService authorizationService =
            new AuthorizationService();

    /**
     * Recibe los repositorios utilizados por el módulo.
     *
     * UserRepository se conserva como parámetro para
     * mantener compatibilidad con los formularios actuales.
     */
    public EmployeeService(
            EmployeeRepository repository,
            UserRepository userRepository
    ) {
        this.repository = repository;
    }

    /**
     * Valida los datos comunes del empleado.
     */
    private void validateEmployee(
            String firstName,
            String lastName,
            String document,
            String username,
            Role role
    ) {

        if (firstName == null || firstName.isBlank()
                || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException(
                    "Los nombres y apellidos son obligatorios."
            );
        }

        if (document == null
                || !document.matches("\\d{8}")) {
            throw new IllegalArgumentException(
                    "El DNI debe tener exactamente 8 dígitos."
            );
        }

        if (username == null
                || !username.matches("[a-zA-Z0-9_]{4,30}")) {
            throw new IllegalArgumentException(
                    "El usuario debe tener entre 4 y 30 caracteres: "
                    + "letras, números o guion bajo."
            );
        }

        if (role != Role.GERENTE && role != Role.EMPLEADO) {
            throw new IllegalArgumentException(
                    "Solo puedes asignar GERENTE o EMPLEADO."
            );
        }
    }

    /**
     * Crea un empleado y una cuenta de acceso.
     *
     * La contraseña se almacena mediante hash
     * y se limpia del arreglo recibido.
     */
    public void createEmployee(
            User currentUser,
            String firstName,
            String lastName,
            String document,
            String username,
            Role role,
            char[] password
    ) throws SQLException {

        try {
            authorizationService.requireAdmin(currentUser);

            validateEmployee(
                    firstName, lastName, document, username, role
            );

            if (password == null || password.length < 10) {
                throw new IllegalArgumentException(
                        "La contraseña debe tener al menos "
                        + "10 caracteres."
                );
            }

            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(password, salt);

            repository.create(
                    firstName.trim(),
                    lastName.trim(),
                    document,
                    username,
                    role,
                    hash,
                    salt,
                    currentUser.getId()
            );

        } finally {
            // Limpia la contraseña independientemente
            // del resultado de la operación.
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Consulta empleados mediante búsqueda
     * y paginación desde SQLite.
     */
    public EmployeeRepository.EmployeePage listEmployees(
            User currentUser,
            String search,
            int page,
            int pageSize
    ) throws SQLException {

        authorizationService.requireAdmin(currentUser);

        return repository.findPage(
                search == null ? "" : search.trim(),
                page,
                pageSize
        );
    }

    /**
     * Modifica los datos personales y el rol.
     *
     * Las cuentas ADMIN están protegidas
     * por el repositorio.
     */
    public void updateEmployee(
            User currentUser,
            int employeeId,
            String firstName,
            String lastName,
            String document,
            String username,
            Role role
    ) throws SQLException {

        authorizationService.requireAdmin(currentUser);

        validateEmployee(
                firstName, lastName, document, username, role
        );

        if (employeeId <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un empleado válido."
            );
        }

        repository.update(
                employeeId,
                firstName.trim(),
                lastName.trim(),
                document,
                username,
                role,
                currentUser.getId()
        );
    }

    /**
     * Activa o desactiva una cuenta sin eliminar
     * la información almacenada en SQLite.
     */
    public void changeEmployeeStatus(
            User currentUser,
            int employeeId,
            boolean active
    ) throws SQLException {

        authorizationService.requireAdmin(currentUser);

        if (employeeId <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un empleado válido."
            );
        }

        repository.setActive(
                employeeId,
                active,
                currentUser.getId()
        );
    }
}
