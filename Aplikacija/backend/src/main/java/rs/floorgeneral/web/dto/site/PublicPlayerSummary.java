package rs.floorgeneral.web.dto.site;

import java.util.List;

/**
 * Igrac na javnom spisku (tab „Igraci", UC-PUB-10; lideri lige, UC-PUB-08): grupa, bilans iz
 * tabele grupe, forma i nizovi. Samo statistika — visina, tezina, godiste i kontakt se nikad ne
 * objavljuju (odluka D-21).
 *
 * @param position         mesto u grupi; null dok tabela grupe nije preracunata
 * @param form             poslednjih 5 meceva, od starijeg ka novijem
 * @param currentStreak    +3 = tri pobede zaredom, −2 = dva poraza zaredom
 * @param longestWinStreak najduzi niz pobeda u sezoni
 * @param biggestWin       najubedljivija odigrana pobeda (sluzbeni rezultat se ne racuna); null ako je nema
 */
public record PublicPlayerSummary(
        PublicPlayer player,
        Long groupId,
        String groupName,
        Integer position,
        int played,
        int wins,
        int losses,
        int pointsFor,
        int pointsAgainst,
        int pointDifference,
        int points,
        List<PublicFormItem> form,
        int currentStreak,
        int longestWinStreak,
        PublicFormItem biggestWin
) {
}
