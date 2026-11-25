package rs.floorgeneral.engine;

/**
 * BR-01 — bodovanje. Vrednosti dolaze iz sezone ({@code Season}), nikad iz koda:
 * podrazumevano 2 za pobedu, 1 za poraz u odigranom mecu, 0 za predaju ili nedolazak.
 */
public record ScoringRules(
        int pointsForWin,
        int pointsForLoss,
        int pointsForForfeit
) {
}
