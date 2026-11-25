package rs.floorgeneral.web.dto.user;

import jakarta.validation.constraints.NotBlank;

/** Promena sopstvene lozinke — trazi staru lozinku (UC-ADM-13). */
public record ChangeOwnPasswordCmd(

        @NotBlank(message = "Stara lozinka je obavezna")
        String oldPassword,

        @NotBlank(message = "Nova lozinka je obavezna")
        String newPassword
) {
}
