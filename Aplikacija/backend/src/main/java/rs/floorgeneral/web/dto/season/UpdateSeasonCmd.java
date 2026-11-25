package rs.floorgeneral.web.dto.season;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Parametri bodovanja su opcioni: kad se izostave, ostaju nepromenjeni. Menjati se smeju
 * samo dok je sezona u statusu DRAFT (FR-4.1) — inace servis vraca 409.
 * Liga se ne menja kroz izmenu sezone.
 */
public record UpdateSeasonCmd(

        @NotBlank(message = "Naziv sezone je obavezan")
        @Size(max = 100, message = "Naziv sezone moze imati najvise 100 karaktera")
        String name,

        LocalDate startDate,

        LocalDate endDate,

        @Positive(message = "Broj igraca mora biti veci od nule")
        int playerCount,

        @PositiveOrZero(message = "Bodovi za pobedu ne mogu biti negativni")
        Integer pointsForWin,

        @PositiveOrZero(message = "Bodovi za poraz ne mogu biti negativni")
        Integer pointsForLoss,

        @PositiveOrZero(message = "Bodovi za predaju ne mogu biti negativni")
        Integer pointsForForfeit,

        @PositiveOrZero(message = "Sluzbeni rezultat pobednika ne moze biti negativan")
        Integer forfeitWinnerScore,

        @PositiveOrZero(message = "Sluzbeni rezultat gubitnika ne moze biti negativan")
        Integer forfeitLoserScore,

        @Min(value = 1, message = "Mec se mora igrati bar do 1 poena")
        Integer targetScore,

        @Min(value = 1, message = "Najveci moguci rezultat mora biti bar 1")
        Integer maxScore,

        @Min(value = 1, message = "Mec mora trajati bar 1 minut")
        Integer durationMinutes
) {
}
