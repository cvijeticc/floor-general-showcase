package rs.floorgeneral.web.dto.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import rs.floorgeneral.domain.SkillLevel;

/**
 * Prijava sa javne forme (floorgeneral.rs/kontakt). Sva polja osim napomena su obavezna, kao na
 * Framer originalu. {@code website} je zamka za botove: forma ga skriva, pa ga popunjava samo bot.
 */
public record CreateApplicationCmd(

        @NotBlank(message = "Ime i prezime su obavezni")
        @Size(max = 120, message = "Ime i prezime mogu imati najvise 120 karaktera")
        String fullName,

        @NotBlank(message = "Nadimak je obavezan")
        @Size(max = 60, message = "Nadimak moze imati najvise 60 karaktera")
        String nickname,

        @NotNull(message = "Visina je obavezna")
        @Min(value = 100, message = "Visina mora biti najmanje 100 cm")
        @Max(value = 260, message = "Visina moze biti najvise 260 cm")
        Integer heightCm,

        @NotNull(message = "Tezina je obavezna")
        @Min(value = 30, message = "Tezina mora biti najmanje 30 kg")
        @Max(value = 250, message = "Tezina moze biti najvise 250 kg")
        Integer weightKg,

        @NotNull(message = "Godiste je obavezno")
        @Min(value = 1900, message = "Godiste ne moze biti pre 1900.")
        @Max(value = 2020, message = "Godiste ne moze biti posle 2020.")
        Integer birthYear,

        @NotBlank(message = "Telefon je obavezan")
        @Size(max = 30, message = "Telefon moze imati najvise 30 karaktera")
        @Pattern(regexp = "^[+0-9 ()/-]{6,30}$", message = "Telefon moze sadrzati samo cifre, razmake i znak +")
        String phone,

        @NotBlank(message = "Email je obavezan")
        @Email(message = "Email nije ispravan")
        @Size(max = 120, message = "Email moze imati najvise 120 karaktera")
        String email,

        @NotNull(message = "Izaberi svoj nivo kosarkaskog znanja")
        SkillLevel skillLevel,

        @Size(max = 500, message = "Napomena moze imati najvise 500 karaktera")
        String note,

        @Size(max = 500, message = "Tekst o sebi moze imati najvise 500 karaktera")
        String about,

        String website
) {
}
