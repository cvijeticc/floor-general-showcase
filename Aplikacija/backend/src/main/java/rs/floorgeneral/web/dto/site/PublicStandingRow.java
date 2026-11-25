package rs.floorgeneral.web.dto.site;

/**
 * Red javne tabele grupe (UC-PUB-02, BR-02): poz. · igrac · O · P · I · DP · PP · KR · bodovi.
 *
 * @param tieBreak kriterijum BR-03 kojim je igrac odvojen od izjednacenih (FR-5.1): HEAD_TO_HEAD_WINS,
 *                 MINI_TABLE_POINTS…; null kad izjednacenja nije bilo
 */
public record PublicStandingRow(
        int position,
        PublicPlayer player,
        int played,
        int wins,
        int losses,
        int pointsFor,
        int pointsAgainst,
        int pointDifference,
        int points,
        String tieBreak
) {
}
