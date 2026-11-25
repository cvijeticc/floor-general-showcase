package rs.floorgeneral.web.dto.groupentry;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Masovno raspoređivanje: admin cekira vise igraca i potvrdi jednom (UC-ADM-07). */
public record AssignPlayersCmd(

        @NotEmpty(message = "Izaberite bar jednog igraca")
        List<Long> playerIds
) {
}
