package rs.floorgeneral.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.service.MatchResultService;
import rs.floorgeneral.service.MatchService;
import rs.floorgeneral.web.dto.match.BulkResultsCmd;
import rs.floorgeneral.web.dto.match.BulkResultsInfo;
import rs.floorgeneral.web.dto.match.CorrectResultCmd;
import rs.floorgeneral.web.dto.match.CorrectionInfo;
import rs.floorgeneral.web.dto.match.CreateMatchCmd;
import rs.floorgeneral.web.dto.match.EnterResultCmd;
import rs.floorgeneral.web.dto.match.ForfeitCmd;
import rs.floorgeneral.web.dto.match.ForfeitInfo;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.match.MatchResult;
import rs.floorgeneral.web.dto.match.PostponeMatchCmd;
import rs.floorgeneral.web.dto.match.UpdateMatchCmd;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Mecevi")
@SecurityRequirement(name = "bearerAuth")
public class MatchController {

    private final MatchService matchService;
    private final MatchResultService matchResultService;

    @GetMapping
    public List<MatchResult> search(@RequestParam(required = false) Long groupId,
                                    @RequestParam(required = false) Long playerId,
                                    @RequestParam(required = false) Long courtId,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(required = false) MatchStatus status,
                                    @RequestParam(required = false) Integer round) {
        return matchService.search(groupId, playerId, courtId, date, status, round);
    }

    @GetMapping("/{id}")
    public MatchInfo findById(@PathVariable Long id) {
        return matchService.findById(id);
    }

    /** Zakazivanje ili unos vec odigranog meca — rezultat se sme poslati odmah (odluka D-07). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchInfo create(@Valid @RequestBody CreateMatchCmd cmd) {
        return matchService.create(cmd);
    }

    /** Termin, teren, kolo i napomena — rezultat se ovde ne dira. */
    @PutMapping("/{id}")
    public MatchInfo update(@PathVariable Long id, @Valid @RequestBody UpdateMatchCmd cmd) {
        return matchService.update(id, cmd);
    }

    /** UC-ADM-09 — pojedinacan unos rezultata. Mec koji vec ima rezultat vraca 409. */
    @PostMapping("/{id}/result")
    public MatchInfo enterResult(@PathVariable Long id, @Valid @RequestBody EnterResultCmd cmd) {
        return matchResultService.enterResult(id, cmd);
    }

    /**
     * UC-ADM-09 — masovni unos celog kola. Jedan los red ne obara ceo unos: odgovor nosi
     * listu upisanih i listu odbijenih sa razlogom.
     */
    @PutMapping("/results")
    public BulkResultsInfo enterResults(@Valid @RequestBody BulkResultsCmd cmd) {
        return matchResultService.enterResults(cmd);
    }

    /** UC-ADM-11 — ispravka vec upisanog rezultata; razlog je obavezan. */
    @PatchMapping("/{id}/result")
    public CorrectionInfo correctResult(@PathVariable Long id, @Valid @RequestBody CorrectResultCmd cmd) {
        return matchResultService.correctResult(id, cmd);
    }

    /** UC-ADM-10 — nedolazak ili predaja; rezultat se uzima iz parametara sezone. */
    @PostMapping("/{id}/forfeit")
    public ForfeitInfo applyForfeit(@PathVariable Long id, @Valid @RequestBody ForfeitCmd cmd) {
        return matchResultService.applyForfeit(id, cmd);
    }

    @PatchMapping("/{id}/postpone")
    public MatchInfo postpone(@PathVariable Long id, @Valid @RequestBody PostponeMatchCmd cmd) {
        return matchService.postpone(id, cmd);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        matchService.delete(id);
    }
}
