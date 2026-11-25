package rs.floorgeneral.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.service.StandingsService;
import rs.floorgeneral.web.dto.standing.StandingResult;

/** UC-ADM-12 — pregled tabele grupe, plus rucno pokretanje preracuna (oporavak i provera). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/standings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Tabela grupe")
@SecurityRequirement(name = "bearerAuth")
public class StandingController {

    private final StandingsService standingsService;

    /** Cita materijalizovanu tabelu — ne racuna se u letu (FR-6.5.1). */
    @GetMapping
    public List<StandingResult> findByGroup(@PathVariable Long groupId) {
        return standingsService.findByGroup(groupId);
    }

    @PostMapping("/recompute")
    public List<StandingResult> recompute(@PathVariable Long groupId) {
        return standingsService.recompute(groupId);
    }
}
