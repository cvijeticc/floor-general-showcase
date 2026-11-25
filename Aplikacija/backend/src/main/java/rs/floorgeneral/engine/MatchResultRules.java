package rs.floorgeneral.engine;

import java.util.Optional;

/**
 * BR-11 — pravila ispravnosti rezultata jednog meca. Prva klasa u paketu {@code engine}:
 * ciste funkcije, bez Springa, JPA i HTTP-a. Ulaz su obicni brojevi, izlaz je odluka.
 * Prevod odluke u HTTP status i poruku radi servisni sloj — zato ovde nema izuzetaka sa statusom.
 * <p>
 * <b>Format meca (odluka D-18, potvrdio narucilac 26.09.2026.).</b> Mec nije „prvi do 11":
 * igra se {@code season.durationMinutes} minuta i pobedjuje onaj ko na kraju ima vise poena.
 * Zato je i <b>2:1 ispravan rezultat</b>, iako su tako mali rezultati retki. Jedanaest poena je
 * granica posle koje mec moze da se zavrsi ranije, ali posto sut spolja vredi dva poena, igrac
 * koji vodi 10:7 i pogodi spolja zavrsava <b>12:7</b> — zato je gornja granica
 * {@code season.maxScore} (12), a ne {@code targetScore} (11).
 */
public final class MatchResultRules {

    private MatchResultRules() {
    }

    /** Zasto rezultat nije prihvatljiv. */
    public enum Violation {
        /** Negativan broj poena. */
        NEGATIVE_SCORE,
        /** Neresen rezultat — mec se resava zlatnim kosem, pa ne sme ostati izjednacen. */
        DRAW,
        /** Neko ima vise poena nego sto je u toj sezoni uopste moguce (npr. 13 kad je najvise 12). */
        ABOVE_MAX
    }

    /**
     * @param maxScore najveci moguci rezultat u toj sezoni ({@code season.maxScore}, podrazumevano 12)
     * @return prazno ako je rezultat ispravan, inace razlog odbijanja
     */
    public static Optional<Violation> validate(int maxScore, int scorePlayer1, int scorePlayer2) {
        if (scorePlayer1 < 0 || scorePlayer2 < 0) {
            return Optional.of(Violation.NEGATIVE_SCORE);
        }
        if (scorePlayer1 == scorePlayer2) {
            return Optional.of(Violation.DRAW);
        }
        if (Math.max(scorePlayer1, scorePlayer2) > maxScore) {
            return Optional.of(Violation.ABOVE_MAX);
        }
        return Optional.empty();
    }

    /** Pobednik se izvodi iz rezultata, nikad se ne unosi rucno. */
    public static boolean firstPlayerWon(int scorePlayer1, int scorePlayer2) {
        return scorePlayer1 > scorePlayer2;
    }
}
