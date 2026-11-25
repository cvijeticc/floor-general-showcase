package rs.floorgeneral.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import rs.floorgeneral.service.PlayoffService;
import rs.floorgeneral.web.dto.playoff.AddSeriesGameCmd;
import rs.floorgeneral.web.dto.playoff.BracketInfo;
import rs.floorgeneral.web.dto.playoff.CorrectSeriesGameCmd;
import rs.floorgeneral.web.dto.playoff.CreateBracketCmd;
import rs.floorgeneral.web.dto.playoff.ScheduleSeriesCmd;
import rs.floorgeneral.web.dto.playoff.SeriesInfo;
import rs.floorgeneral.web.dto.playoff.SeriesWalkoverCmd;
import rs.floorgeneral.web.dto.playoff.SetSeriesPlayersCmd;
import rs.floorgeneral.web.dto.playoff.UpdateStageRoundCmd;

/**
 * Rok 3 — playoff (BR-08, BR-09): drvo faze, kola i unos meceva serije. Svaka izmena je jedan
 * zapis „Ponisti" (D-17).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Playoff")
@SecurityRequirement(name = "bearerAuth")
public class PlayoffController {

    private final PlayoffService playoffService;

    @GetMapping("/stages/{stageId}/bracket")
    public BracketInfo bracket(@PathVariable Long stageId) {
        return playoffService.bracket(stageId);
    }

    @PostMapping("/stages/{stageId}/bracket")
    @ResponseStatus(HttpStatus.CREATED)
    public BracketInfo createBracket(@PathVariable Long stageId, @Valid @RequestBody(required = false) CreateBracketCmd cmd) {
        return playoffService.createBracket(stageId, cmd);
    }

    @PutMapping("/stage-rounds/{roundId}")
    public BracketInfo updateRound(@PathVariable Long roundId, @Valid @RequestBody UpdateStageRoundCmd cmd) {
        return playoffService.updateRound(roundId, cmd);
    }

    @PostMapping("/series/{seriesId}/games")
    @ResponseStatus(HttpStatus.CREATED)
    public SeriesInfo addGame(@PathVariable Long seriesId, @Valid @RequestBody AddSeriesGameCmd cmd) {
        return playoffService.addGame(seriesId, cmd);
    }

    @PutMapping("/series/{seriesId}/games/{matchId}")
    public SeriesInfo correctGame(@PathVariable Long seriesId, @PathVariable Long matchId,
                                  @Valid @RequestBody CorrectSeriesGameCmd cmd) {
        return playoffService.correctGame(seriesId, matchId, cmd);
    }

    @DeleteMapping("/series/{seriesId}/games/{matchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGame(@PathVariable Long seriesId, @PathVariable Long matchId) {
        playoffService.deleteGame(seriesId, matchId);
    }

    @PostMapping("/series/{seriesId}/walkover")
    public SeriesInfo walkover(@PathVariable Long seriesId, @Valid @RequestBody SeriesWalkoverCmd cmd) {
        return playoffService.walkover(seriesId, cmd);
    }

    @DeleteMapping("/series/{seriesId}/walkover")
    public SeriesInfo reopen(@PathVariable Long seriesId) {
        return playoffService.reopen(seriesId);
    }

    @PutMapping("/series/{seriesId}/players")
    public SeriesInfo setPlayers(@PathVariable Long seriesId, @Valid @RequestBody SetSeriesPlayersCmd cmd) {
        return playoffService.setPlayers(seriesId, cmd);
    }

    @PutMapping("/series/{seriesId}/schedule")
    public SeriesInfo schedule(@PathVariable Long seriesId, @Valid @RequestBody ScheduleSeriesCmd cmd) {
        return playoffService.schedule(seriesId, cmd);
    }
}
