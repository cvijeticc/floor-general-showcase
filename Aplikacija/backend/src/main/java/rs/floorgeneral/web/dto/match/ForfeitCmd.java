package rs.floorgeneral.web.dto.match;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-ADM-10 — sluzbeni rezultat. Rezultat se NE salje: uzima se iz parametara sezone
 * (protivnik forfeitWinnerScore, kaznjeni forfeitLoserScore), pa se ovde bira samo ko je kaznjen.
 * Kod {@link ForfeitType#DOUBLE_FORFEIT} kaznjeni su obojica i {@code losingPlayerId} se ne salje (D-23).
 */
public record ForfeitCmd(

        Long losingPlayerId,

        @NotNull(message = "Tip je obavezan: WALKOVER (nedolazak), FORFEIT (predaja) ili DOUBLE_FORFEIT (obojica)")
        ForfeitType type,

        @NotBlank(message = "Obrazlozenje je obavezno")
        @Size(max = 500, message = "Obrazlozenje moze imati najvise 500 karaktera")
        String note
) {
}
