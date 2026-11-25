package rs.floorgeneral.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Raspored drveta (BR-05, BR-09) — isti kao na kosturu lige za 64 igraca (M01…M63). */
class BracketMathTest {

    @Test
    void sixtyFourPlayers_haveSixRoundsHalvingEachTime() {
        assertThat(BracketMath.roundCount(64)).isEqualTo(6);
        assertThat(BracketMath.seriesInRound(64, 1)).isEqualTo(32);
        assertThat(BracketMath.seriesInRound(64, 2)).isEqualTo(16);
        assertThat(BracketMath.seriesInRound(64, 6)).isEqualTo(1);
    }

    @Test
    void sizeIsAParameter_notOnlySixtyFour() {
        assertThat(BracketMath.roundCount(128)).isEqualTo(7);
        assertThat(BracketMath.roundCount(4)).isEqualTo(2);
        assertThat(BracketMath.isValidSize(48)).isFalse();
        assertThat(BracketMath.isValidSize(1)).isFalse();
        assertThatThrownBy(() -> BracketMath.roundCount(48)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BracketMath.seriesInRound(64, 7)).isInstanceOf(IllegalArgumentException.class);
    }

    /** M01 + M02 → M33 (gore M01, dole M02); M31 + M32 → M48. */
    @Test
    void winnerGoesToHalfPosition_oddOnTop() {
        assertThat(BracketMath.nextPosition(1)).isEqualTo(1);
        assertThat(BracketMath.nextPosition(2)).isEqualTo(1);
        assertThat(BracketMath.nextPosition(31)).isEqualTo(16);
        assertThat(BracketMath.nextPosition(32)).isEqualTo(16);
        assertThat(BracketMath.goesToFirstSlot(1)).isTrue();
        assertThat(BracketMath.goesToFirstSlot(2)).isFalse();
    }

    @Test
    void numbersFollowTheLeagueSkeleton() {
        assertThat(BracketMath.number(64, 1, 1, false)).isEqualTo(1);
        assertThat(BracketMath.number(64, 1, 32, false)).isEqualTo(32);
        assertThat(BracketMath.number(64, 2, 1, false)).isEqualTo(33);
        assertThat(BracketMath.number(64, 3, 1, false)).isEqualTo(49);
        assertThat(BracketMath.number(64, 4, 1, false)).isEqualTo(57);
        assertThat(BracketMath.number(64, 5, 2, false)).isEqualTo(62);
        assertThat(BracketMath.number(64, 6, 1, false)).isEqualTo(63);
        assertThat(BracketMath.number(64, 6, 1, true)).isEqualTo(64);
    }

    @Test
    void semiFinalIsTheRoundBeforeTheFinal() {
        assertThat(BracketMath.isSemiFinal(64, 5)).isTrue();
        assertThat(BracketMath.isSemiFinal(64, 6)).isFalse();
        assertThat(BracketMath.isSemiFinal(4, 1)).isTrue();
    }

    @Test
    void defaultRoundNames() {
        assertThat(BracketMath.defaultRoundName(64, 1)).isEqualTo("Top 64");
        assertThat(BracketMath.defaultRoundName(64, 3)).isEqualTo("Top 16");
        assertThat(BracketMath.defaultRoundName(64, 4)).isEqualTo("Četvrtfinale");
        assertThat(BracketMath.defaultRoundName(64, 5)).isEqualTo("Polufinale");
        assertThat(BracketMath.defaultRoundName(64, 6)).isEqualTo("Finale");
    }
}
