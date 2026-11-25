package rs.floorgeneral.web.dto.match;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Zakazivanje meca ili unos vec odigranog meca (odluka D-07 — retroaktivan unos je normalan slucaj).
 * Kad se posalju oba rezultata, mec se odmah cuva kao zavrsen, bez prolaska kroz SCHEDULED.
 * Datum u proslosti je dozvoljen i ne proverava se.
 */
public record CreateMatchCmd(

        @NotNull(message = "Grupa je obavezna")
        Long groupId,

        @Positive(message = "Kolo mora biti vece od nule")
        int roundNumber,

        @NotNull(message = "Prvi igrac je obavezan")
        Long player1Id,

        @NotNull(message = "Drugi igrac je obavezan")
        Long player2Id,

        Long courtId,

        LocalDateTime scheduledStart,

        @PositiveOrZero(message = "Rezultat ne moze biti negativan")
        Integer scorePlayer1,

        @PositiveOrZero(message = "Rezultat ne moze biti negativan")
        Integer scorePlayer2,

        @Size(max = 500, message = "Napomena moze imati najvise 500 karaktera")
        String note
) {
}
