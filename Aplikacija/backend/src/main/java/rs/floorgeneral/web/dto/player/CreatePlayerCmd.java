package rs.floorgeneral.web.dto.player;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Polja prate pravu bazu lige (Prijavljeni.xls) — vidi V5 i odluku D-16. */
public record CreatePlayerCmd(

        @NotBlank(message = "Ime je obavezno")
        @Size(max = 60, message = "Ime moze imati najvise 60 karaktera")
        String firstName,

        @NotBlank(message = "Prezime je obavezno")
        @Size(max = 60, message = "Prezime moze imati najvise 60 karaktera")
        String lastName,

        @NotBlank(message = "Nadimak je obavezan")
        @Size(max = 60, message = "Nadimak moze imati najvise 60 karaktera")
        String nickname,

        @Min(value = 1900, message = "Godiste ne moze biti pre 1900.")
        @Max(value = 2020, message = "Godiste ne moze biti posle 2020.")
        Integer birthYear,

        @Size(max = 30, message = "Telefon moze imati najvise 30 karaktera")
        String phone,

        @Min(value = 100, message = "Visina mora biti najmanje 100 cm")
        @Max(value = 260, message = "Visina moze biti najvise 260 cm")
        Integer heightCm,

        @Min(value = 30, message = "Tezina mora biti najmanje 30 kg")
        @Max(value = 250, message = "Tezina moze biti najvise 250 kg")
        Integer weightKg,

        @Size(max = 40, message = "Podatak o uplati moze imati najvise 40 karaktera")
        String paymentNote,

        @Size(max = 500, message = "Napomena moze imati najvise 500 karaktera")
        String note
) {
}
