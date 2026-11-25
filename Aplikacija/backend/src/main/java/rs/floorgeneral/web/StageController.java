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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.service.StageService;
import rs.floorgeneral.web.dto.stage.ChangeStageStatusCmd;
import rs.floorgeneral.web.dto.stage.CreateStageCmd;
import rs.floorgeneral.web.dto.stage.StageInfo;
import rs.floorgeneral.web.dto.stage.StageResult;
import rs.floorgeneral.web.dto.stage.UpdateStageCmd;

/**
 * UC-ADM-14 i UC-ADM-15. Faza se pravi i lista u kontekstu sezone, a dalje se radi po njenom ID-ju.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Faze")
@SecurityRequirement(name = "bearerAuth")
public class StageController {

    private final StageService stageService;

    @GetMapping("/seasons/{seasonId}/stages")
    public List<StageResult> searchBySeason(@PathVariable Long seasonId) {
        return stageService.searchBySeason(seasonId);
    }

    @PostMapping("/seasons/{seasonId}/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public StageInfo create(@PathVariable Long seasonId, @Valid @RequestBody CreateStageCmd cmd) {
        return stageService.create(seasonId, cmd);
    }

    @GetMapping("/stages/{id}")
    public StageInfo findById(@PathVariable Long id) {
        return stageService.findById(id);
    }

    @PutMapping("/stages/{id}")
    public StageInfo update(@PathVariable Long id, @Valid @RequestBody UpdateStageCmd cmd) {
        return stageService.update(id, cmd);
    }

    /** DRAFT -> ACTIVE -> FINISHED; pri pokretanju se proverava format (FR-4.2). */
    @PatchMapping("/stages/{id}/status")
    public StageInfo changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStageStatusCmd cmd) {
        return stageService.changeStatus(id, cmd);
    }

    @DeleteMapping("/stages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        stageService.delete(id);
    }
}
