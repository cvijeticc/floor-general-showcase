package rs.floorgeneral.web.dto.site;

import java.time.LocalDate;
import java.util.List;

/**
 * Aktuelna sezona za javni sajt (UC-PUB-02/05) — samo ono sto sme da se objavi.
 * Grupe nose parametre prolaza svoje faze, da bi tabela mogla da oboji zonu prolaza (BR-05).
 */
public record PublicSeason(
        Long id,
        String name,
        String leagueName,
        LocalDate startDate,
        LocalDate endDate,
        List<Group> groups
) {

    public record Group(
            Long id,
            String name,
            Long stageId,
            String stageName,
            Integer directAdvance,
            Integer additionalAdvance
    ) {
    }
}
