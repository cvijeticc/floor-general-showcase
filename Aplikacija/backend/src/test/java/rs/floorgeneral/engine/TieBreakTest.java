package rs.floorgeneral.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * BR-03 — razdvajanje izjednacenih (UC-ENG-02). Primeri iz Priloga B specifikacije su obavezni,
 * narocito ponovni preracun mini-tabele posle svakog izdvajanja.
 */
class TieBreakTest {

    private static final ScoringRules RULES = new ScoringRules(2, 1, 0);

    private static final long MARKO = 1;
    private static final long NIKOLA = 2;
    private static final long STEFAN = 3;

    /** D-23 — obostrana predaja u medjusobnom duelu nikome ne donosi pobedu; odlucuje drugi duel. */
    @Test
    void twoWayTie_doubleForfeitGivesNoHeadToHeadWin() {
        List<PlayedMatch> h2h = List.of(
                PlayedMatch.doubleForfeit(MARKO, NIKOLA, 0),
                new PlayedMatch(NIKOLA, MARKO, 11, 9, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(MARKO, 20, 150, 120),
                new TieBreak.Totals(NIKOLA, 20, 140, 130));

        List<TieBreak.Placed> order = TieBreak.order(totals, h2h, RULES);

        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(NIKOLA, MARKO);
        assertThat(order).extracting(TieBreak.Placed::criterion).containsOnly(TieBreak.Criterion.HEAD_TO_HEAD_WINS);
    }

    /** Prilog B: tri igraca sa po 24 boda; mini-tabela (7, 6, 5 bodova) daje Marko, Nikola, Stefan. */
    @Test
    void appendixB_threeWayTie_resolvedByMiniTablePoints() {
        List<PlayedMatch> h2h = List.of(
                new PlayedMatch(MARKO, NIKOLA, 11, 8, false),
                new PlayedMatch(NIKOLA, MARKO, 11, 9, false),
                new PlayedMatch(MARKO, STEFAN, 11, 7, false),
                new PlayedMatch(STEFAN, MARKO, 6, 11, false),
                new PlayedMatch(NIKOLA, STEFAN, 11, 10, false),
                new PlayedMatch(STEFAN, NIKOLA, 11, 9, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(MARKO, 24, 150, 120),
                new TieBreak.Totals(NIKOLA, 24, 160, 110),
                new TieBreak.Totals(STEFAN, 24, 170, 100));

        List<TieBreak.Placed> order = TieBreak.order(totals, h2h, RULES);

        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(MARKO, NIKOLA, STEFAN);
        assertThat(order).extracting(TieBreak.Placed::criterion).containsOnly(TieBreak.Criterion.MINI_TABLE_POINTS);
    }

    /**
     * OBAVEZAN TEST (BR-03.3, Prilog B): cim se jedan igrac izdvoji, mini-tabela se za preostale
     * racuna IZ POCETKA, samo od njihovih medjusobnih meceva.
     * <p>
     * X dva puta pobedjuje i Y i Z (8 bodova u mini-tabeli) i izdvaja se. Y i Z imaju po 5 bodova.
     * U mini-tabeli od troje Z bi bio ispred (KR −11 prema −13, jer je protiv X-a gubio tesno), ali
     * se posle izdvajanja X-a gledaju samo mecevi Y–Z: 11:1 i 10:11 → Y ima +9 i ide ispred Z-a.
     */
    @Test
    void afterSeparation_remainingPlayersAreRecomputedFromScratch() {
        long x = 1;
        long y = 2;
        long z = 3;
        List<PlayedMatch> h2h = List.of(
                new PlayedMatch(x, y, 11, 0, false),
                new PlayedMatch(y, x, 0, 11, false),
                new PlayedMatch(x, z, 11, 10, false),
                new PlayedMatch(z, x, 10, 11, false),
                new PlayedMatch(y, z, 11, 1, false),
                new PlayedMatch(z, y, 11, 10, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(x, 30, 100, 80),
                new TieBreak.Totals(y, 30, 100, 80),
                new TieBreak.Totals(z, 30, 100, 80));

        List<TieBreak.Placed> order = TieBreak.order(totals, h2h, RULES);

        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(x, y, z);
        assertThat(order).extracting(TieBreak.Placed::criterion).containsExactly(
                TieBreak.Criterion.MINI_TABLE_POINTS,
                TieBreak.Criterion.HEAD_TO_HEAD_DIFFERENCE,
                TieBreak.Criterion.HEAD_TO_HEAD_DIFFERENCE);
    }

    /**
     * Prilog B, nastavak kljucnog testa: posle izdvajanja ostaju dvojica sa 1:1 i kos-razlikom 0
     * u medjusobnim mecevima (11:8, 8:11) — tada odlucuje kos-razlika CELE grupe, ne mini-tabela od troje.
     */
    @Test
    void afterSeparation_twoWithEqualHeadToHead_fallBackToWholeGroupDifference() {
        long x = 1;
        long y = 2;
        long z = 3;
        List<PlayedMatch> h2h = List.of(
                new PlayedMatch(x, y, 11, 0, false),
                new PlayedMatch(y, x, 0, 11, false),
                new PlayedMatch(x, z, 11, 10, false),
                new PlayedMatch(z, x, 10, 11, false),
                new PlayedMatch(y, z, 11, 8, false),
                new PlayedMatch(z, y, 11, 8, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(x, 30, 150, 100),
                new TieBreak.Totals(y, 30, 140, 130),
                new TieBreak.Totals(z, 30, 160, 120));

        List<TieBreak.Placed> order = TieBreak.order(totals, h2h, RULES);

        // U mini-tabeli od troje Y bi bio iza Z-a i po KR (−22 prema −2) — ovde odlucuje KR grupe: Z +40, Y +10.
        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(x, z, y);
        assertThat(order.get(1).criterion()).isEqualTo(TieBreak.Criterion.GROUP_DIFFERENCE);
    }

    /** Dvojica sa 1:1 i KR 0 medjusobno → kos-razlika cele grupe. */
    @Test
    void twoTiedWithEqualHeadToHead_fallBackToGroupDifference() {
        List<PlayedMatch> h2h = List.of(
                new PlayedMatch(MARKO, NIKOLA, 11, 8, false),
                new PlayedMatch(NIKOLA, MARKO, 11, 8, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(MARKO, 24, 150, 130),
                new TieBreak.Totals(NIKOLA, 24, 160, 120));

        List<TieBreak.Placed> order = TieBreak.order(totals, h2h, RULES);

        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(NIKOLA, MARKO);
        assertThat(order).extracting(TieBreak.Placed::criterion).containsOnly(TieBreak.Criterion.GROUP_DIFFERENCE);
    }

    /** Dvojica: medjusobne pobede odlucuju i kad je ukupna kos-razlika drugacija. */
    @Test
    void twoTied_headToHeadWinsBeatOverallDifference() {
        List<PlayedMatch> h2h = List.of(
                new PlayedMatch(MARKO, NIKOLA, 11, 9, false),
                new PlayedMatch(NIKOLA, MARKO, 7, 11, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(MARKO, 30, 100, 90),
                new TieBreak.Totals(NIKOLA, 30, 180, 60));

        List<TieBreak.Placed> order = TieBreak.order(totals, h2h, RULES);

        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(MARKO, NIKOLA);
        assertThat(order.get(0).criterion()).isEqualTo(TieBreak.Criterion.HEAD_TO_HEAD_WINS);
    }

    /** Dvojica 1:1 u pobedama — odlucuje kos-razlika iz ta dva meca. */
    @Test
    void twoTied_oneWinEach_headToHeadDifferenceDecides() {
        List<PlayedMatch> h2h = List.of(
                new PlayedMatch(MARKO, NIKOLA, 11, 2, false),
                new PlayedMatch(NIKOLA, MARKO, 11, 10, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(MARKO, 30, 100, 90),
                new TieBreak.Totals(NIKOLA, 30, 180, 60));

        List<TieBreak.Placed> order = TieBreak.order(totals, h2h, RULES);

        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(MARKO, NIKOLA);
        assertThat(order.get(1).criterion()).isEqualTo(TieBreak.Criterion.HEAD_TO_HEAD_DIFFERENCE);
    }

    /** Bez izjednacenja kriterijum ostaje prazan. */
    @Test
    void differentPoints_noCriterion() {
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(MARKO, 10, 50, 40),
                new TieBreak.Totals(NIKOLA, 12, 40, 50));

        List<TieBreak.Placed> order = TieBreak.order(totals, List.of(), RULES);

        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(NIKOLA, MARKO);
        assertThat(order).extracting(TieBreak.Placed::criterion).containsOnlyNulls();
    }

    /** Potpuno isti brojevi: redosled po ID-ju (zreb jos ne postoji), oznacen kao nereseno. */
    @Test
    void identicalPlayers_orderedByIdAndMarkedUnresolved() {
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(7, 0, 0, 0),
                new TieBreak.Totals(3, 0, 0, 0),
                new TieBreak.Totals(5, 0, 0, 0));

        List<TieBreak.Placed> order = TieBreak.order(totals, List.of(), RULES);

        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(3L, 5L, 7L);
        assertThat(order).extracting(TieBreak.Placed::criterion).containsOnly(TieBreak.Criterion.UNRESOLVED);
    }

    /** Kos-razlika cele grupe izjednacena — odlucuju dati poeni cele grupe. */
    @Test
    void wholeGroupDifferenceEqual_pointsForDecides() {
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(MARKO, 6, 90, 80),
                new TieBreak.Totals(NIKOLA, 6, 100, 90));

        List<TieBreak.Placed> order = TieBreak.order(totals, List.of(), RULES);

        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(NIKOLA, MARKO);
        assertThat(order).extracting(TieBreak.Placed::criterion).containsOnly(TieBreak.Criterion.GROUP_POINTS_FOR);
    }

    /**
     * Cetvorica izjednacena: jedan se izdvaja bodovima mini-tabele, ostala trojica se racunaju iz
     * pocetka SAMO medjusobno — mecevi protiv izdvojenog vise ne vaze.
     */
    @Test
    void fourWayTie_remainingThreeRecomputedWithoutSeparatedPlayer() {
        long a = 1;
        long b = 2;
        long c = 3;
        long d = 4;
        List<PlayedMatch> matches = new ArrayList<>();
        // A pobedjuje sve (izdvaja se prvi).
        matches.add(new PlayedMatch(a, b, 11, 0, false));
        matches.add(new PlayedMatch(a, c, 11, 9, false));
        matches.add(new PlayedMatch(a, d, 11, 10, false));
        // B, C, D u krug — svako po jednu pobedu; protiv A su KR razliciti, ali to vise ne sme da vazi.
        matches.add(new PlayedMatch(b, c, 11, 5, false));
        matches.add(new PlayedMatch(c, d, 11, 6, false));
        matches.add(new PlayedMatch(d, b, 11, 8, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(a, 20, 0, 0),
                new TieBreak.Totals(b, 20, 0, 0),
                new TieBreak.Totals(c, 20, 0, 0),
                new TieBreak.Totals(d, 20, 0, 0));

        List<TieBreak.Placed> order = TieBreak.order(totals, matches, RULES);

        // Medjusobno B/C/D: B +6−3 = +3, C −6+5 = −1, D −5+3 = −2. Sa mecevima protiv A bi B bio poslednji (−8).
        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(a, b, c, d);
        assertThat(order.get(0).criterion()).isEqualTo(TieBreak.Criterion.MINI_TABLE_POINTS);
        assertThat(order.get(1).criterion()).isEqualTo(TieBreak.Criterion.MINI_TABLE_DIFFERENCE);
    }

    /** Trojica u krug sa istim bodovima i KR 0 u mini-tabeli — odlucuju dati poeni mini-tabele (3c). */
    @Test
    void threeInCircle_equalMiniDifference_miniPointsForDecides() {
        long a = 1;
        long b = 2;
        long c = 3;
        List<PlayedMatch> matches = List.of(
                new PlayedMatch(a, b, 11, 9, false),
                new PlayedMatch(b, c, 10, 8, false),
                new PlayedMatch(c, a, 12, 10, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(a, 10, 0, 0),
                new TieBreak.Totals(b, 10, 0, 0),
                new TieBreak.Totals(c, 10, 0, 0));

        List<TieBreak.Placed> order = TieBreak.order(totals, matches, RULES);

        // Dati poeni u mini-tabeli: A 21, C 20, B 19.
        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(a, c, b);
        assertThat(order).extracting(TieBreak.Placed::criterion).containsOnly(TieBreak.Criterion.MINI_TABLE_POINTS_FOR);
    }

    /** Sluzbeni rezultat u medjusobnom mecu nosi bodove sezone za predaju (BR-01). */
    @Test
    void forfeitInMiniTable_usesForfeitPoints() {
        long a = 1;
        long b = 2;
        long c = 3;
        List<PlayedMatch> matches = List.of(
                new PlayedMatch(a, b, 11, 0, true),
                new PlayedMatch(b, c, 11, 5, false),
                new PlayedMatch(c, a, 11, 9, false));
        List<TieBreak.Totals> totals = List.of(
                new TieBreak.Totals(a, 10, 0, 0),
                new TieBreak.Totals(b, 10, 0, 0),
                new TieBreak.Totals(c, 10, 0, 0));

        // Sa 0 za predaju: A 3 b, B 2 b, C 3 b → B poslednji; A i C: C je pobedio A.
        List<TieBreak.Placed> order = TieBreak.order(totals, matches, RULES);
        assertThat(order).extracting(TieBreak.Placed::playerId).containsExactly(c, a, b);

        // Sa 1 za predaju (kako liga racuna, D-22) svi imaju 3 boda u mini-tabeli → kos-razlika:
        // A +11−2 = +9, C −6+2 = −4, B −11+6 = −5.
        List<TieBreak.Placed> lenient = TieBreak.order(totals, matches, new ScoringRules(2, 1, 1));
        assertThat(lenient).extracting(TieBreak.Placed::playerId).containsExactly(a, c, b);
        assertThat(lenient).extracting(TieBreak.Placed::criterion).containsOnly(TieBreak.Criterion.MINI_TABLE_DIFFERENCE);
    }
}
