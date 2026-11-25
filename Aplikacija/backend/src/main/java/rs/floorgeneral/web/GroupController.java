package rs.floorgeneral.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.service.GroupService;
import rs.floorgeneral.web.dto.group.CreateGroupCmd;
import rs.floorgeneral.web.dto.group.GroupInfo;
import rs.floorgeneral.web.dto.group.GroupResult;
import rs.floorgeneral.web.dto.group.UpdateGroupCmd;

/**
 * UC-ADM-06. Grupa se pravi u okviru FAZE (od V3), a lista se i po fazi i po celoj sezoni.
 * Igraci grupe se citaju sa GET /api/v1/players?groupId={id}.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Grupe")
@SecurityRequirement(name = "bearerAuth")
public class GroupController {

    private final GroupService groupService;

    @GetMapping("/stages/{stageId}/groups")
    public List<GroupResult> searchByStage(@PathVariable Long stageId) {
        return groupService.searchByStage(stageId);
    }

    @GetMapping("/seasons/{seasonId}/groups")
    public List<GroupResult> searchBySeason(@PathVariable Long seasonId) {
        return groupService.searchBySeason(seasonId);
    }

    @PostMapping("/stages/{stageId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupInfo create(@PathVariable Long stageId, @Valid @RequestBody CreateGroupCmd cmd) {
        return groupService.create(stageId, cmd);
    }

    @GetMapping("/groups/{id}")
    public GroupInfo findById(@PathVariable Long id) {
        return groupService.findById(id);
    }

    @PutMapping("/groups/{id}")
    public GroupInfo update(@PathVariable Long id, @Valid @RequestBody UpdateGroupCmd cmd) {
        return groupService.update(id, cmd);
    }

    @DeleteMapping("/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        groupService.delete(id);
    }
}
