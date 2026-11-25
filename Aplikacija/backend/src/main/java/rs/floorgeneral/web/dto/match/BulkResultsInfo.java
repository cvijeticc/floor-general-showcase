package rs.floorgeneral.web.dto.match;

import java.util.List;

/** Odgovor masovnog unosa: sta je upisano i sta je odbijeno, sa razlogom. */
public record BulkResultsInfo(
        int appliedCount,
        int rejectedCount,
        List<MatchResult> applied,
        List<RejectedResult> rejected
) {
}
