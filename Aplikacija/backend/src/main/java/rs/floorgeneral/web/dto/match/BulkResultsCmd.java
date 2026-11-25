package rs.floorgeneral.web.dto.match;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Celo kolo u jednom pozivu — glavni slucaj upotrebe (UC-ADM-09): uvece jedna osoba unosi
 * ~60 rezultata odjednom. Jedan los red ne obara ceo unos.
 */
public record BulkResultsCmd(

        @NotEmpty(message = "Posaljite bar jedan rezultat")
        @Valid
        List<BulkResultEntry> results
) {
}
