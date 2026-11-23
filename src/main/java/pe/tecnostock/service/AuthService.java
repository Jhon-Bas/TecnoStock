
package pe.tecnostock.service;

import pe.tecnostock.model.User;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.repository.UserRepository.Credentials;
import pe.tecnostock.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Gestiona la autenticación de usuarios de TecnoStock.
 *
 * Comprueba las credenciales y verifica que la cuenta
 * se encuentre activa antes de permitir el acceso.
 */
public class AuthService {

    private final UserRepository userRepository;

    /**
     * Recibe el repositorio que permite consultar
     * las cuentas almacenadas en la base de datos.
     */
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Intenta autenticar a un usuario.
     *
     * Por seguridad, el arreglo recibido se limpia
     * al finalizar la operación.
     *
     * @param username Nombre de usuario.
     * @param password Contraseña ingresada.
     * @return Usuario autenticado o un Optional vacío.
     * @throws SQLException Si falla la consulta a SQLite.
     */
    public Optional<User> login(
            String username,
            char[] password
    ) throws SQLException {

        try {
            if (username == null || password == null) {
                return Optional.empty();
            }

            String normalizedUsername = username.trim();

            if (normalizedUsername.isEmpty()
                    || password.length == 0) {
                return Optional.empty();
            }

            Optional<Credentials> account =
                    userRepository.findByUsername(
                            normalizedUsername
                    );

            if (account.isEmpty()) {
                return Optional.empty();
            }

            Credentials credentials = account.get();

            // Las cuentas desactivadas no pueden acceder.
            if (!credentials.user().isActive()) {
                return Optional.empty();
            }

            boolean validPassword =
                    PasswordUtil.verifyPassword(
                            password,
                            credentials.passwordHash(),
                            credentials.passwordSalt()
                    );

            if (!validPassword) {
                return Optional.empty();
            }

            return Optional.of(credentials.user());

        } finally {
            // Eliminamos la contraseña del arreglo recibido.
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
