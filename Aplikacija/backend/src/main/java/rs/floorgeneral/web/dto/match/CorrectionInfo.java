package rs.floorgeneral.web.dto.match;

/**
 * Odgovor na korekciju. {@code warning} je popunjen kad je faza vec zatvorena — tada korekcija
 * menja i plasman, pa admin mora da zna (AC-8).
 */
public record CorrectionInfo(
        MatchInfo match,
        String previousResult,
        String warning
) {
}
