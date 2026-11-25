package rs.floorgeneral.web.dto.stage;

import rs.floorgeneral.domain.StageStatus;
import rs.floorgeneral.domain.StageType;

/**
 * Detalj jedne faze sa parametrima formata. Ovde zive brojevi koje je zabranjeno hardkodirati
 * (spec poglavlje 4): broj grupa, velicina grupe, krugovi, prolaz i velicina drveta.
 */
public record StageInfo(
        Long id,
        Long seasonId,
        String seasonName,
        String name,
        StageType type,
        int orderNo,
        StageStatus status,
        Integer groupCount,
        Integer groupSize,
        Integer roundCount,
        Integer directAdvance,
        Integer additionalAdvance,
        Integer bracketSize
) {
}
