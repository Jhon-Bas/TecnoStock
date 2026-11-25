package pe.tecnostock.modules.sales.command;

import java.util.ArrayDeque;
import java.util.Deque;

/** Ejecuta comandos y conserva su historial reversible. */
public final class CartCommandHistory {
    private final Deque<CartCommand> history = new ArrayDeque<>();
    /** Registra únicamente operaciones ejecutadas correctamente. */
    public void execute(CartCommand command) {
        command.execute();
        history.push(command);
    }
    /** Revierte la última operación, si existe. */
    public boolean undo() {
        if (history.isEmpty()) return false;
        history.peek().undo();
        history.pop();
        return true;
    }
    /** Descarta el historial al completar una venta. */
    public void clear() { history.clear(); }
}