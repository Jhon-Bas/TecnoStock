
package pe.tecnostock.service;

import pe.tecnostock.model.User;
import pe.tecnostock.repository.UserRepository;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Comprueba la vigencia de una sesión autenticada.
 *
 * Detecta cuentas desactivadas, modificaciones
 * de rol y cambios de contraseña realizados
 * después del inicio de sesión.
 */
public class SessionService {

    private final UserRepository userRepository;

    /**
     * Recibe el repositorio utilizado para consultar
     * el estado real de las cuentas.
     */
    public SessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Verifica que los datos actuales de la cuenta
     * coincidan con los de la sesión original.
     *
     * @return true si la sesión continúa siendo válida.
     */
    public boolean isSessionValid(User currentUser)
            throws SQLException {

        if (currentUser == null) {
            return false;
        }

        Optional<UserRepository.Credentials> account =
                userRepository.findByUsername(
                        currentUser.getUsername()
                );

        if (account.isEmpty()) {
            return false;
        }

        User storedUser = account.get().user();

        // Rechaza cuentas que fueron desactivadas.
        if (!storedUser.isActive()) {
            return false;
        }

        // Comprueba que sea la misma cuenta y empleado.
        if (storedUser.getId() != currentUser.getId()
                || storedUser.getEmployeeId()
                   != currentUser.getEmployeeId()) {
            return false;
        }

        // Exige iniciar sesión de nuevo tras cambiar el rol.
        if (storedUser.getRole() != currentUser.getRole()) {
            return false;
        }

        // Invalida la sesión si la contraseña, el estado
        // o el rol cambiaron desde el inicio de sesión.
        return storedUser.getSessionVersion()
                == currentUser.getSessionVersion();
    }
}
