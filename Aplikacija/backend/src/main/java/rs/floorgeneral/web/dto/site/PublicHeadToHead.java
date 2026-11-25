package rs.floorgeneral.web.dto.site;

/** Medjusobni skor sa jednim protivnikom u sezoni (FR-6.5.2). */
public record PublicHeadToHead(
        PublicPlayer opponent,
        int played,
        int wins,
        int losses,
        int pointsFor,
        int pointsAgainst
) {
}
