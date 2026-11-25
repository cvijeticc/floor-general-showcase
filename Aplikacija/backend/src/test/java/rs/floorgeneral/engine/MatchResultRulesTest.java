package rs.floorgeneral.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import rs.floorgeneral.engine.MatchResultRules.Violation;

/**
 * BR-11 uz odluku D-18: mec traje osam minuta i pobedjuje onaj ko ima vise poena, pa je ispravan
 * i rezultat 2:1. Gornja granica je {@code season.maxScore} (12), jer sut spolja vredi dva poena.
 * Neresen rezultat ne postoji — resava se zlatnim kosem.
 */
class MatchResultRulesTest {

    @Test
    void validate_winnerAtTargetScore_isAccepted() {
        assertThat(MatchResultRules.validate(12, 11, 7)).isEmpty();
        assertThat(MatchResultRules.validate(12, 3, 11)).isEmpty();
    }

    /** Ko vodi 10:7 i pogodi spolja zavrsava 12:7 — zato je 12 dozvoljeno, a ne greska. */
    @Test
    void validate_winnerOverTargetScore_isAccepted() {
        assertThat(MatchResultRules.validate(12, 12, 7)).isEmpty();
        assertThat(MatchResultRules.validate(12, 0, 12)).isEmpty();
    }

    /** Mec prekinut istekom vremena: niko nije stigao do 11, pobedjuje onaj ko vodi. */
    @Test
    void validate_matchDecidedOnTime_isAccepted() {
        assertThat(MatchResultRules.validate(12, 8, 10)).isEmpty();
        assertThat(MatchResultRules.validate(12, 5, 7)).isEmpty();
        assertThat(MatchResultRules.validate(12, 2, 1)).isEmpty();
    }

    @Test
    void validate_forfeitScore_isAccepted() {
        assertThat(MatchResultRules.validate(12, 11, 0)).isEmpty();
    }

    @Test
    void validate_draw_isRejected() {
        assertThat(MatchResultRules.validate(12, 10, 10)).contains(Violation.DRAW);
        assertThat(MatchResultRules.validate(12, 0, 0)).contains(Violation.DRAW);
    }

    @Test
    void validate_scoreAboveMax_isRejected() {
        assertThat(MatchResultRules.validate(12, 13, 9)).contains(Violation.ABOVE_MAX);
        assertThat(MatchResultRules.validate(12, 4, 110)).contains(Violation.ABOVE_MAX);
    }

    @Test
    void validate_negativeScore_isRejected() {
        assertThat(MatchResultRules.validate(12, -1, 11)).contains(Violation.NEGATIVE_SCORE);
    }

    /** Gornja granica je parametar sezone — pravila rade i za sezonu sa drugim formatom. */
    @Test
    void validate_respectsSeasonMaxScore() {
        assertThat(MatchResultRules.validate(21, 21, 19)).isEmpty();
        assertThat(MatchResultRules.validate(21, 22, 19)).contains(Violation.ABOVE_MAX);
    }

    @Test
    void firstPlayerWon_followsScores() {
        assertThat(MatchResultRules.firstPlayerWon(11, 7)).isTrue();
        assertThat(MatchResultRules.firstPlayerWon(7, 11)).isFalse();
        assertThat(MatchResultRules.firstPlayerWon(2, 1)).isTrue();
    }
}
