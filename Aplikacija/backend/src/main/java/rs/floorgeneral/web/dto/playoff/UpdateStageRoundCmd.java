package rs.floorgeneral.web.dto.playoff;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Naziv kola i format: broj pobeda u seriji i format meca (prazno = format sezone). */
public record UpdateStageRoundCmd(
        @NotBlank(message = "Naziv kola je obavezan")
        @Size(max = 60, message = "Naziv kola moze imati najvise 60 znakova")
        String name,

        @NotNull(message = "Broj potrebnih pobeda je obavezan")
        @Min(value = 1, message = "Serija trazi bar jednu pobedu")
        @Max(value = 4, message = "Serija moze trajati najvise do 4 pobede")
        Integer winsRequired,

        @Min(value = 1, message = "Granica poena mora biti pozitivna")
        Integer targetScore,

        @Min(value = 1, message = "Najveci rezultat mora biti pozitivan")
        Integer maxScore,

        @Min(value = 1, message = "Trajanje mora biti pozitivno")
        Integer durationMinutes
) {
}
