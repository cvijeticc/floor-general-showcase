package rs.floorgeneral.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** UC-PUB-06 — forma, nizovi i medjusobni skor (FR-6.1.7, FR-6.5.2). Cist engine, bez baze. */
class PlayerFormTest {

    @Test
    void last_keepsNewestGamesInChronologicalOrder() {
        List<PlayerGame> games = List.of(game(1, 11, 8), game(2, 9, 11), game(3, 12, 7));

        assertThat(PlayerForm.last(games, 2)).extracting(PlayerGame::matchId).containsExactly(2L, 3L);
    }

    @Test
    void last_withFewerGamesThanRequested_returnsAll() {
        assertThat(PlayerForm.last(List.of(game(1, 11, 8)), 10)).hasSize(1);
        assertThat(PlayerForm.last(List.of(), 10)).isEmpty();
    }

    @Test
    void currentStreak_countsWinsFromTheNewestGame() {
        List<PlayerGame> games = List.of(game(1, 11, 2), game(2, 3, 11), game(3, 11, 9), game(4, 12, 7));

        assertThat(PlayerForm.currentStreak(games)).isEqualTo(2);
    }

    @Test
    void currentStreak_isNegativeForLosses() {
        List<PlayerGame> games = List.of(game(1, 11, 2), game(2, 3, 11), game(3, 8, 10));

        assertThat(PlayerForm.currentStreak(games)).isEqualTo(-2);
    }

    @Test
    void currentStreak_withoutGames_isZero() {
        assertThat(PlayerForm.currentStreak(List.of())).isZero();
    }

    /** Sluzbeni rezultat je pobeda kao i svaka druga — niz se ne prekida (BR-10.2). */
    @Test
    void streaks_countForfeitWins() {
        List<PlayerGame> games = List.of(game(1, 11, 9), forfeit(2, 11, 0), game(3, 12, 10));

        assertThat(PlayerForm.currentStreak(games)).isEqualTo(3);
        assertThat(PlayerForm.longestWinStreak(games)).isEqualTo(3);
    }

    @Test
    void longestWinStreak_findsLongestRunAnywhereInSeason() {
        List<PlayerGame> games = new ArrayList<>(List.of(game(1, 11, 2), game(2, 11, 3), game(3, 2, 11)));
        games.addAll(List.of(game(4, 11, 4), game(5, 11, 5), game(6, 11, 6), game(7, 5, 11), game(8, 11, 7)));

        assertThat(PlayerForm.longestWinStreak(games)).isEqualTo(3);
        assertThat(PlayerForm.longestWinStreak(List.of(game(1, 0, 11)))).isZero();
    }

    /** 8 minuta, vise poena pobedjuje (D-18) — i 2:1 je pobeda. */
    @Test
    void biggestWin_takesLargestMarginAndEarlierOnTie() {
        List<PlayerGame> games = List.of(game(1, 2, 1), game(2, 11, 4), game(3, 0, 11), game(4, 12, 5));

        assertThat(PlayerForm.biggestWin(games)).map(PlayerGame::matchId).contains(2L);
    }

    /** 11:0 zbog nedolaska protivnika nije odigran mec — ne ulazi u rekorde. */
    @Test
    void records_ignoreForfeits() {
        List<PlayerGame> games = List.of(forfeit(1, 11, 0), game(2, 11, 8), forfeit(3, 0, 11), game(4, 7, 11));

        assertThat(PlayerForm.biggestWin(games)).map(PlayerGame::matchId).contains(2L);
        assertThat(PlayerForm.heaviestLoss(games)).map(PlayerGame::matchId).contains(4L);
        assertThat(PlayerForm.biggestWin(List.of(forfeit(1, 11, 0)))).isEmpty();
    }

    @Test
    void heaviestLoss_isEmptyForUnbeatenPlayer() {
        assertThat(PlayerForm.heaviestLoss(List.of(game(1, 11, 8), game(2, 12, 0)))).isEmpty();
        assertThat(PlayerForm.heaviestLoss(List.of(game(1, 9, 11), game(2, 0, 11), game(3, 1, 12))))
                .map(PlayerGame::matchId).contains(2L);
    }

    /** Dvokruzno: isti protivnik dva puta — skor se sabira u jedan red, redom prvog susreta. */
    @Test
    void headToHead_sumsBothLegsAgainstSameOpponent() {
        List<PlayerGame> games = List.of(
                new PlayerGame(1, 20, 11, 8, false),
                new PlayerGame(2, 30, 6, 11, false),
                new PlayerGame(3, 20, 9, 11, false));

        List<PlayerForm.HeadToHead> h2h = PlayerForm.headToHead(games);

        assertThat(h2h).extracting(PlayerForm.HeadToHead::opponentId).containsExactly(20L, 30L);
        PlayerForm.HeadToHead first = h2h.get(0);
        assertThat(first.played()).isEqualTo(2);
        assertThat(first.wins()).isEqualTo(1);
        assertThat(first.losses()).isEqualTo(1);
        assertThat(first.pointsFor()).isEqualTo(20);
        assertThat(first.pointsAgainst()).isEqualTo(19);
    }

    @Test
    void game_marginIsFromPlayersPointOfView() {
        assertThat(game(1, 12, 7).margin()).isEqualTo(5);
        assertThat(game(1, 8, 10).margin()).isEqualTo(-2);
        assertThat(game(1, 8, 10).won()).isFalse();
    }

    private static PlayerGame game(long matchId, int scored, int conceded) {
        return new PlayerGame(matchId, 99, scored, conceded, false);
    }

    private static PlayerGame forfeit(long matchId, int scored, int conceded) {
        return new PlayerGame(matchId, 99, scored, conceded, true);
    }
}
