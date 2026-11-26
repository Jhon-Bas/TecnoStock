package pe.tecnostock.modules.sales.command;

/** Encapsula una operación reversible del carrito. */
public interface CartCommand {
    /** Ejecuta la operación. */
    void execute();
    /** Revierte la operación ejecutada. */
    void undo();
}