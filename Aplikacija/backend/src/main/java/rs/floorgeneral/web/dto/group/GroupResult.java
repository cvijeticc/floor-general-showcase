package rs.floorgeneral.web.dto.group;

/** Stavka liste grupa; playerCount je „koliko je mesta popunjeno" iz UC-ADM-07. */
public record GroupResult(
        Long id,
        Long stageId,
        String stageName,
        String name,
        Integer groupSize,
        long playerCount
) {
}
