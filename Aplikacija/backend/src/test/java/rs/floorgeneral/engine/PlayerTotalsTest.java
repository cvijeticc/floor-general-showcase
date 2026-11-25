package rs.floorgeneral.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Statistika igraca (tab „Statistika") — sluzbeni rezultat nije ni napad ni odbrana. */
class PlayerTotalsTest {

    @Test
    void forfeitsAreCountedSeparately_notInPointsOrAverages() {
        List<PlayerGame> games = List.of(
                new PlayerGame(1, 10, 11, 5, false),
                new PlayerGame(2, 11, 12, 10, false),
                new PlayerGame(3, 12, 9, 11, false),
                new PlayerGame(4, 13, 11, 0, true),
                new PlayerGame(5, 14, 0, 11, true));

        PlayerTotals totals = PlayerTotals.of(games, 2);

        assertThat(totals.played()).isEqualTo(3);
        assertThat(totals.wins()).isEqualTo(2);
        assertThat(totals.losses()).isEqualTo(1);
        assertThat(totals.pointsFor()).isEqualTo(32);
        assertThat(totals.pointsAgainst()).isEqualTo(26);
        assertThat(totals.pointDifference()).isEqualTo(6);
        assertThat(totals.averageFor()).isEqualTo(10.7);
        assertThat(totals.averageAgainst()).isEqualTo(8.7);
        assertThat(totals.winPercentage()).isEqualTo(67);
        assertThat(totals.closeWins()).isEqualTo(1);
        assertThat(totals.closeLosses()).isEqualTo(1);
        assertThat(totals.shutouts()).isZero();
        assertThat(totals.officialWins()).isEqualTo(1);
        assertThat(totals.officialLosses()).isEqualTo(1);
    }

    @Test
    void shutout_isAPlayedWinWithoutConcededPoints() {
        PlayerTotals totals = PlayerTotals.of(List.of(new PlayerGame(1, 2, 11, 0, false)), 2);

        assertThat(totals.shutouts()).isEqualTo(1);
        assertThat(totals.winPercentage()).isEqualTo(100);
    }

    @Test
    void noGames_givesZeroes() {
        PlayerTotals totals = PlayerTotals.of(List.of(), 2);

        assertThat(totals.played()).isZero();
        assertThat(totals.averageFor()).isZero();
        assertThat(totals.winPercentage()).isZero();
    }
}
