package rs.floorgeneral.web.dto.match;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** UC-ADM-09 — pojedinacan unos rezultata. Pobednik se izvodi, ne salje se (BR-11). */
public record EnterResultCmd(

        @NotNull(message = "Rezultat prvog igraca je obavezan")
        @PositiveOrZero(message = "Rezultat ne moze biti negativan")
        Integer scorePlayer1,

        @NotNull(message = "Rezultat drugog igraca je obavezan")
        @PositiveOrZero(message = "Rezultat ne moze biti negativan")
        Integer scorePlayer2
) {
}
