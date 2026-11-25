package rs.floorgeneral.web.dto.undo;

/**
 * Ishod ponistavanja: sta je ponisteno i sta je sada na vrhu steka (da dugme odmah pokaze
 * sledeci korak, bez jos jednog poziva).
 */
public record UndoInfo(
        Long undoneId,
        String description,
        UndoEntryResult next
) {
}
