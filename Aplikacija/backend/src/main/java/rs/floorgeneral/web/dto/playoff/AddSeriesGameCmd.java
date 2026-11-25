package rs.floorgeneral.web.dto.playoff;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Rezultat sledeceg meca serije (BR-08). {@code score1} je rezultat prvog igraca serije.
 * Termin i teren su opcioni — bez njih mec nasledjuje termin serije.
 */
public record AddSeriesGameCmd(
        @NotNull(message = "Rezultat prvog igraca je obavezan")
        @Min(value = 0, message = "Rezultat ne moze biti negativan")
        Integer score1,

        @NotNull(message = "Rezultat drugog igraca je obavezan")
        @Min(value = 0, message = "Rezultat ne moze biti negativan")
        Integer score2,

        LocalDateTime scheduledStart,

        Long courtId
) {
}
