package rs.floorgeneral.web.dto.match;

import java.time.LocalDateTime;
import rs.floorgeneral.domain.MatchStatus;

/** Detalj jednog meca. Pobednik se izvodi iz rezultata, ne unosi se (BR-11). */
public record MatchInfo(
        Long id,
        Long stageId,
        String stageName,
        Long groupId,
        String groupName,
        int roundNumber,
        Long player1Id,
        String player1Name,
        Long player2Id,
        String player2Name,
        Integer scorePlayer1,
        Integer scorePlayer2,
        Long winnerId,
        String winnerName,
        MatchStatus status,
        Long courtId,
        String courtName,
        LocalDateTime scheduledStart,
        String note
) {
}
