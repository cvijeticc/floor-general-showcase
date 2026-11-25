package rs.floorgeneral.web.dto.group;

/**
 * Detalj jedne grupe. Spisak igraca se namerno ne ugradjuje ovde — uzima se sa
 * GET /api/v1/players?groupId={id}, da postoji samo jedno mesto koje vraca igrace.
 */
public record GroupInfo(
        Long id,
        Long stageId,
        String stageName,
        Long seasonId,
        String seasonName,
        String name,
        Integer groupSize,
        long playerCount
) {
}
