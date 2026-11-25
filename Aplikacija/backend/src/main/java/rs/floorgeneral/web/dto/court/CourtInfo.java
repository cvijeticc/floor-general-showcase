package rs.floorgeneral.web.dto.court;

/** Detaljan pregled jednog terena (GET po ID-ju, kreiranje, izmena). */
public record CourtInfo(
        Long id,
        String name,
        String address
) {
}
