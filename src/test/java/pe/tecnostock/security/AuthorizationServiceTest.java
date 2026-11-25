package pe.tecnostock.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import pe.tecnostock.service.AuthorizationService;
import pe.tecnostock.config.TemporaryDatabase;
import pe.tecnostock.config.Database;
import pe.tecnostock.config.migration.SchemaMigrator;
import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.admin.repository.EmployeeRepository;
import pe.tecnostock.modules.admin.service.EmployeeService;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.service.BootstrapService;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba la autorización por permiso y por sesión vigente,
 * usando una base SQLite temporal.
 */
@ResourceLock("tecnostock.db.path")
public class AuthorizationServiceTest {

    @TempDir
    Path tempDirectory;

    private String previousPath;

    private UserRepository userRepository;
    private EmployeeService employeeService;
    private AuthorizationService authorizationService;

    private User admin;
    private User manager;
    private User employee;

    /** Prepara cuentas de prueba en una base temporal. */
    @BeforeEach
    void setUp() throws SQLException {

        previousPath = TemporaryDatabase.use(tempDirectory);
        new SchemaMigrator().migrate();

        userRepository = new UserRepository();
        employeeService = new EmployeeService(
                new EmployeeRepository(), userRepository
        );
        authorizationService = new AuthorizationService();

        new BootstrapService().createInitialAdmin(
                "Administrador", "Prueba", "12345678",
                "admin_test", "AdminPrueba2026!".toCharArray()
        );
        admin = findUser("admin_test");

        createAccount("gerente01", "11111111", Role.GERENTE);
        createAccount("empleado01", "22222222", Role.EMPLEADO);

        // Simula que ya cambiaron su contraseña temporal.
        confirmPasswordChange("gerente01");
        confirmPasswordChange("empleado01");

        manager = findUser("gerente01");
        employee = findUser("empleado01");
    }

    /** Restaura la configuración previa de la base de datos. */
    @AfterEach
    void tearDown() {
        TemporaryDatabase.restore(previousPath);
    }

    /** Autoriza operaciones permitidas con sesión vigente. */
    @Test
    void shouldAuthorizePermittedRole() {

        assertDoesNotThrow(() -> authorizationService.requirePermission(
                admin, Permission.MANAGE_USERS));
        assertDoesNotThrow(() -> authorizationService.requirePermission(
                manager, Permission.MANAGE_PRODUCTS));
        assertDoesNotThrow(() -> authorizationService.requirePermission(
                employee, Permission.REGISTER_SALES));
    }

    /** Rechaza operaciones fuera del rol. */
    @Test
    void shouldRejectForbiddenRole() {

        assertThrows(SecurityException.class,
                () -> authorizationService.requirePermission(
                        manager, Permission.MANAGE_USERS));
        assertThrows(SecurityException.class,
                () -> authorizationService.requirePermission(
                        employee, Permission.VIEW_REPORTS));
    }

    /** Rechaza una sesión inexistente. */
    @Test
    void shouldRejectNullUser() {

        assertThrows(SecurityException.class,
                () -> authorizationService.requirePermission(
                        null, Permission.VIEW_REPORTS));
    }

    /** Bloquea cuentas con contraseña temporal pendiente. */
    @Test
    void shouldRejectPendingPasswordChange()
            throws SQLException {

        createAccount("gerente02", "33333333", Role.GERENTE);
        User pending = findUser("gerente02");

        assertThrows(SecurityException.class,
                () -> authorizationService.requirePermission(
                        pending, Permission.VIEW_REPORTS));
    }

    /** Invalida una sesión tras desactivar la cuenta. */
    @Test
    void shouldInvalidateDisabledAccountSession()
            throws SQLException {

        employeeService.changeEmployeeStatus(
                admin, manager.getEmployeeId(), false
        );

        // El objeto User es el de antes de desactivar la cuenta.
        assertThrows(SecurityException.class,
                () -> authorizationService.requirePermission(
                        manager, Permission.MANAGE_PRODUCTS));
    }

    /** Registra una cuenta mediante el servicio administrativo. */
    private void createAccount(String currentUser, String document, Role role)
            throws SQLException {

        employeeService.createEmployee(
                admin, "Empleado", "Prueba", document,
                currentUser, role, "Temporal2026!".toCharArray()
        );
    }

    /** Recupera la cuenta creada para las pruebas. */
    private User findUser(String currentUser) throws SQLException {
        return userRepository.findByUsername(currentUser)
                .orElseThrow().user();
    }

    /** Simula el cambio obligatorio de contraseña en la base temporal. */
    private void confirmPasswordChange(String currentUser)
            throws SQLException {

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE usuarios SET must_change_password = 0 "
                             + "WHERE username = ?"
             )) {
            statement.setString(1, currentUser);
            statement.executeUpdate();
        }
    }
}
