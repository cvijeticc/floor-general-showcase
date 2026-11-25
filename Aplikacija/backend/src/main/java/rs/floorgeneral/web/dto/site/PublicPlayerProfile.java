package rs.floorgeneral.web.dto.site;

import java.util.List;

/**
 * Javni profil igraca (UC-PUB-06, FR-6.1.7): grupa, bilans, forma, svi mecevi i medjusobni skor.
 * Samo statistika — visina, tezina, godiste i kontakt se nikad ne objavljuju (odluka D-21).
 *
 * @param group               grupa iz poslednje faze u kojoj igrac igra; null ako nije ni u jednoj grupi
 * @param groupStandings      cela tabela te grupe — bilans igraca je njegov red, isti kao na tabu „Tabele";
 *                            prazna dok tabela nije preracunata
 * @param form                poslednjih 10 meceva, od starijeg ka novijem
 * @param biggestWinMatchId   ID meca iz {@code matches}; null dok igrac nema odigranu pobedu
 *                            (sluzbeni rezultat se ne racuna)
 * @param heaviestLossMatchId ID meca iz {@code matches}; null dok igrac nema odigran poraz
 * @param matches             svi javni mecevi igraca u sezoni, hronoloski — odigrani i predstojeci
 */
public record PublicPlayerProfile(
        PublicPlayer player,
        PublicSeason.Group group,
        List<PublicStandingRow> groupStandings,
        List<PublicFormItem> form,
        int currentStreak,
        int longestWinStreak,
        Long biggestWinMatchId,
        Long heaviestLossMatchId,
        List<PublicMatch> matches,
        List<PublicHeadToHead> headToHead
) {
}
