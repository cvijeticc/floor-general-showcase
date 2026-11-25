package rs.floorgeneral.web.dto.playoff;

import java.util.List;

/**
 * Kolo drveta sa formatom koji vazi u njemu — sopstvenim ili, kad ga kolo nema, formatom sezone.
 *
 * @param winsRequired    pobeda potrebnih za prolaz (2 = serija na dve pobede)
 * @param ownFormat       true kad kolo ima sopstveni format (npr. Final Four do 16), false = format sezone
 */
public record RoundInfo(
        Long id,
        int roundNo,
        String name,
        int winsRequired,
        int targetScore,
        int maxScore,
        int durationMinutes,
        boolean ownFormat,
        List<SeriesInfo> series
) {
}
