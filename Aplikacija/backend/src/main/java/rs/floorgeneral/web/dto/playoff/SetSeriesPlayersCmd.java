package rs.floorgeneral.web.dto.playoff;

import jakarta.validation.constraints.Size;

/** Rucno postavljanje para (kostur prvog kola ili zamena igraca koji je odustao). */
public record SetSeriesPlayersCmd(
        Long player1Id,

        Long player2Id,

        @Size(max = 40, message = "Oznaka nosioca moze imati najvise 40 znakova")
        String seed1,

        @Size(max = 40, message = "Oznaka nosioca moze imati najvise 40 znakova")
        String seed2
) {
}
