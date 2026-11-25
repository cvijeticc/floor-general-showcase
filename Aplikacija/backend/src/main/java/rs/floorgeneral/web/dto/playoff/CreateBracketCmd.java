package rs.floorgeneral.web.dto.playoff;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Pravljenje drveta za KNOCKOUT fazu (sledece sezone): kola i sve serije prema {@code bracketSize}.
 * Parovi prvog kola su opcioni — mogu se uneti i kasnije, par po par.
 */
public record CreateBracketCmd(
        @Valid
        List<SetSeriesPlayersCmd> firstRound
) {
}
