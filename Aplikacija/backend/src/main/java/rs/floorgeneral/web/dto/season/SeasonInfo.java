package rs.floorgeneral.web.dto.season;

import java.time.LocalDate;
import rs.floorgeneral.domain.SeasonStatus;

/** Detaljan pregled jedne sezone, zajedno sa parametrima bodovanja (BR-01, BR-10.2, BR-11). */
public record SeasonInfo(
        Long id,
        Long leagueId,
        String leagueName,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        int playerCount,
        SeasonStatus status,
        int pointsForWin,
        int pointsForLoss,
        int pointsForForfeit,
        int forfeitWinnerScore,
        int forfeitLoserScore,
        int targetScore,
        int maxScore,
        int durationMinutes
) {
}
