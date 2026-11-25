package rs.floorgeneral.engine;

/**
 * Jedan red tabele grupe (BR-02). Izlaz iz {@link StandingsCalculator}.
 *
 * @param tieBreak kriterijum BR-03 kojim je igrac odvojen od izjednacenih; null kad nije bilo izjednacenja
 */
public record StandingRow(
        long playerId,
        int position,
        int played,
        int wins,
        int losses,
        int pointsFor,
        int pointsAgainst,
        int pointDifference,
        int points,
        TieBreak.Criterion tieBreak
) {
}
