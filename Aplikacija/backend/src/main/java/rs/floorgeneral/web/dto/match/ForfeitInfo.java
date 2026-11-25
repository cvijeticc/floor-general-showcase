package rs.floorgeneral.web.dto.match;

/**
 * Odgovor na sluzbeni rezultat. {@code warning} je popunjen kad igrac dodje do praga nedolazaka
 * iz BR-10.4 — sistem tada samo PREDLAZE iskljucenje, odluku donosi admin.
 */
public record ForfeitInfo(
        MatchInfo match,
        String warning
) {
}
