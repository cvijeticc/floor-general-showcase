package rs.floorgeneral.web.dto.match;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Menja se termin, teren, kolo i napomena — NIKAD rezultat (za njega su UC-ADM-09 i UC-ADM-11). */
public record UpdateMatchCmd(

        @Positive(message = "Kolo mora biti vece od nule")
        int roundNumber,

        Long courtId,

        LocalDateTime scheduledStart,

        @Size(max = 500, message = "Napomena moze imati najvise 500 karaktera")
        String note
) {
}
