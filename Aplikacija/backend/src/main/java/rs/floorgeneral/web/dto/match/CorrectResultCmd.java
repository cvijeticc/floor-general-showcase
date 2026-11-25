package rs.floorgeneral.web.dto.match;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** UC-ADM-11 — ispravka vec upisanog rezultata. Razlog je obavezan (BR-12, FR-3.2). */
public record CorrectResultCmd(

        @NotNull(message = "Rezultat prvog igraca je obavezan")
        @PositiveOrZero(message = "Rezultat ne moze biti negativan")
        Integer scorePlayer1,

        @NotNull(message = "Rezultat drugog igraca je obavezan")
        @PositiveOrZero(message = "Rezultat ne moze biti negativan")
        Integer scorePlayer2,

        @NotBlank(message = "Razlog korekcije je obavezan")
        @Size(max = 400, message = "Razlog moze imati najvise 400 karaktera")
        String reason
) {
}
