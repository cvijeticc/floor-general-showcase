package rs.floorgeneral.web.dto.stage;

import rs.floorgeneral.domain.StageStatus;
import rs.floorgeneral.domain.StageType;

/** Stavka liste faza jedne sezone. */
public record StageResult(
        Long id,
        Long seasonId,
        String name,
        StageType type,
        int orderNo,
        StageStatus status
) {
}
