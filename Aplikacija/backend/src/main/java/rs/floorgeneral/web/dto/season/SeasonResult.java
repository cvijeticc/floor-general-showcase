package rs.floorgeneral.web.dto.season;

import java.time.LocalDate;
import rs.floorgeneral.domain.SeasonStatus;

/** Stavka liste/pretrage sezona. */
public record SeasonResult(
        Long id,
        Long leagueId,
        String leagueName,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        int playerCount,
        SeasonStatus status
) {
}
