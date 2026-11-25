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
import rs.floorgeneral.service.LeagueService;
import rs.floorgeneral.web.dto.league.CreateLeagueCmd;
import rs.floorgeneral.web.dto.league.LeagueInfo;
import rs.floorgeneral.web.dto.league.LeagueResult;
import rs.floorgeneral.web.dto.league.UpdateLeagueCmd;

@RestController
@RequestMapping("/api/v1/leagues")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Lige")
@SecurityRequirement(name = "bearerAuth")
public class LeagueController {

    private final LeagueService leagueService;

    @GetMapping
    public List<LeagueResult> search() {
        return leagueService.search();
    }

    @GetMapping("/{id}")
    public LeagueInfo findById(@PathVariable Long id) {
        return leagueService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueInfo create(@Valid @RequestBody CreateLeagueCmd cmd) {
        return leagueService.create(cmd);
    }

    @PutMapping("/{id}")
    public LeagueInfo update(@PathVariable Long id, @Valid @RequestBody UpdateLeagueCmd cmd) {
        return leagueService.update(id, cmd);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        leagueService.delete(id);
    }
}
