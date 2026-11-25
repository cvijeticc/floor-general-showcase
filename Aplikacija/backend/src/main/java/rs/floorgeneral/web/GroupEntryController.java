package rs.floorgeneral.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.service.GroupEntryService;
import rs.floorgeneral.web.dto.group.GroupInfo;
import rs.floorgeneral.web.dto.groupentry.AssignPlayersCmd;
import rs.floorgeneral.web.dto.player.PlayerResult;

/** UC-ADM-07 — rasporedjivanje igraca u grupe. Obe izmene vracaju novo stanje grupe. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Rasporedjivanje igraca")
@SecurityRequirement(name = "bearerAuth")
public class GroupEntryController {

    private final GroupEntryService groupEntryService;

    @GetMapping("/stages/{stageId}/unassigned-players")
    public List<PlayerResult> unassignedPlayers(@PathVariable Long stageId) {
        return groupEntryService.findUnassignedForStage(stageId);
    }

    @PostMapping("/groups/{groupId}/players")
    public GroupInfo assignPlayers(@PathVariable Long groupId, @Valid @RequestBody AssignPlayersCmd cmd) {
        return groupEntryService.assignPlayers(groupId, cmd);
    }

    @DeleteMapping("/groups/{groupId}/players/{playerId}")
    public GroupInfo removePlayer(@PathVariable Long groupId, @PathVariable Long playerId) {
        return groupEntryService.removePlayer(groupId, playerId);
    }
}
