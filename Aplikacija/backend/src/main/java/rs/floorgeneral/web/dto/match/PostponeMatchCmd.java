package rs.floorgeneral.web.dto.match;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Odlaganje: nov termin i OBAVEZAN razlog (UC-ADM-08). Status prelazi u POSTPONED. */
public record PostponeMatchCmd(

        LocalDateTime scheduledStart,

        @NotBlank(message = "Razlog odlaganja je obavezan")
        @Size(max = 500, message = "Razlog moze imati najvise 500 karaktera")
        String note
) {
}
