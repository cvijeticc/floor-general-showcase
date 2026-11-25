package rs.floorgeneral.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.service.ApplicationService;
import rs.floorgeneral.service.PublicPlayerService;
import rs.floorgeneral.service.PublicPlayoffService;
import rs.floorgeneral.service.PublicStatsService;
import rs.floorgeneral.service.StatsPhase;
import rs.floorgeneral.service.PublicSiteService;
import rs.floorgeneral.web.dto.application.CreateApplicationCmd;
import rs.floorgeneral.web.dto.site.PublicBracket;
import rs.floorgeneral.web.dto.site.PublicMatch;
import rs.floorgeneral.web.dto.site.PublicPlayerProfile;
import rs.floorgeneral.web.dto.site.PublicPlayerSummary;
import rs.floorgeneral.web.dto.site.PublicSeason;
import rs.floorgeneral.web.dto.site.PublicStandingRow;
import rs.floorgeneral.web.dto.site.PublicStatRow;

/**
 * Javni sajt — bez prijave (SecurityConfig: /api/v1/public/** je permitAll).
 * Sve je samo citanje, osim prijave u ligu sa forme na /kontakt (D-20).
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Javni sajt")
public class PublicSiteController {

    private final PublicSiteService publicSiteService;
    private final PublicPlayerService publicPlayerService;
    private final PublicPlayoffService publicPlayoffService;
    private final PublicStatsService publicStatsService;
    private final ApplicationService applicationService;

    @GetMapping("/season")
    public PublicSeason currentSeason() {
        return publicSiteService.currentSeason();
    }

    @GetMapping("/groups/{groupId}/standings")
    public List<PublicStandingRow> standings(@PathVariable Long groupId) {
        return publicSiteService.standings(groupId);
    }

    @GetMapping("/matches")
    public List<PublicMatch> matches(@RequestParam Long seasonId,
                                     @RequestParam(required = false) Long groupId,
                                     @RequestParam(required = false) Boolean played) {
        return publicSiteService.matches(seasonId, groupId, played);
    }

    /** UC-PUB-10 — tab „Igraci": svi igraci iz grupa sezone, sa bilansom i formom. Bez seasonId: aktuelna. */
    @GetMapping("/players")
    public List<PublicPlayerSummary> players(@RequestParam(required = false) Long seasonId) {
        return publicPlayerService.players(seasonId);
    }

    /** UC-PUB-06 — profil igraca. 404 ako igrac ne igra u toj sezoni. Bez seasonId: aktuelna. */
    @GetMapping("/players/{playerId}")
    public PublicPlayerProfile player(@PathVariable Long playerId,
                                      @RequestParam(required = false) Long seasonId) {
        return publicPlayerService.profile(playerId, seasonId);
    }

    /** UC-PLA-01 — drvo playoff-a sa serijama i mecevima. 404 dok playoff nije objavljen. */
    @GetMapping("/playoff")
    public PublicBracket playoff(@RequestParam(required = false) Long seasonId) {
        return publicPlayoffService.bracket(seasonId);
    }

    /**
     * Statistika igraca (kao tab „Statistika" na turniriukraju.com): odigrano, dati i primljeni
     * poeni, proseci, procenat pobeda. {@code phase}: ALL (podrazumevano), GROUP ili KNOCKOUT.
     */
    @GetMapping("/stats")
    public List<PublicStatRow> stats(@RequestParam(required = false) Long seasonId,
                                     @RequestParam(required = false, defaultValue = "ALL") StatsPhase phase) {
        return publicStatsService.stats(seasonId, phase);
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public void submitApplication(@Valid @RequestBody CreateApplicationCmd cmd) {
        applicationService.submit(cmd);
    }
}
