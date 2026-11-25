package rs.floorgeneral.web.dto.league;

/** Detaljan pregled jedne lige (GET po ID-ju, kreiranje, izmena). */
public record LeagueInfo(
        Long id,
        String name
) {
}
