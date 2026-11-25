package rs.floorgeneral.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.GroupEntry;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.engine.PlayerTotals;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.web.dto.site.PublicStatRow;

/**
 * Tab „Statistika" na javnom sajtu: poeni po igracima kroz cele sezone, samo grupnu fazu ili samo
 * playoff (na turniriukraju.com „Sve faze / Samo eliminacije"). Racuna {@link PlayerTotals}.
 * Igrac bez ijednog odigranog meca u fazi se ne prikazuje.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicStatsService {

    /** „Tesan" mec: razlika do 2 poena — jedan sut spolja (D-18). Definicija prikaza, ne format takmicenja. */
    static final int CLOSE_MARGIN = 2;

    private final PublicSiteService publicSiteService;
    private final GroupEntryRepository groupEntryRepository;
    private final MatchRepository matchRepository;

    public List<PublicStatRow> stats(Long seasonId, StatsPhase phase) {
        Season season = publicSiteService.resolveSeason(seasonId);
        StatsPhase wanted = phase == null ? StatsPhase.ALL : phase;

        // Grupa igraca: iz poslednje faze u kojoj igra (D-04), kao na spisku igraca.
        Map<Long, GroupEntry> entries = new HashMap<>();
        for (GroupEntry entry : groupEntryRepository.findVisibleInSeason(season.getSeasonID())) {
            entries.merge(entry.getPlayer().getPlayerID(), entry, (current, candidate) ->
                    candidate.getGroup().getStage().getOrderNo() > current.getGroup().getStage().getOrderNo()
                            ? candidate : current);
        }

        List<Match> matches = PlayerResults.chronological(matchRepository.findPublicInSeason(season.getSeasonID())).stream()
                .filter(match -> wanted == StatsPhase.ALL
                        || (wanted == StatsPhase.GROUP) == (match.getStage().getType() == StageType.GROUP))
                .toList();

        Map<Long, Player> players = new LinkedHashMap<>();
        matches.forEach(match -> {
            players.putIfAbsent(match.getPlayer1().getPlayerID(), match.getPlayer1());
            players.putIfAbsent(match.getPlayer2().getPlayerID(), match.getPlayer2());
        });
        Map<Long, List<Match>> byPlayer = PlayerResults.byPlayer(matches);

        List<PublicStatRow> rows = new ArrayList<>();
        for (Map.Entry<Long, List<Match>> e : byPlayer.entrySet()) {
            long playerId = e.getKey();
            PlayerTotals totals = PlayerTotals.of(PlayerResults.games(e.getValue(), playerId), CLOSE_MARGIN);
            if (totals.played() == 0 && totals.officialWins() + totals.officialLosses() == 0) {
                continue;
            }
            GroupEntry entry = entries.get(playerId);
            rows.add(new PublicStatRow(
                    PublicSiteService.toPlayer(players.get(playerId)),
                    entry == null ? null : entry.getGroup().getGroupID(),
                    entry == null ? null : entry.getGroup().getName(),
                    totals.played(), totals.wins(), totals.losses(), totals.winPercentage(),
                    totals.pointsFor(), totals.pointsAgainst(), totals.pointDifference(),
                    totals.averageFor(), totals.averageAgainst(),
                    totals.closeWins(), totals.closeLosses(), totals.shutouts(),
                    totals.officialWins(), totals.officialLosses()));
        }

        // Kao na turniriukraju.com: najvise datih poena po mecu prvi.
        rows.sort(Comparator.comparingDouble(PublicStatRow::averageFor).reversed()
                .thenComparing(Comparator.comparingInt(PublicStatRow::pointDifference).reversed())
                .thenComparing(row -> row.player().nickname(), String.CASE_INSENSITIVE_ORDER));
        return rows;
    }
}
