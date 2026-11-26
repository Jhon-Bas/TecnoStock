package pe.tecnostock.model;

import java.time.LocalDateTime;

/**
 * Constancia de un cambio de stock, tal como quedó registrada.
 *
 * Responde por qué cambió el stock: qué tipo de movimiento fue,
 * cuántas unidades, cómo estaba antes, cómo quedó, quién lo hizo y
 * el motivo. Se lee del historial y no se modifica.
 */
public record InventoryMovement(
        int id,
        String productCode,
        String productName,
        MovementType type,
        int quantity,
        int previousStock,
        int resultingStock,
        LocalDateTime date,
        String username,
        String reason
) {}