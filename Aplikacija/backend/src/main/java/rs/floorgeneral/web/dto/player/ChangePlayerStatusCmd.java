package rs.floorgeneral.web.dto.player;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import rs.floorgeneral.domain.PlayerStatus;

/**
 * BR-10 — odustanak i diskvalifikacija. Razlog (note) se za sada samo upisuje u log:
 * tabele audit loga nema (UC-ADM-20 je van MVP-a), a Player nema kolonu za belesku.
 */
public record ChangePlayerStatusCmd(

        @NotNull(message = "Status igraca je obavezan")
        PlayerStatus status,

        @Size(max = 500, message = "Razlog moze imati najvise 500 karaktera")
        String note
) {
}
