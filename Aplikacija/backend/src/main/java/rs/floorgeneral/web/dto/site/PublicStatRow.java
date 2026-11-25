package rs.floorgeneral.web.dto.site;

/**
 * Red statistike igraca (tab „Statistika"). Samo brojke sa terena — bez licnih podataka (D-21).
 * Sluzbeni rezultati nisu u odigranim mecevima ni u poenima; broje se posebno.
 *
 * @param played        odigrani mecevi u izabranoj fazi (bez sluzbenih rezultata)
 * @param averageFor    prosek datih poena po mecu, jedna decimala
 * @param winPercentage procenat pobeda 0–100
 * @param closeWins     pobede razlikom do 2 poena (jedan sut spolja)
 * @param shutouts      pobede bez primljenog poena
 */
public record PublicStatRow(
        PublicPlayer player,
        Long groupId,
        String groupName,
        int played,
        int wins,
        int losses,
        int winPercentage,
        int pointsFor,
        int pointsAgainst,
        int pointDifference,
        double averageFor,
        double averageAgainst,
        int closeWins,
        int closeLosses,
        int shutouts,
        int officialWins,
        int officialLosses
) {
}
