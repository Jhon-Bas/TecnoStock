package pe.tecnostock.modules.sales.command;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Comprueba la ejecución y reversión de comandos. */
class CartCommandHistoryTest {
    /** Comprueba el orden inverso de las operaciones. */
    @Test void undoInReverseOrder() {
        var history = new CartCommandHistory();
        var items = new ArrayList<String>();
        history.execute(new CartCommandAction(() -> items.add("A"), () -> items.remove("A")));
        history.execute(new CartCommandAction(() -> items.add("B"), () -> items.remove("B")));
        assertEquals(List.of("A", "B"), items);
        assertTrue(history.undo());
        assertEquals(List.of("A"), items);
        assertTrue(history.undo());
        assertTrue(items.isEmpty());
        assertFalse(history.undo());
    }
    /** Una operación fallida no debe entrar al historial. */
    @Test void failedCommandIsNotRecorded() {
        var history = new CartCommandHistory();
        assertThrows(IllegalStateException.class, () -> history.execute(
                new CartCommandAction(() -> { throw new IllegalStateException(); }, () -> fail())));
        assertFalse(history.undo());
    }
    /** Impide deshacer acciones de una venta finalizada. */
    @Test void clearHistory() {
        var history = new CartCommandHistory();
        history.execute(new CartCommandAction(() -> {}, () -> fail()));
        history.clear();
        assertFalse(history.undo());
    }
}