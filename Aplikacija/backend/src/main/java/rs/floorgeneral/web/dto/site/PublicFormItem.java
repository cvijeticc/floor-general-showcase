package rs.floorgeneral.web.dto.site;

import rs.floorgeneral.domain.MatchStatus;

/**
 * Jedan mec forme iz ugla igraca (UC-PUB-06): pobeda ili poraz, rezultat i protivnik — za
 * kvadratic forme i njegov opis („Pobeda 11:8 protiv Zeka, 11. kolo").
 *
 * @param roundName naziv kola playoff-a („Top 64"); null za grupni mec — tada vazi {@code roundNumber}
 */
public record PublicFormItem(
        Long matchId,
        int roundNumber,
        boolean won,
        int scored,
        int conceded,
        PublicPlayer opponent,
        MatchStatus status,
        String roundName
) {
}
