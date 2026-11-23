
package pe.tecnostock.security;

import org.junit.jupiter.api.Test;
import pe.tecnostock.util.PasswordUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de protección de contraseñas.
 *
 * Verifica que las contraseñas se almacenen mediante
 * hash y que la comprobación de credenciales funcione.
 */
public class PasswordUtilTest {

    /**
     * Una contraseña correcta debe coincidir
     * con el hash generado originalmente.
     */
    @Test
    void shouldAcceptCorrectPassword() {

        String salt = PasswordUtil.generateSalt();

        String hash = PasswordUtil.hashPassword(
                "ClavePrueba2026!".toCharArray(),
                salt
        );

        boolean valid = PasswordUtil.verifyPassword(
                "ClavePrueba2026!".toCharArray(),
                hash,
                salt
        );

        assertTrue(valid);
    }

    /**
     * Una contraseña diferente debe ser rechazada.
     */
    @Test
    void shouldRejectIncorrectPassword() {

        String salt = PasswordUtil.generateSalt();

        String hash = PasswordUtil.hashPassword(
                "ClaveOriginal2026!".toCharArray(),
                salt
        );

        boolean valid = PasswordUtil.verifyPassword(
                "ClaveIncorrecta2026!".toCharArray(),
                hash,
                salt
        );

        assertFalse(valid);
    }

    /**
     * Dos salts independientes deben generar
     * hashes diferentes para una misma contraseña.
     */
    @Test
    void shouldGenerateDifferentHashesWithDifferentSalts() {

        String firstSalt = PasswordUtil.generateSalt();
        String secondSalt = PasswordUtil.generateSalt();

        assertNotEquals(firstSalt, secondSalt);

        String firstHash = PasswordUtil.hashPassword(
                "MismaClave2026!".toCharArray(),
                firstSalt
        );

        String secondHash = PasswordUtil.hashPassword(
                "MismaClave2026!".toCharArray(),
                secondSalt
        );

        assertNotEquals(firstHash, secondHash);
    }
}
