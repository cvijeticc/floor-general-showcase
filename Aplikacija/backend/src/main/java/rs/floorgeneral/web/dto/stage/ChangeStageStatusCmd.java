package rs.floorgeneral.web.dto.stage;

import jakarta.validation.constraints.NotNull;
import rs.floorgeneral.domain.StageStatus;

/**
 * Faza ide DRAFT -> ACTIVE -> FINISHED. Pri prelasku u ACTIVE servis proverava format
 * (FR-4.2 i kapacitet grupa) — to je trenutak "pre pokretanja faze" iz UC-ADM-15.
 */
public record ChangeStageStatusCmd(

        @NotNull(message = "Status faze je obavezan")
        StageStatus status
) {
}
