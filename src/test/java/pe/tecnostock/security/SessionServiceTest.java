package pe.tecnostock.security;

import org.junit.jupiter.api.Test;

import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.service.SessionService;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba las reglas de vigencia de sesiones.
 *
 * Utiliza un repositorio simulado para evitar
 * modificaciones en la base de datos real.
 */
public class SessionServiceTest {

    /**
     * Simula la información que devuelve SQLite.
     */
    private static class FakeUserRepository
            extends UserRepository {

        private final User storedUser;

        FakeUserRepository(User storedUser) {
            this.storedUser = storedUser;
        }

        @Override
        public Optional<Credentials> findByUsername(
                String username
        ) throws SQLException {

            if (storedUser == null
                    || !storedUser.getUsername().equals(username)) {
                return Optional.empty();
            }

            return Optional.of(new Credentials(
                    storedUser,
                    "hash-de-prueba",
                    "salt-de-prueba"
            ));
        }
    }

    /**
     * Acepta una sesión que conserva sus datos.
     */
    @Test
    void shouldAcceptValidSession() throws SQLException {

        User session = new User(
                1, 10, "admin", Role.ADMIN, true, 3
        );

        User stored = new User(
                1, 10, "admin", Role.ADMIN, true, 3
        );

        SessionService service = new SessionService(
                new FakeUserRepository(stored)
        );

        assertTrue(service.isSessionValid(session));
    }

    /**
     * Rechaza una cuenta desactivada.
     */
    @Test
    void shouldRejectInactiveAccount() throws SQLException {

        User session = new User(
                2, 20, "gerente01", Role.GERENTE, true, 1
        );

        User stored = new User(
                2, 20, "gerente01", Role.GERENTE, false, 2
        );

        SessionService service = new SessionService(
                new FakeUserRepository(stored)
        );

        assertFalse(service.isSessionValid(session));
    }

    /**
     * Rechaza una sesión cuando cambia su rol.
     */
    @Test
    void shouldRejectChangedRole() throws SQLException {

        User session = new User(
                3, 30, "usuario01", Role.GERENTE, true, 4
        );

        User stored = new User(
                3, 30, "usuario01", Role.EMPLEADO, true, 5
        );

        SessionService service = new SessionService(
                new FakeUserRepository(stored)
        );

        assertFalse(service.isSessionValid(session));
    }

    /**
     * Rechaza sesiones anteriores a un cambio
     * de contraseña o modificación sensible.
     */
    @Test
    void shouldRejectOldSessionVersion()
            throws SQLException {

        User session = new User(
                4, 40, "empleado01", Role.EMPLEADO, true, 2
        );

        User stored = new User(
                4, 40, "empleado01", Role.EMPLEADO, true, 3
        );

        SessionService service = new SessionService(
                new FakeUserRepository(stored)
        );

        assertFalse(service.isSessionValid(session));
    }

    /**
     * Rechaza sesiones de cuentas inexistentes.
     */
    @Test
    void shouldRejectMissingAccount() throws SQLException {

        User session = new User(
                5, 50, "inexistente", Role.EMPLEADO, true, 0
        );

        SessionService service = new SessionService(
                new FakeUserRepository(null)
        );

        assertFalse(service.isSessionValid(session));
    }
}