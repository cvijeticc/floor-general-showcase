package rs.floorgeneral.web.dto.stage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Tip faze se NE menja izmenom — faza pogresnog tipa se brise i pravi ponovo, jer bi promena
 * tipa ostavila grupe i meceve bez smisla. Parametri se ne diraju kad je faza ACTIVE
 * i vec ima odigrane meceve (UC-ADM-14).
 */
public record UpdateStageCmd(

        @NotBlank(message = "Naziv faze je obavezan")
        @Size(max = 100, message = "Naziv faze moze imati najvise 100 karaktera")
        String name,

        @Positive(message = "Redni broj faze mora biti veci od nule")
        int orderNo,

        @Positive(message = "Broj grupa mora biti veci od nule")
        Integer groupCount,

        @Positive(message = "Velicina grupe mora biti veca od nule")
        Integer groupSize,

        @Positive(message = "Broj krugova mora biti 1 ili 2")
        Integer roundCount,

        @Positive(message = "Direktan prolaz mora biti veci od nule")
        Integer directAdvance,

        @PositiveOrZero(message = "Dodatni prolaz ne moze biti negativan")
        Integer additionalAdvance,

        @Positive(message = "Velicina drveta mora biti veca od nule")
        Integer bracketSize
) {
}
