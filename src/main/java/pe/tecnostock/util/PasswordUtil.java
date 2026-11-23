
package pe.tecnostock.util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Proporciona métodos para proteger y verificar contraseñas.
 *
 * Utiliza PBKDF2 con SHA-256 y un salt aleatorio
 * para evitar almacenar contraseñas en texto plano.
 */
public final class PasswordUtil {

    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private PasswordUtil() {
        // Impide crear objetos de esta clase utilitaria.
    }

    /**
     * Genera un salt aleatorio para una contraseña.
     *
     * @return Salt codificado en Base64.
     */
    public static String generateSalt() {

        byte[] salt = new byte[SALT_LENGTH];

        RANDOM.nextBytes(salt);

        return Base64.getEncoder()
                .encodeToString(salt);
    }

    /**
     * Convierte una contraseña en un hash seguro.
     *
     * @param password Contraseña ingresada.
     * @param saltBase64 Salt generado previamente.
     * @return Hash de la contraseña codificado en Base64.
     */
    public static String hashPassword(
            char[] password, String saltBase64) {

        byte[] salt = Base64.getDecoder()
                .decode(saltBase64);

        KeySpec spec = new PBEKeySpec(
                password, salt, ITERATIONS, KEY_LENGTH
        );

        try {

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] hash =
                    factory.generateSecret(spec).getEncoded();

            try {
                return Base64.getEncoder()
                        .encodeToString(hash);
            } finally {
                Arrays.fill(hash, (byte) 0);
            }

        } catch (Exception e) {

            throw new IllegalStateException(
                    "No se pudo proteger la contraseña.",
                    e
            );

        } finally {

            // Libera la contraseña almacenada en la especificación.
            ((PBEKeySpec) spec).clearPassword();
            Arrays.fill(salt, (byte) 0);
        }
    }

    /**
     * Verifica una contraseña ingresada contra un hash.
     *
     * @return true cuando la contraseña es correcta.
     */
    public static boolean verifyPassword(
            char[] password,
            String storedHash,
            String storedSalt) {

        String calculatedHash =
                hashPassword(password, storedSalt);

        byte[] expected = Base64.getDecoder()
                .decode(storedHash);

        byte[] actual = Base64.getDecoder()
                .decode(calculatedHash);

        try {
            return java.security.MessageDigest.isEqual(
                    expected, actual
            );
        } finally {
            Arrays.fill(expected, (byte) 0);
            Arrays.fill(actual, (byte) 0);
        }
    }
}
