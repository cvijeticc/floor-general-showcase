package rs.floorgeneral.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** BR-08 — serija na N pobeda (do cetvrtfinala 2, Final Four 1). */
class SeriesRulesTest {

    private static SeriesRules.Game won(int a, int b) {
        return new SeriesRules.Game(a, b);
    }

    @Test
    void bestOfThree_twoWinsDecide() {
        SeriesRules.State afterOne = SeriesRules.evaluate(2, List.of(won(11, 7)));
        assertThat(afterOne.decided()).isFalse();
        assertThat(afterOne.winsFirst()).isEqualTo(1);

        SeriesRules.State afterTwo = SeriesRules.evaluate(2, List.of(won(11, 7), won(12, 10)));
        assertThat(afterTwo.decided()).isTrue();
        assertThat(afterTwo.winner()).contains(SeriesRules.Side.FIRST);
    }

    @Test
    void bestOfThree_oneOneGoesToThirdGame() {
        SeriesRules.State state = SeriesRules.evaluate(2, List.of(won(11, 7), won(8, 11)));
        assertThat(state.decided()).isFalse();
        assertThat(SeriesRules.canAddGame(2, List.of(won(11, 7), won(8, 11)))).isEmpty();

        SeriesRules.State third = SeriesRules.evaluate(2, List.of(won(11, 7), won(8, 11), won(9, 11)));
        assertThat(third.winner()).contains(SeriesRules.Side.SECOND);
        assertThat(third.winsSecond()).isEqualTo(2);
    }

    @Test
    void decidedSeries_doesNotAcceptAnotherGame() {
        assertThat(SeriesRules.canAddGame(2, List.of(won(11, 0), won(11, 1))))
                .contains(SeriesRules.Violation.ALREADY_DECIDED);
        assertThat(SeriesRules.canAddGame(1, List.of(won(16, 14))))
                .contains(SeriesRules.Violation.ALREADY_DECIDED);
    }

    @Test
    void consistency_noGameAfterTheDecidingOne() {
        assertThat(SeriesRules.isConsistent(2, List.of(won(11, 7), won(7, 11), won(11, 9)))).isTrue();
        // Ispravka drugog meca u 11:7 bi seriju resila posle dva — treci mec vise ne sme da postoji.
        assertThat(SeriesRules.isConsistent(2, List.of(won(11, 7), won(11, 7), won(11, 9)))).isFalse();
    }

    @Test
    void maxGames_followsWinsRequired() {
        assertThat(SeriesRules.maxGames(2)).isEqualTo(3);
        assertThat(SeriesRules.maxGames(1)).isEqualTo(1);
        assertThatThrownBy(() -> SeriesRules.evaluate(0, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
