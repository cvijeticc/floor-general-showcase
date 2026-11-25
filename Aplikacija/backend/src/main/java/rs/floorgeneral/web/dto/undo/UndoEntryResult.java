package rs.floorgeneral.web.dto.undo;

import java.time.LocalDateTime;
import rs.floorgeneral.domain.UndoAction;

/** Stavka steka ponistavanja — ono sto dugme „Ponisti" prikazuje (UC-ADM-23). */
public record UndoEntryResult(
        Long id,
        UndoAction action,
        String description,
        String performedBy,
        LocalDateTime performedAt
) {
}
