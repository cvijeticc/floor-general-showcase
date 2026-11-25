package rs.floorgeneral.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UC-ENG-01 — preracun tabele grupe. Ciste funkcije: ulaz su igraci, odigrani mecevi i parametri
 * bodovanja, izlaz je gotova tabela sa pozicijama. Bez Springa, JPA i baze.
 * <p>
 * <b>Idempotentno (BR-12.3):</b> isti ulaz uvek daje isti izlaz, koliko god puta se pozove.
 * Zato je i poslednji kriterijum sortiranja deterministican (ID igraca), pa dva igraca sa
 * istim brojkama nikad ne menjaju mesta izmedju dva preracuna.
 * <p>
 * <b>Redosled (BR-02, BR-03):</b> bodovi, a izjednaceni po pravilima {@link TieBreak} — medjusobni
 * skor, mini-tabela sa ponovnim racunanjem, pa kos-razlika i dati poeni u celoj grupi (UC-ENG-02).
 * Svaki red nosi kriterijum kojim je odvojen, da ga tabela moze objasniti (FR-5.1).
 */
public final class StandingsCalculator {

    private StandingsCalculator() {
    }

    public static List<StandingRow> compute(Collection<Long> playerIds,
                                            Collection<PlayedMatch> matches,
                                            ScoringRules rules) {
        Map<Long, Accumulator> byPlayer = new LinkedHashMap<>();
        for (Long playerId : playerIds) {
            byPlayer.put(playerId, new Accumulator(playerId));
        }

        for (PlayedMatch match : matches) {
            Accumulator first = byPlayer.get(match.player1Id());
            Accumulator second = byPlayer.get(match.player2Id());
            if (first == null || second == null) {
                // Mec igraca koji vise nije u grupi se ne racuna — tabela prati sastav grupe.
                continue;
            }

            first.addScores(match.scorePlayer1(), match.scorePlayer2());
            second.addScores(match.scorePlayer2(), match.scorePlayer1());

            if (match.bothForfeited()) {
                // D-23 — obojica kaznjena: dva poraza, nijedna pobeda.
                first.addLoss(rules.pointsForForfeit());
                second.addLoss(rules.pointsForForfeit());
                continue;
            }

            Accumulator winner = byPlayer.get(match.winnerId());
            Accumulator loser = byPlayer.get(match.loserId());
            winner.addWin(rules.pointsForWin());
            loser.addLoss(match.loserForfeited() ? rules.pointsForForfeit() : rules.pointsForLoss());
        }

        List<TieBreak.Totals> totals = byPlayer.values().stream()
                .map(row -> new TieBreak.Totals(row.playerId, row.points, row.pointsFor, row.pointsAgainst))
                .toList();
        List<PlayedMatch> counted = matches.stream()
                .filter(match -> byPlayer.containsKey(match.player1Id()) && byPlayer.containsKey(match.player2Id()))
                .toList();

        List<StandingRow> table = new ArrayList<>(byPlayer.size());
        int position = 1;
        for (TieBreak.Placed placed : TieBreak.order(totals, counted, rules)) {
            table.add(byPlayer.get(placed.playerId()).toRow(position++, placed.criterion()));
        }
        return table;
    }

    private static final class Accumulator {

        private final long playerId;
        private int played;
        private int wins;
        private int losses;
        private int pointsFor;
        private int pointsAgainst;
        private int points;

        private Accumulator(long playerId) {
            this.playerId = playerId;
        }

        private void addScores(int scored, int conceded) {
            played++;
            pointsFor += scored;
            pointsAgainst += conceded;
        }

        private void addWin(int pointsForWin) {
            wins++;
            points += pointsForWin;
        }

        private void addLoss(int pointsForLoss) {
            losses++;
            points += pointsForLoss;
        }

        private StandingRow toRow(int position, TieBreak.Criterion tieBreak) {
            return new StandingRow(playerId, position, played, wins, losses,
                    pointsFor, pointsAgainst, pointsFor - pointsAgainst, points, tieBreak);
        }
    }
}
