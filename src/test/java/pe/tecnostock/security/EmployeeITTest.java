
package pe.tecnostock.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import pe.tecnostock.config.AuditInitializer;
import pe.tecnostock.config.DatabaseInitializer;
import pe.tecnostock.config.PasswordPolicyInitializer;
import pe.tecnostock.config.SessionVersionInitializer;

import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;

import pe.tecnostock.modules.admin.repository.AuditRepository;
import pe.tecnostock.modules.admin.repository.EmployeeRepository;
import pe.tecnostock.repository.UserRepository;

import pe.tecnostock.service.AuthService;
import pe.tecnostock.service.BootstrapService;
import pe.tecnostock.modules.admin.service.EmployeeService;
import pe.tecnostock.service.PasswordChangeService;
import pe.tecnostock.modules.admin.service.PasswordResetService;
import pe.tecnostock.service.SessionService;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del módulo administrativo.
 *
 * Utiliza una base SQLite temporal para comprobar
 * el registro, consulta, edición, permisos,
 * desactivación, contraseñas y auditoría.
 *
 * Nunca utiliza la base de datos principal.
 */
@ResourceLock("tecnostock.db.path")
public class EmployeeITTest {

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;

    private User administrator;

    private UserRepository userRepository;
    private EmployeeRepository employeeRepository;
    private EmployeeService employeeService;

    /**
     * Prepara una base de datos independiente
     * antes de cada prueba.
     */
    @BeforeEach
    void setUp() throws SQLException {

        previousDatabasePath =
                System.getProperty("tecnostock.db.path");

        Path testDatabase = temporaryDirectory.resolve(
                "test-" + System.nanoTime() + ".db"
        );

        System.setProperty(
                "tecnostock.db.path",
                testDatabase.toString()
        );

        // Reproduce la inicialización real de TecnoStock.
        DatabaseInitializer.initialize();
        AuditInitializer.initialize();
        PasswordPolicyInitializer.initialize();
        SessionVersionInitializer.initialize();

        userRepository = new UserRepository();
        employeeRepository = new EmployeeRepository();

        employeeService = new EmployeeService(
                employeeRepository,
                userRepository
        );

        // Crea un administrador ficticio únicamente
        // dentro de la base temporal de esta prueba.
        BootstrapService bootstrap = new BootstrapService();

        bootstrap.createInitialAdmin(
                "Administrador",
                "Prueba",
                "12345678",
                "admin_test",
                "AdminPrueba2026!".toCharArray()
        );

        administrator = userRepository.findByUsername(
                "admin_test"
        ).orElseThrow().user();
    }

    /**
     * Restaura la configuración de SQLite
     * después de terminar cada prueba.
     */
    @AfterEach
    void tearDown() {

        if (previousDatabasePath == null) {
            System.clearProperty("tecnostock.db.path");
        } else {
            System.setProperty(
                    "tecnostock.db.path",
                    previousDatabasePath
            );
        }
    }

    /**
     * Registra una cuenta de prueba con datos distintos.
     */
    private void createEmployee(
            String username,
            String document,
            Role role
    ) throws SQLException {

        employeeService.createEmployee(
                administrator,
                "Empleado",
                "Prueba",
                document,
                username,
                role,
                "Temporal2026!".toCharArray()
        );
    }

    /**
     * Comprueba el registro de empleados,
     * el creador de la cuenta y la paginación.
     */
    @Test
    void shouldCreateAndPaginateEmployees()
            throws SQLException {

        createEmployee(
                "gerente01", "11111111", Role.GERENTE
        );

        createEmployee(
                "empleado01", "22222222", Role.EMPLEADO
        );

        createEmployee(
                "empleado02", "33333333", Role.EMPLEADO
        );

        var firstPage = employeeService.listEmployees(
                administrator, "", 0, 2
        );

        var secondPage = employeeService.listEmployees(
                administrator, "", 1, 2
        );

        // Incluye también al administrador inicial.
        assertEquals(4, firstPage.total());
        assertEquals(2, firstPage.employees().size());
        assertEquals(2, secondPage.employees().size());

        var managerPage = employeeService.listEmployees(
                administrator, "gerente01", 0, 10
        );

        assertEquals(1, managerPage.total());

        var manager = managerPage.employees().get(0);

        assertEquals("GERENTE", manager.role());
        assertTrue(manager.active());
        assertEquals("admin_test", manager.createdBy());
        assertNotNull(manager.createdAt());
    }

    /**
     * Comprueba que un cambio de rol invalide
     * la sesión que el usuario tenía abierta.
     */
    @Test
    void shouldUpdateRoleAndInvalidateSession()
            throws SQLException {

        createEmployee(
                "gerente01", "11111111", Role.GERENTE
        );

        User oldSession = userRepository.findByUsername(
                "gerente01"
        ).orElseThrow().user();

        var employee = employeeService.listEmployees(
                administrator, "gerente01", 0, 10
        ).employees().get(0);

        employeeService.updateEmployee(
                administrator,
                employee.id(),
                "Empleado",
                "Actualizado",
                "11111111",
                "gerente01",
                Role.EMPLEADO
        );

        User updated = userRepository.findByUsername(
                "gerente01"
        ).orElseThrow().user();

        assertEquals(Role.EMPLEADO, updated.getRole());

        SessionService sessionService =
                new SessionService(userRepository);

        assertFalse(
                sessionService.isSessionValid(oldSession)
        );

        // Comprueba que quedó registrada la acción.
        AuditRepository auditRepository =
                new AuditRepository();

        var audit = auditRepository.findPage(
                "CAMBIO_ROL", 0, 10
        );

        assertEquals(1, audit.total());
        assertEquals(
                "CAMBIO_ROL",
                audit.events().get(0).action()
        );
    }

    /**
     * Comprueba que desactivar una cuenta impida
     * su acceso sin eliminar sus datos.
     */
    @Test
    void shouldDeactivateAndReactivateAccount()
            throws SQLException {

        createEmployee(
                "empleado01", "22222222", Role.EMPLEADO
        );

        var employee = employeeService.listEmployees(
                administrator, "empleado01", 0, 10
        ).employees().get(0);

        employeeService.changeEmployeeStatus(
                administrator,
                employee.id(),
                false
        );

        AuthService authService =
                new AuthService(userRepository);

        Optional<User> inactiveLogin = authService.login(
                "empleado01",
                "Temporal2026!".toCharArray()
        );

        assertTrue(inactiveLogin.isEmpty());

        employeeService.changeEmployeeStatus(
                administrator,
                employee.id(),
                true
        );

        Optional<User> activeLogin = authService.login(
                "empleado01",
                "Temporal2026!".toCharArray()
        );

        assertTrue(activeLogin.isPresent());

        // La cuenta continúa existiendo con el mismo ID.
        var restored = employeeService.listEmployees(
                administrator, "empleado01", 0, 10
        ).employees().get(0);

        assertEquals(employee.id(), restored.id());
        assertTrue(restored.active());
    }

    /**
     * Comprueba que un gerente no pueda acceder
     * a las operaciones administrativas.
     */
    @Test
    void shouldRejectManagerAdministrativeActions()
            throws SQLException {

        createEmployee(
                "gerente01", "11111111", Role.GERENTE
        );

        User manager = userRepository.findByUsername(
                "gerente01"
        ).orElseThrow().user();

        assertThrows(
                SecurityException.class,
                () -> employeeService.listEmployees(
                        manager, "", 0, 10
                )
        );

        assertThrows(
                SecurityException.class,
                () -> employeeService.createEmployee(
                        manager,
                        "Usuario",
                        "SinPermiso",
                        "87654321",
                        "sinpermiso",
                        Role.EMPLEADO,
                        "Temporal2026!".toCharArray()
                )
        );
    }

    /**
     * Comprueba que el restablecimiento invalide
     * la contraseña anterior y obligue al cambio.
     */
    @Test
    void shouldResetPasswordAndInvalidateOldSession()
            throws SQLException {

        createEmployee(
                "gerente01", "11111111", Role.GERENTE
        );

        User oldSession = userRepository.findByUsername(
                "gerente01"
        ).orElseThrow().user();

        PasswordResetService resetService =
                new PasswordResetService();

        resetService.resetPassword(
                administrator,
                oldSession.getId(),
                "NuevaTemporal2026!".toCharArray(),
                "NuevaTemporal2026!".toCharArray()
        );

        AuthService authService =
                new AuthService(userRepository);

        assertTrue(authService.login(
                "gerente01",
                "Temporal2026!".toCharArray()
        ).isEmpty());

        assertTrue(authService.login(
                "gerente01",
                "NuevaTemporal2026!".toCharArray()
        ).isPresent());

        SessionService sessionService =
                new SessionService(userRepository);

        assertFalse(
                sessionService.isSessionValid(oldSession)
        );

        PasswordChangeService changeService =
                new PasswordChangeService();

        User newSession = userRepository.findByUsername(
                "gerente01"
        ).orElseThrow().user();

        assertTrue(
                changeService.mustChangePassword(newSession)
        );

        var audit = new AuditRepository().findPage(
                "PASSWORD_RESET", 0, 20
        );

        assertTrue(audit.total() >= 1);
    }
}
