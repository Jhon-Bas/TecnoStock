package pe.tecnostock.modules.sales.command;

import java.util.Objects;

/** Comando concreto que encapsula una acción y su operación inversa. */
public final class CartCommandAction implements CartCommand {
    private final Runnable action;
    private final Runnable reverse;
    /** Recibe las operaciones directa e inversa del receptor. */
    public CartCommandAction(Runnable action, Runnable reverse) {
        this.action = Objects.requireNonNull(action);
        this.reverse = Objects.requireNonNull(reverse);
    }
    /** Ejecuta la acción encapsulada. */
    @Override public void execute() { action.run(); }
    /** Ejecuta la operación inversa. */
    @Override public void undo() { reverse.run(); }
}