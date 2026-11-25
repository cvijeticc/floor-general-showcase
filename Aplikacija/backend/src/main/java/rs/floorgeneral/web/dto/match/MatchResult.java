package rs.floorgeneral.web.dto.match;

import java.time.LocalDateTime;
import rs.floorgeneral.domain.MatchStatus;

/** Stavka liste meceva; nosi i ID-jeve igraca, jer se na njoj gradi masovni unos rezultata. */
public record MatchResult(
        Long id,
        Long groupId,
        String groupName,
        int roundNumber,
        Long player1Id,
        String player1Name,
        Long player2Id,
        String player2Name,
        Integer scorePlayer1,
        Integer scorePlayer2,
        MatchStatus status,
        String courtName,
        LocalDateTime scheduledStart
) {
}
