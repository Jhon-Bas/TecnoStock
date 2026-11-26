package pe.tecnostock.util;

/**
 * Apoyo para consultas SQL compartidas por los repositorios.
 */
public final class SqlUtil {

    private SqlUtil() {
        // Impide crear instancias de esta clase.
    }

    /**
     * Evita que % o _ escritos por el usuario actúen como comodines
     * en un LIKE. La consulta debe declarar ESCAPE '\'.
     */
    public static String escapeLike(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}