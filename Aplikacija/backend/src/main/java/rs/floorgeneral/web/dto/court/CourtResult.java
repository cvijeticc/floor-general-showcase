package rs.floorgeneral.web.dto.court;

/** Stavka liste/pretrage terena. */
public record CourtResult(
        Long id,
        String name,
        String address
) {
}
