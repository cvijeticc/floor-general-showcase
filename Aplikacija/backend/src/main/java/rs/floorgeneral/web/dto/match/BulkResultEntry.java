package rs.floorgeneral.web.dto.match;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Jedan red masovnog unosa rezultata (UC-ADM-09). */
public record BulkResultEntry(

        @NotNull(message = "ID meca je obavezan")
        Long matchId,

        @NotNull(message = "Rezultat prvog igraca je obavezan")
        @PositiveOrZero(message = "Rezultat ne moze biti negativan")
        Integer scorePlayer1,

        @NotNull(message = "Rezultat drugog igraca je obavezan")
        @PositiveOrZero(message = "Rezultat ne moze biti negativan")
        Integer scorePlayer2
) {
}
