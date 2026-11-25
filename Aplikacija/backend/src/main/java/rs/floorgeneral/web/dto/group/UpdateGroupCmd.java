package rs.floorgeneral.web.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Grupa se ne seli iz faze u fazu — menja se samo naziv. */
public record UpdateGroupCmd(

        @NotBlank(message = "Naziv grupe je obavezan")
        @Size(max = 40, message = "Naziv grupe moze imati najvise 40 karaktera")
        String name
) {
}
