package rs.floorgeneral.web.dto.court;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourtCmd(

        @NotBlank(message = "Naziv terena je obavezan")
        @Size(max = 60, message = "Naziv terena moze imati najvise 60 karaktera")
        String name,

        @Size(max = 160, message = "Adresa moze imati najvise 160 karaktera")
        String address
) {
}
