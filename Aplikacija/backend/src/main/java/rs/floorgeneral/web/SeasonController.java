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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.domain.SeasonStatus;
import rs.floorgeneral.service.SeasonService;
import rs.floorgeneral.web.dto.season.ChangeSeasonStatusCmd;
import rs.floorgeneral.web.dto.season.CreateSeasonCmd;
import rs.floorgeneral.web.dto.season.SeasonInfo;
import rs.floorgeneral.web.dto.season.SeasonResult;
import rs.floorgeneral.web.dto.season.UpdateSeasonCmd;

@RestController
@RequestMapping("/api/v1/seasons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Sezone")
@SecurityRequirement(name = "bearerAuth")
public class SeasonController {

    private final SeasonService seasonService;

    @GetMapping
    public List<SeasonResult> search(@RequestParam(required = false) Long leagueId,
                                     @RequestParam(required = false) SeasonStatus status) {
        return seasonService.search(leagueId, status);
    }

    @GetMapping("/{id}")
    public SeasonInfo findById(@PathVariable Long id) {
        return seasonService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeasonInfo create(@Valid @RequestBody CreateSeasonCmd cmd) {
        return seasonService.create(cmd);
    }

    @PutMapping("/{id}")
    public SeasonInfo update(@PathVariable Long id, @Valid @RequestBody UpdateSeasonCmd cmd) {
        return seasonService.update(id, cmd);
    }

    /** DRAFT -> ACTIVE -> FINISHED; svaki drugi prelaz vraca 409. */
    @PatchMapping("/{id}/status")
    public SeasonInfo changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeSeasonStatusCmd cmd) {
        return seasonService.changeStatus(id, cmd);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        seasonService.delete(id);
    }
}
