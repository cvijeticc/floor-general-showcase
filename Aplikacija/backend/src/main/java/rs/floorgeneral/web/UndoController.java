package rs.floorgeneral.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.service.UndoService;
import rs.floorgeneral.web.dto.undo.UndoEntryResult;
import rs.floorgeneral.web.dto.undo.UndoInfo;

/** UC-ADM-23 — dugme „Ponisti" i precica Ctrl+Z u admin panelu zovu ove tri operacije. */
@RestController
@RequestMapping("/api/v1/undo")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Ponistavanje")
@SecurityRequirement(name = "bearerAuth")
public class UndoController {

    private static final int MAX_HISTORY = 50;

    private final UndoService undoService;

    /** Sta ce ponistavanje uraditi sledece; 204 kad nema sta da se ponisti. */
    @GetMapping("/last")
    public ResponseEntity<UndoEntryResult> last() {
        return undoService.lastUndoable()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Istorija izmena koje se jos mogu ponistiti, od najnovije. */
    @GetMapping
    public List<UndoEntryResult> recent(@RequestParam(defaultValue = "10") int limit) {
        return undoService.recent(Math.clamp(limit, 1, MAX_HISTORY));
    }

    /** Ponistava poslednju izmenu; ponovljen poziv ide korak dublje kroz stek. */
    @PostMapping
    public UndoInfo undoLast() {
        return undoService.undoLast();
    }
}
