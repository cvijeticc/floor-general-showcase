package rs.floorgeneral.engine;

/**
 * Odigran mec sveden na ono sto tabeli treba. Ulaz u {@link StandingsCalculator} — bez JPA,
 * bez statusa, bez datuma: samo dva igraca, rezultat i podatak da li je gubitnik kaznjen
 * predajom ili nedolaskom (tada dobija {@code pointsForForfeit} umesto {@code pointsForLoss}).
 * <p>
 * {@code bothForfeited} (D-23): obojica su predala ili se nijedan nije pojavio. Mec nema pobednika,
 * oba igraca dobijaju poraz i {@code pointsForForfeit} bodova; {@link #winnerId()} i
 * {@link #loserId()} se tada ne koriste.
 */
public record PlayedMatch(
        long player1Id,
        long player2Id,
        int scorePlayer1,
        int scorePlayer2,
        boolean loserForfeited,
        boolean bothForfeited
) {

    public PlayedMatch(long player1Id, long player2Id, int scorePlayer1, int scorePlayer2, boolean loserForfeited) {
        this(player1Id, player2Id, scorePlayer1, scorePlayer2, loserForfeited, false);
    }

    /** Obostrani sluzbeni rezultat (D-23) — rezultat je onaj koji sezona daje kaznjenom igracu. */
    public static PlayedMatch doubleForfeit(long player1Id, long player2Id, int forfeitLoserScore) {
        return new PlayedMatch(player1Id, player2Id, forfeitLoserScore, forfeitLoserScore, true, true);
    }

    public long winnerId() {
        return scorePlayer1 > scorePlayer2 ? player1Id : player2Id;
    }

    public long loserId() {
        return scorePlayer1 > scorePlayer2 ? player2Id : player1Id;
    }
}
