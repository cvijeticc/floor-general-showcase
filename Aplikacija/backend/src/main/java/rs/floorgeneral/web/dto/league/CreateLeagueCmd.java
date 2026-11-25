package rs.floorgeneral.web.dto.league;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLeagueCmd(

        @NotBlank(message = "Naziv lige je obavezan")
        @Size(max = 100, message = "Naziv lige moze imati najvise 100 karaktera")
        String name
) {
}
