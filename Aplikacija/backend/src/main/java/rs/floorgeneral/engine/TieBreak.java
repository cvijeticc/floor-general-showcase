package rs.floorgeneral.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * BR-03 — razdvajanje igraca sa istim brojem bodova (FIBA/UEFA „mini-tabela"). Ciste funkcije,
 * bez Springa i baze; ulaz su ukupni brojevi iz grupe i odigrani mecevi.
 * <p>
 * Redosled kriterijuma, tacno kako ga trazi specifikacija:
 * <ol>
 *   <li>bodovi (to radi {@link StandingsCalculator} pre poziva);</li>
 *   <li><b>dva</b> izjednacena: broj pobeda u medjusobnim mecevima, pa kos-razlika iz njih;</li>
 *   <li><b>tri ili vise</b>: mini-tabela samo od njihovih medjusobnih meceva — bodovi, kos-razlika,
 *       dati poeni. <b>Cim se neko izdvoji, mini-tabela se za preostale racuna iz pocetka</b>
 *       (Prilog B; obavezan test u {@code TieBreakTest});</li>
 *   <li>kos-razlika u celoj grupi;</li>
 *   <li>dati poeni u celoj grupi.</li>
 * </ol>
 * Kriterijumi 6 (tehnicke greske) i 7 (zreb) se jos ne vode — dok ne stignu, poslednji je ID igraca
 * ({@link Criterion#UNRESOLVED}), da preracun ostane idempotentan (BR-12.3), a tabela pokaze da je
 * redosled privremen (FR-5.1).
 */
public final class TieBreak {

    private TieBreak() {
    }

    /** Kriterijum kojim je igrac odvojen od ostalih sa istim brojem bodova (FR-5.1). */
    public enum Criterion {
        HEAD_TO_HEAD_WINS,
        HEAD_TO_HEAD_DIFFERENCE,
        MINI_TABLE_POINTS,
        MINI_TABLE_DIFFERENCE,
        MINI_TABLE_POINTS_FOR,
        GROUP_DIFFERENCE,
        GROUP_POINTS_FOR,
        UNRESOLVED
    }

    /** Ukupni brojevi igraca u celoj grupi — ulaz za korake 4 i 5. */
    public record Totals(long playerId, int points, int pointsFor, int pointsAgainst) {

        int difference() {
            return pointsFor - pointsAgainst;
        }
    }

    /** Igrac na svom mestu; {@code criterion} je null kad je odvojen bodovima. */
    public record Placed(long playerId, Criterion criterion) {
    }

    /**
     * Konacan redosled grupe: po bodovima, a izjednaceni po BR-03.
     *
     * @param totals  ukupni brojevi svakog igraca u grupi
     * @param matches svi odigrani mecevi grupe (i sluzbeni rezultati — i oni nose bodove)
     */
    public static List<Placed> order(Collection<Totals> totals, Collection<PlayedMatch> matches, ScoringRules rules) {
        Map<Integer, List<Totals>> byPoints = new TreeMap<>(Comparator.reverseOrder());
        for (Totals t : totals) {
            byPoints.computeIfAbsent(t.points(), points -> new ArrayList<>()).add(t);
        }

        List<Placed> order = new ArrayList<>(totals.size());
        for (List<Totals> tied : byPoints.values()) {
            if (tied.size() == 1) {
                order.add(new Placed(tied.get(0).playerId(), null));
            } else {
                order.addAll(resolve(tied, matches, rules));
            }
        }
        return order;
    }

    private static List<Placed> resolve(List<Totals> tied, Collection<PlayedMatch> matches, ScoringRules rules) {
        if (tied.size() == 1) {
            return List.of(new Placed(tied.get(0).playerId(), null));
        }

        Map<Long, MiniRow> mini = miniTable(tied, matches, rules);

        if (tied.size() == 2) {
            List<Placed> byWins = separateTwo(tied, mini, row -> row.wins, Criterion.HEAD_TO_HEAD_WINS);
            if (byWins != null) {
                return byWins;
            }
            List<Placed> byDifference = separateTwo(tied, mini, MiniRow::difference, Criterion.HEAD_TO_HEAD_DIFFERENCE);
            if (byDifference != null) {
                return byDifference;
            }
            return wholeGroup(tied);
        }

        List<Step> steps = List.of(
                new Step(row -> row.points, Criterion.MINI_TABLE_POINTS),
                new Step(MiniRow::difference, Criterion.MINI_TABLE_DIFFERENCE),
                new Step(row -> row.pointsFor, Criterion.MINI_TABLE_POINTS_FOR));

        for (Step step : steps) {
            Map<Integer, List<Totals>> partitions = new TreeMap<>(Comparator.reverseOrder());
            for (Totals t : tied) {
                partitions.computeIfAbsent(step.value().applyAsInt(mini.get(t.playerId())), v -> new ArrayList<>()).add(t);
            }
            if (partitions.size() > 1) {
                // Neko se izdvojio — preostali izjednaceni krecu od pocetka, sa mini-tabelom samo izmedju sebe.
                List<Placed> order = new ArrayList<>(tied.size());
                for (List<Totals> part : partitions.values()) {
                    if (part.size() == 1) {
                        order.add(new Placed(part.get(0).playerId(), step.criterion()));
                    } else {
                        order.addAll(resolve(part, matches, rules));
                    }
                }
                return order;
            }
        }
        return wholeGroup(tied);
    }

    private static List<Placed> separateTwo(List<Totals> tied, Map<Long, MiniRow> mini,
                                            ToIntFunction<MiniRow> value, Criterion criterion) {
        Totals a = tied.get(0);
        Totals b = tied.get(1);
        int va = value.applyAsInt(mini.get(a.playerId()));
        int vb = value.applyAsInt(mini.get(b.playerId()));
        if (va == vb) {
            return null;
        }
        Totals first = va > vb ? a : b;
        Totals second = first == a ? b : a;
        return List.of(new Placed(first.playerId(), criterion), new Placed(second.playerId(), criterion));
    }

    /** Koraci 4 i 5 — cela grupa; ko i tada ostane izjednacen, ide po ID-ju (privremeno, bez zreba). */
    private static List<Placed> wholeGroup(List<Totals> tied) {
        List<Totals> sorted = new ArrayList<>(tied);
        sorted.sort(Comparator.comparingInt(Totals::difference).reversed()
                .thenComparing(Comparator.comparingInt(Totals::pointsFor).reversed())
                .thenComparingLong(Totals::playerId));

        List<Placed> order = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            Totals t = sorted.get(i);
            order.add(new Placed(t.playerId(), wholeGroupCriterion(sorted, i)));
        }
        return order;
    }

    /** Najjaci kriterijum koji igraca odvaja od oba suseda u poretku. */
    private static Criterion wholeGroupCriterion(List<Totals> sorted, int index) {
        Totals t = sorted.get(index);
        boolean sameAsNeighbourDifference = false;
        boolean sameAsNeighbourScored = false;
        for (int neighbour : new int[] {index - 1, index + 1}) {
            if (neighbour < 0 || neighbour >= sorted.size()) {
                continue;
            }
            Totals other = sorted.get(neighbour);
            if (other.difference() == t.difference()) {
                sameAsNeighbourDifference = true;
                if (other.pointsFor() == t.pointsFor()) {
                    sameAsNeighbourScored = true;
                }
            }
        }
        if (!sameAsNeighbourDifference) {
            return Criterion.GROUP_DIFFERENCE;
        }
        return sameAsNeighbourScored ? Criterion.UNRESOLVED : Criterion.GROUP_POINTS_FOR;
    }

    /** Mini-tabela: samo mecevi u kojima su oba igraca iz izjednacene grupe (bodovanje sezone). */
    private static Map<Long, MiniRow> miniTable(List<Totals> tied, Collection<PlayedMatch> matches, ScoringRules rules) {
        Set<Long> ids = tied.stream().map(Totals::playerId).collect(Collectors.toSet());
        Map<Long, MiniRow> mini = new LinkedHashMap<>();
        for (Totals t : tied) {
            mini.put(t.playerId(), new MiniRow());
        }
        for (PlayedMatch match : matches) {
            if (!ids.contains(match.player1Id()) || !ids.contains(match.player2Id())) {
                continue;
            }
            MiniRow first = mini.get(match.player1Id());
            MiniRow second = mini.get(match.player2Id());
            first.pointsFor += match.scorePlayer1();
            first.pointsAgainst += match.scorePlayer2();
            second.pointsFor += match.scorePlayer2();
            second.pointsAgainst += match.scorePlayer1();

            if (match.bothForfeited()) {
                // D-23 — obojica kaznjena: nijedan ne dobija pobedu u medjusobnom skoru.
                first.points += rules.pointsForForfeit();
                second.points += rules.pointsForForfeit();
                continue;
            }

            MiniRow winner = mini.get(match.winnerId());
            MiniRow loser = mini.get(match.loserId());
            winner.wins++;
            winner.points += rules.pointsForWin();
            loser.points += match.loserForfeited() ? rules.pointsForForfeit() : rules.pointsForLoss();
        }
        return mini;
    }

    private record Step(ToIntFunction<MiniRow> value, Criterion criterion) {
    }

    private static final class MiniRow {
        private int wins;
        private int points;
        private int pointsFor;
        private int pointsAgainst;

        private int difference() {
            return pointsFor - pointsAgainst;
        }
    }
}
