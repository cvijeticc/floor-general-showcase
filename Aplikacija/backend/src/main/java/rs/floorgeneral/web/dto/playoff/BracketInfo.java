package rs.floorgeneral.web.dto.playoff;

import java.util.List;
import rs.floorgeneral.domain.StageStatus;

/**
 * Drvo playoff faze za admin ekran „Playoff" (BR-05, BR-08, BR-09): kola sa serijama i mecevima.
 *
 * @param bracketSize broj igraca u drvetu ({@code Stage.bracketSize})
 */
public record BracketInfo(
        Long stageId,
        String stageName,
        StageStatus status,
        int bracketSize,
        List<RoundInfo> rounds
) {
}
