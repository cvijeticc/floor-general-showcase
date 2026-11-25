package rs.floorgeneral.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * UC-PUB-06 — forma, nizovi i medjusobni skor igraca (FR-6.1.7, FR-6.5.2). Ciste funkcije:
 * ulaz su odigrani mecevi jednog igraca <b>hronoloski</b> (najstariji prvi), izlaz su brojke
 * za profil, spisak igraca i kolonu „Forma" u tabeli. Bez Springa, JPA i baze.
 * <p>
 * Bilans (pobede, porazi, kos-razlika, bodovi) se ovde ne racuna — on je red materijalizovane
 * tabele grupe ({@link StandingsCalculator}), da profil i tabela nikad ne pokazu razlicite brojke.
 */
public final class PlayerForm {

    private PlayerForm() {
    }

    /** Poslednjih {@code n} stavki, i dalje od starije ka novijoj. */
    public static <T> List<T> last(List<T> chronological, int n) {
        return List.copyOf(chronological.subList(Math.max(0, chronological.size() - n), chronological.size()));
    }

    /** Trenutni niz: +3 = tri pobede zaredom, −2 = dva poraza zaredom, 0 = jos nema meceva. */
    public static int currentStreak(List<PlayerGame> games) {
        if (games.isEmpty()) {
            return 0;
        }
        boolean won = games.get(games.size() - 1).won();
        int length = 0;
        for (int i = games.size() - 1; i >= 0 && games.get(i).won() == won; i--) {
            length++;
        }
        return won ? length : -length;
    }

    public static int longestWinStreak(List<PlayerGame> games) {
        int longest = 0;
        int current = 0;
        for (PlayerGame game : games) {
            current = game.won() ? current + 1 : 0;
            longest = Math.max(longest, current);
        }
        return longest;
    }

    /** Najubedljivija odigrana pobeda (sluzbeni rezultat se ne racuna); kod iste razlike ranija. */
    public static Optional<PlayerGame> biggestWin(List<PlayerGame> games) {
        PlayerGame best = null;
        for (PlayerGame game : games) {
            if (!game.forfeit() && game.won() && (best == null || game.margin() > best.margin())) {
                best = game;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Najtezi odigran poraz (sluzbeni rezultat se ne racuna); kod iste razlike raniji. */
    public static Optional<PlayerGame> heaviestLoss(List<PlayerGame> games) {
        PlayerGame worst = null;
        for (PlayerGame game : games) {
            if (!game.forfeit() && !game.won() && (worst == null || game.margin() < worst.margin())) {
                worst = game;
            }
        }
        return Optional.ofNullable(worst);
    }

    /** Medjusobni skor sa svakim protivnikom (FR-6.5.2), redom prvog susreta. */
    public static List<HeadToHead> headToHead(List<PlayerGame> games) {
        Map<Long, HeadToHead> byOpponent = new LinkedHashMap<>();
        for (PlayerGame game : games) {
            byOpponent.merge(game.opponentId(), HeadToHead.of(game), HeadToHead::plus);
        }
        return List.copyOf(byOpponent.values());
    }

    public record HeadToHead(
            long opponentId,
            int played,
            int wins,
            int losses,
            int pointsFor,
            int pointsAgainst
    ) {

        /** Sluzbeni rezultat se racuna — on je deo medjusobnog skora kao i u tabeli (BR-10.2). */
        private static HeadToHead of(PlayerGame game) {
            return new HeadToHead(game.opponentId(), 1, game.won() ? 1 : 0, game.won() ? 0 : 1,
                    game.scored(), game.conceded());
        }

        private HeadToHead plus(HeadToHead other) {
            return new HeadToHead(opponentId, played + other.played, wins + other.wins, losses + other.losses,
                    pointsFor + other.pointsFor, pointsAgainst + other.pointsAgainst);
        }
    }
}
