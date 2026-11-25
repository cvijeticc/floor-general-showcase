package rs.floorgeneral.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** UC-ENG-01 — bodovanje i tabela grupe (BR-01, BR-02). Cist engine, bez baze. */
class StandingsCalculatorTest {

    private static final ScoringRules DEFAULT_RULES = new ScoringRules(2, 1, 0);

    @Test
    void compute_emptyGroup_givesZeroRowForEveryPlayer() {
        List<StandingRow> table = StandingsCalculator.compute(List.of(1L, 2L, 3L), List.of(), DEFAULT_RULES);

        assertThat(table).hasSize(3);
        assertThat(table).allSatisfy(row -> {
            assertThat(row.played()).isZero();
            assertThat(row.points()).isZero();
            assertThat(row.pointDifference()).isZero();
        });
        assertThat(table.get(0).position()).isEqualTo(1);
        assertThat(table.get(2).position()).isEqualTo(3);
    }

    @Test
    void compute_singleMatch_countsBothSides() {
        List<StandingRow> table = StandingsCalculator.compute(List.of(1L, 2L),
                List.of(new PlayedMatch(1L, 2L, 11, 7, false)), DEFAULT_RULES);

        StandingRow winner = row(table, 1L);
        StandingRow loser = row(table, 2L);

        assertThat(winner.position()).isEqualTo(1);
        assertThat(winner.played()).isEqualTo(1);
        assertThat(winner.wins()).isEqualTo(1);
        assertThat(winner.pointsFor()).isEqualTo(11);
        assertThat(winner.pointsAgainst()).isEqualTo(7);
        assertThat(winner.pointDifference()).isEqualTo(4);
        assertThat(winner.points()).isEqualTo(2);

        assertThat(loser.position()).isEqualTo(2);
        assertThat(loser.losses()).isEqualTo(1);
        assertThat(loser.pointDifference()).isEqualTo(-4);
        assertThat(loser.points()).isEqualTo(1);
    }

    /** BR-10.2 — kaznjeni dobija 0 bodova, protivnik 2, a kos-razlika se i tada racuna. */
    @Test
    void compute_forfeit_givesZeroPointsToPunishedPlayer() {
        List<StandingRow> table = StandingsCalculator.compute(List.of(1L, 2L),
                List.of(new PlayedMatch(1L, 2L, 11, 0, true)), DEFAULT_RULES);

        assertThat(row(table, 1L).points()).isEqualTo(2);
        assertThat(row(table, 2L).points()).isZero();
        assertThat(row(table, 2L).pointDifference()).isEqualTo(-11);
        assertThat(row(table, 2L).played()).isEqualTo(1);
    }

    /** D-23 — obojica kaznjena: dva poraza, oba dobijaju bodove za predaju, nijedna pobeda. */
    @Test
    void compute_doubleForfeit_givesLossAndForfeitPointsToBoth() {
        List<StandingRow> table = StandingsCalculator.compute(List.of(1L, 2L),
                List.of(PlayedMatch.doubleForfeit(1L, 2L, 0)), new ScoringRules(2, 1, 0));

        assertThat(table).allSatisfy(row -> {
            assertThat(row.played()).isEqualTo(1);
            assertThat(row.wins()).isZero();
            assertThat(row.losses()).isEqualTo(1);
            assertThat(row.points()).isZero();
            assertThat(row.pointDifference()).isZero();
        });
    }

    /** D-23 — sa bodovanjem 2/1/0 obostrana predaja je gora od odigranog poraza. */
    @Test
    void compute_doubleForfeit_ranksBelowPlayedLoss() {
        List<PlayedMatch> matches = List.of(
                new PlayedMatch(1L, 3L, 11, 9, false),
                PlayedMatch.doubleForfeit(2L, 3L, 0),
                new PlayedMatch(1L, 2L, 11, 5, false));

        List<StandingRow> table = StandingsCalculator.compute(List.of(1L, 2L, 3L), matches, DEFAULT_RULES);

        assertThat(row(table, 1L).points()).isEqualTo(4);
        assertThat(row(table, 2L).points()).isEqualTo(1);
        assertThat(row(table, 3L).points()).isEqualTo(1);
        assertThat(row(table, 2L).losses()).isEqualTo(2);
        assertThat(row(table, 3L).losses()).isEqualTo(2);
        assertThat(table.get(0).playerId()).isEqualTo(1L);
    }

    @Test
    void compute_ordersByPointsThenDifferenceThenScored() {
        List<PlayedMatch> matches = List.of(
                new PlayedMatch(1L, 2L, 11, 3, false),
                new PlayedMatch(2L, 3L, 11, 9, false),
                new PlayedMatch(3L, 1L, 11, 10, false));

        List<StandingRow> table = StandingsCalculator.compute(List.of(1L, 2L, 3L), matches, DEFAULT_RULES);

        // Svi imaju po jednu pobedu i poraz (3 boda) — odlucuje kos-razlika.
        assertThat(table).extracting(StandingRow::points).containsOnly(3);
        assertThat(table.get(0).playerId()).isEqualTo(1L);
        assertThat(table.get(0).pointDifference()).isEqualTo(7);
        assertThat(table.get(1).playerId()).isEqualTo(3L);
        assertThat(table.get(2).playerId()).isEqualTo(2L);
        assertThat(table.get(2).pointDifference()).isEqualTo(-6);
    }

    /** BR-12.3 — dva uzastopna preracuna daju identican izlaz. */
    @Test
    void compute_isIdempotent() {
        List<Long> players = List.of(1L, 2L, 3L, 4L);
        List<PlayedMatch> matches = List.of(
                new PlayedMatch(1L, 2L, 11, 7, false),
                new PlayedMatch(3L, 4L, 11, 0, true),
                new PlayedMatch(1L, 3L, 9, 11, false));

        List<StandingRow> first = StandingsCalculator.compute(players, matches, DEFAULT_RULES);
        List<StandingRow> second = StandingsCalculator.compute(players, matches, DEFAULT_RULES);

        assertThat(second).isEqualTo(first);
    }

    /** Igraci sa istim brojkama uvek dobiju isti redosled — inace preracun ne bi bio idempotentan. */
    @Test
    void compute_tiedPlayersKeepDeterministicOrder() {
        List<StandingRow> table = StandingsCalculator.compute(List.of(7L, 3L, 5L), List.of(), DEFAULT_RULES);

        assertThat(table).extracting(StandingRow::playerId).containsExactly(3L, 5L, 7L);
    }

    /** Parametri bodovanja su iz sezone — sa 1/0 tabela izgleda drugacije. */
    @Test
    void compute_respectsSeasonScoringParameters() {
        ScoringRules oneZero = new ScoringRules(1, 0, 0);
        List<PlayedMatch> matches = List.of(new PlayedMatch(1L, 2L, 11, 7, false));

        List<StandingRow> table = StandingsCalculator.compute(List.of(1L, 2L), matches, oneZero);

        assertThat(row(table, 1L).points()).isEqualTo(1);
        assertThat(row(table, 2L).points()).isZero();
    }

    @Test
    void compute_ignoresMatchesOfPlayersOutsideTheGroup() {
        List<PlayedMatch> matches = List.of(
                new PlayedMatch(1L, 99L, 11, 7, false),
                new PlayedMatch(1L, 2L, 11, 5, false));

        List<StandingRow> table = StandingsCalculator.compute(List.of(1L, 2L), matches, DEFAULT_RULES);

        assertThat(table).hasSize(2);
        assertThat(row(table, 1L).played()).isEqualTo(1);
        assertThat(row(table, 1L).pointsFor()).isEqualTo(11);
    }

    private static StandingRow row(List<StandingRow> table, long playerId) {
        return table.stream()
                .filter(row -> row.playerId() == playerId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Nema reda za igraca " + playerId));
    }
}
