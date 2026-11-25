package rs.floorgeneral.web.dto.playoff;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Ispravka rezultata meca serije; razlog je opcion i upisuje se u napomenu meca. */
public record CorrectSeriesGameCmd(
        @NotNull(message = "Rezultat prvog igraca je obavezan")
        @Min(value = 0, message = "Rezultat ne moze biti negativan")
        Integer score1,

        @NotNull(message = "Rezultat drugog igraca je obavezan")
        @Min(value = 0, message = "Rezultat ne moze biti negativan")
        Integer score2,

        @Size(max = 300, message = "Razlog moze imati najvise 300 znakova")
        String reason
) {
}
