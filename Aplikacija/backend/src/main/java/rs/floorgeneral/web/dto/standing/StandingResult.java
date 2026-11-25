package rs.floorgeneral.web.dto.standing;

/**
 * Jedan red tabele grupe onako kako se prikazuje (BR-02):
 * poz. · igrac · odigrano · P · I · DP · PP · KR · bodovi.
 *
 * @param tieBreak kriterijum BR-03 kojim je igrac odvojen od izjednacenih (FR-5.1); null bez izjednacenja
 */
public record StandingResult(
        int position,
        Long playerId,
        String playerName,
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
