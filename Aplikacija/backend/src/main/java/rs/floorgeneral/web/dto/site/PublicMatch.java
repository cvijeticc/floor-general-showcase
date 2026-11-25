package rs.floorgeneral.web.dto.site;

import java.time.LocalDateTime;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.StageType;

/**
 * Mec za javni raspored i rezultate (UC-PUB-04/05). Bez napomene — ona je interna.
 *
 * @param stageType  GROUP ili KNOCKOUT — playoff mec nema grupu nego kolo drveta i seriju
 * @param roundName  naziv kola playoff-a („Top 64", „Finale"); null za grupni mec
 * @param seriesId   serija playoff-a; null za grupni mec
 * @param gameNumber redni broj meca u seriji (1, 2, 3)
 */
public record PublicMatch(
        Long id,
        Long groupId,
        String groupName,
        int roundNumber,
        PublicPlayer player1,
        PublicPlayer player2,
        Integer scorePlayer1,
        Integer scorePlayer2,
        Long winnerId,
        MatchStatus status,
        String courtName,
        LocalDateTime scheduledStart,
        StageType stageType,
        String roundName,
        Long seriesId,
        Integer gameNumber
) {
}
