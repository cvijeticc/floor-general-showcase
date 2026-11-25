package rs.floorgeneral.web.dto.application;

import jakarta.validation.constraints.NotNull;
import rs.floorgeneral.domain.ApplicationStatus;

/** Rucna promena statusa (javili smo se / odbijen). Primanje ide kroz „napravi igraca". */
public record ChangeApplicationStatusCmd(

        @NotNull(message = "Status je obavezan")
        ApplicationStatus status
) {
}
