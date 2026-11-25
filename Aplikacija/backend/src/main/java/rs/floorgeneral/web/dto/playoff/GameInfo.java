package rs.floorgeneral.web.dto.playoff;

import java.time.LocalDateTime;
import rs.floorgeneral.domain.MatchStatus;

/** Mec serije; {@code score1} je uvek rezultat prvog igraca serije. */
public record GameInfo(
        Long matchId,
        int gameNumber,
        int score1,
        int score2,
        Long winnerId,
        MatchStatus status,
        LocalDateTime scheduledStart,
        String courtName
) {
}
