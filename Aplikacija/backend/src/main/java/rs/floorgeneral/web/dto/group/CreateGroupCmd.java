package rs.floorgeneral.web.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Faza se uzima iz putanje. Broj grupa se ne hardkoduje — admin ih pravi koliko format trazi. */
public record CreateGroupCmd(

        @NotBlank(message = "Naziv grupe je obavezan")
        @Size(max = 40, message = "Naziv grupe moze imati najvise 40 karaktera")
        String name
) {
}
