package pe.tecnostock.model;

import java.time.LocalDateTime;

/**
 * Constancia de un cambio de stock, tal como quedÃ³ registrada.
 *
 * Responde por quÃ© cambiÃ³ el stock: quÃ© tipo de movimiento fue,
 * cuÃ¡ntas unidades, cÃ³mo estaba antes, cÃ³mo quedÃ³, quiÃ©n lo hizo y
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