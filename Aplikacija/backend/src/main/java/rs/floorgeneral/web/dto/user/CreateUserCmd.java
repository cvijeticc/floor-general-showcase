package rs.floorgeneral.web.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import rs.floorgeneral.domain.UserRole;

/** UC-ADM-13 — nov nalog. Minimalnu duzinu lozinke proverava servis (parametar, ne konstanta). */
public record CreateUserCmd(

        @NotBlank(message = "Korisnicko ime je obavezno")
        @Size(max = 60, message = "Korisnicko ime moze imati najvise 60 karaktera")
        String username,

        @NotBlank(message = "Lozinka je obavezna")
        String password,

        @Email(message = "Email nije u ispravnom formatu")
        @Size(max = 120, message = "Email moze imati najvise 120 karaktera")
        String email,

        UserRole role
) {
}
