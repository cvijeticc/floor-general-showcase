package rs.floorgeneral.engine;

import java.util.List;
import java.util.Optional;

/**
 * BR-08 — serija u playoff-u: igra se dok jedan igrac ne dostigne potreban broj pobeda (2 do
 * cetvrtfinala, 1 u Final Four-u — parametar kola, {@code stage_rounds.wins_required}).
 * Ciste funkcije: ulaz su rezultati meceva iz ugla serije (prvi igrac serije je uvek prvi broj).
 */
public final class SeriesRules {

    private SeriesRules() {
    }

    /** Jedan mec serije, iz ugla serije: {@code scoreFirst} je rezultat prvog igraca serije. */
    public record Game(int scoreFirst, int scoreSecond) {

        boolean firstWon() {
            return scoreFirst > scoreSecond;
        }
    }

    /** Pobeda prvog / drugog igraca serije i ko ju je osvojio (prazno dok traje). */
    public record State(int winsFirst, int winsSecond, Optional<Side> winner) {

        public boolean decided() {
            return winner.isPresent();
        }
    }

    public enum Side {
        FIRST,
        SECOND
    }

    /** Zasto se mec ne moze dodati u seriju. */
    public enum Violation {
        /** Serija je vec resena — treci mec se ne igra kad je posle dva 2:0 (BR-08). */
        ALREADY_DECIDED
    }

    public static State evaluate(int winsRequired, List<Game> games) {
        if (winsRequired < 1) {
            throw new IllegalArgumentException("Serija trazi bar jednu pobedu");
        }
        int first = 0;
        int second = 0;
        for (Game game : games) {
            if (game.firstWon()) {
                first++;
            } else {
                second++;
            }
        }
        Optional<Side> winner = first >= winsRequired ? Optional.of(Side.FIRST)
                : second >= winsRequired ? Optional.of(Side.SECOND)
                : Optional.empty();
        return new State(first, second, winner);
    }

    /** Da li sme da se doda jos jedan mec: serija ne sme biti resena. */
    public static Optional<Violation> canAddGame(int winsRequired, List<Game> games) {
        return evaluate(winsRequired, games).decided() ? Optional.of(Violation.ALREADY_DECIDED) : Optional.empty();
    }

    /**
     * Da li je serija ispravna posle izmene: nijedan mec ne sme biti odigran posle meca koji je
     * seriju vec resio (npr. 2:0 pa treci mec). Koristi se pri ispravci rezultata ranijeg meca.
     */
    public static boolean isConsistent(int winsRequired, List<Game> games) {
        for (int played = 1; played < games.size(); played++) {
            if (evaluate(winsRequired, games.subList(0, played)).decided()) {
                return false;
            }
        }
        return true;
    }

    /** Najvise meceva u seriji: na 2 pobede → 3, na 1 → 1. */
    public static int maxGames(int winsRequired) {
        return 2 * winsRequired - 1;
    }
}
