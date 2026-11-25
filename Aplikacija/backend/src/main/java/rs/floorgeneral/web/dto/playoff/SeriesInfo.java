package rs.floorgeneral.web.dto.playoff;

import java.time.LocalDateTime;
import java.util.List;
import rs.floorgeneral.web.dto.site.PublicPlayer;

/**
 * Serija u drvetu (admin).
 *
 * @param number    redni broj na kosturu lige: M01…M63, mec za 3. mesto je poslednji
 * @param status    PENDING (ceka igraca) · READY (oba igraca, bez meceva) · IN_PROGRESS · FINISHED
 * @param walkover  resena sluzbeno — protivnik se nije pojavio ili je predao
 */
public record SeriesInfo(
        Long id,
        int number,
        int roundNo,
        int position,
        boolean thirdPlace,
        PublicPlayer player1,
        PublicPlayer player2,
        String seed1,
        String seed2,
        int wins1,
        int wins2,
        Long winnerId,
        boolean walkover,
        String status,
        LocalDateTime scheduledStart,
        Long courtId,
        String courtName,
        String note,
        List<GameInfo> games
) {
}
