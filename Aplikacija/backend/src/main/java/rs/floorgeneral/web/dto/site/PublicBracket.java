package rs.floorgeneral.web.dto.site;

import java.util.List;

/**
 * Javno drvo playoff-a (UC-PLA-01): kola, serije i mecevi. Bez internih napomena.
 *
 * @param bracketSize broj igraca u drvetu
 */
public record PublicBracket(
        Long stageId,
        String stageName,
        int bracketSize,
        List<PublicRound> rounds
) {

    /**
     * @param winsRequired 2 = serija na dve pobede, 1 = jedna utakmica (Final Four)
     * @param targetScore  granica poena posle koje mec moze da se zavrsi ranije
     */
    public record PublicRound(
            int roundNo,
            String name,
            int winsRequired,
            int targetScore,
            int maxScore,
            int durationMinutes,
            List<PublicSeries> series
    ) {
    }

    /**
     * @param number   redni broj na kosturu lige (M01…M63; mec za 3. mesto je poslednji)
     * @param status   PENDING · READY · IN_PROGRESS · FINISHED
     * @param walkover resena sluzbeno (nedolazak ili predaja)
     */
    public record PublicSeries(
            Long id,
            int number,
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
            java.time.LocalDateTime scheduledStart,
            String courtName,
            List<PublicGame> games
    ) {
    }

    /** Mec serije; {@code score1} je rezultat prvog igraca serije. */
    public record PublicGame(
            Long matchId,
            int gameNumber,
            int score1,
            int score2,
            Long winnerId,
            java.time.LocalDateTime scheduledStart,
            String courtName
    ) {
    }
}
