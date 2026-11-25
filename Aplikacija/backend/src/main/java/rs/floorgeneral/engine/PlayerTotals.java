package rs.floorgeneral.engine;

import java.util.List;

/**
 * Statistika igraca za tab „Statistika" (kao „Poeni po igracima" na turniriukraju.com): koliko je
 * odigrao, dao i primio, proseci po mecu i nekoliko izvedenih brojki. Ciste funkcije.
 * <p>
 * <b>Sluzbeni rezultat se ne racuna u odigrane meceve ni u poene</b> — 11:0 bez igre nije ni
 * napad ni odbrana (isto kao na turniriukraju.com i kao „najubedljivija pobeda", D-21). Broji se
 * posebno ({@code officialWins}, {@code officialLosses}).
 *
 * @param played         odigrani mecevi (bez sluzbenih rezultata)
 * @param closeWins      pobede razlikom do {@code closeMargin} poena (jedan sut spolja)
 * @param shutouts       odigrane pobede bez primljenog poena
 */
public record PlayerTotals(
        int played,
        int wins,
        int losses,
        int pointsFor,
        int pointsAgainst,
        int closeWins,
        int closeLosses,
        int shutouts,
        int officialWins,
        int officialLosses
) {

    public static PlayerTotals of(List<PlayerGame> games, int closeMargin) {
        int played = 0;
        int wins = 0;
        int losses = 0;
        int pointsFor = 0;
        int pointsAgainst = 0;
        int closeWins = 0;
        int closeLosses = 0;
        int shutouts = 0;
        int officialWins = 0;
        int officialLosses = 0;
        for (PlayerGame game : games) {
            if (game.forfeit()) {
                if (game.won()) {
                    officialWins++;
                } else {
                    officialLosses++;
                }
                continue;
            }
            played++;
            pointsFor += game.scored();
            pointsAgainst += game.conceded();
            boolean close = Math.abs(game.margin()) <= closeMargin;
            if (game.won()) {
                wins++;
                if (close) {
                    closeWins++;
                }
                if (game.conceded() == 0) {
                    shutouts++;
                }
            } else {
                losses++;
                if (close) {
                    closeLosses++;
                }
            }
        }
        return new PlayerTotals(played, wins, losses, pointsFor, pointsAgainst, closeWins, closeLosses, shutouts,
                officialWins, officialLosses);
    }

    public int pointDifference() {
        return pointsFor - pointsAgainst;
    }

    /** Prosek datih poena po odigranom mecu, na jednu decimalu; 0 bez meceva. */
    public double averageFor() {
        return average(pointsFor);
    }

    public double averageAgainst() {
        return average(pointsAgainst);
    }

    /** Procenat pobeda u odigranim mecevima, ceo broj 0–100. */
    public int winPercentage() {
        return played == 0 ? 0 : Math.round(100f * wins / played);
    }

    private double average(int total) {
        return played == 0 ? 0 : Math.round(10.0 * total / played) / 10.0;
    }
}
