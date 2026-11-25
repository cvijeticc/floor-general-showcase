package rs.floorgeneral.web.dto.user;

import jakarta.validation.constraints.NotBlank;

/** Reset tudje lozinke od strane administratora — stara lozinka se ne trazi. */
public record ResetPasswordCmd(

        @NotBlank(message = "Nova lozinka je obavezna")
        String newPassword
) {
}
