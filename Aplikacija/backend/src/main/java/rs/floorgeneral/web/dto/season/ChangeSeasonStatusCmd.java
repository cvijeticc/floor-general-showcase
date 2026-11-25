package rs.floorgeneral.web.dto.season;

import jakarta.validation.constraints.NotNull;
import rs.floorgeneral.domain.SeasonStatus;

/** Sezona ide DRAFT -> ACTIVE -> FINISHED; unazad se ne vraca (UC-ADM-05). */
public record ChangeSeasonStatusCmd(

        @NotNull(message = "Status sezone je obavezan")
        SeasonStatus status
) {
}
