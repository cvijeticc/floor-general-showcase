package rs.floorgeneral.web.dto.match;

/** Red masovnog unosa koji nije prihvacen, sa razlogom na srpskom. */
public record RejectedResult(
        Long matchId,
        String reason
) {
}
