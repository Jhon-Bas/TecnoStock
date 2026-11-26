package pe.tecnostock.inventory;

import org.junit.jupiter.api.Test;

import pe.tecnostock.model.MovementType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprueba las reglas de stock de cada movimiento. No usa base de datos.
 */
public class MovementTypeTest {

    @Test
    void entryShouldIncreaseStock() {

        assertEquals(15, MovementType.ENTRY.calculateStock(10, 5));
        assertEquals(5, MovementType.ENTRY.calculateStock(0, 5));
    }

    @Test
    void exitShouldDecreaseStock() {

        assertEquals(5, MovementType.EXIT.calculateStock(10, 5));
        assertEquals(0, MovementType.EXIT.calculateStock(10, 10));
    }

    @Test
    void shouldRejectNegativeStock() {

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> MovementType.EXIT.calculateStock(3, 4));

        assertTrue(error.getMessage().contains("3"));
    }

    @Test
    void entryAndExitRequirePositiveQuantity() {

        for (int invalid : new int[] {0, -1, -100}) {

            assertThrows(IllegalArgumentException.class,
                    () -> MovementType.ENTRY.calculateStock(10, invalid));
            assertThrows(IllegalArgumentException.class,
                    () -> MovementType.EXIT.calculateStock(10, invalid));
        }
    }

    @Test
    void adjustmentShouldSetCountedStock() {

        assertEquals(3, MovementType.ADJUSTMENT.calculateStock(10, 3));
        assertEquals(12, MovementType.ADJUSTMENT.calculateStock(10, 12));
        assertEquals(0, MovementType.ADJUSTMENT.calculateStock(10, 0));
    }

    @Test
    void adjustmentShouldRejectInvalidValues() {

        assertThrows(IllegalArgumentException.class,
                () -> MovementType.ADJUSTMENT.calculateStock(10, -1));
        assertThrows(IllegalArgumentException.class,
                () -> MovementType.ADJUSTMENT.calculateStock(10, 10));
    }

    @Test
    void shouldRespectStockLimit() {

        int maximum = MovementType.MAX_STOCK;

        assertEquals(maximum,
                MovementType.ENTRY.calculateStock(maximum - 1, 1));

        assertThrows(IllegalArgumentException.class,
                () -> MovementType.ENTRY.calculateStock(maximum, 1));
        assertThrows(IllegalArgumentException.class,
                () -> MovementType.ENTRY.calculateStock(
                        1, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> MovementType.ADJUSTMENT.calculateStock(5, maximum + 1));
    }
}