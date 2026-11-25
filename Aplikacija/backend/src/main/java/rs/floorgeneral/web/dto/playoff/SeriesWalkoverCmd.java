package rs.floorgeneral.web.dto.playoff;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Serija resena bez (svih) meceva: protivnik se nije pojavio ili je predao, pa {@code winnerId}
 * prolazi dalje (poruka lige 08.11.: „u slucaju da se ne pojavite automatski ce protivnik proci").
 */
public record SeriesWalkoverCmd(
        @NotNull(message = "Izaberite igraca koji prolazi dalje")
        Long winnerId,

        @Size(max = 300, message = "Razlog moze imati najvise 300 znakova")
        String reason
) {
}
