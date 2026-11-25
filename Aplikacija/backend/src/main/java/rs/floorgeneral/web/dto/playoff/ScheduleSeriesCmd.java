package rs.floorgeneral.web.dto.playoff;

import java.time.LocalDateTime;

/** Termin i teren serije (svi mecevi serije se igraju istog dana — poruka lige 08.11.). */
public record ScheduleSeriesCmd(
        LocalDateTime scheduledStart,
        Long courtId
) {
}
