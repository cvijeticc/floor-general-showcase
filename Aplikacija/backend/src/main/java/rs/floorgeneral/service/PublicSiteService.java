package rs.floorgeneral.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Series;
import rs.floorgeneral.domain.SeasonStatus;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageStatus;
import rs.floorgeneral.domain.Standing;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.SeasonRepository;
import rs.floorgeneral.repository.StandingRepository;
import rs.floorgeneral.web.dto.site.PublicMatch;
import rs.floorgeneral.web.dto.site.PublicPlayer;
import rs.floorgeneral.web.dto.site.PublicSeason;
import rs.floorgeneral.web.dto.site.PublicStandingRow;

/**
 * Javni sajt, tab „Rezultati" (UC-PUB-02, 04, 05) — samo citanje, bez prijave.
 * Vidi se samo sezona koja nije u pripremi i grupe faza koje nisu u pripremi: admin moze da
 * slaze sledecu sezonu, a da posetioci ne vide polupopunjene grupe.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicSiteService {

    private static final Set<MatchStatus> UPCOMING =
            EnumSet.of(MatchStatus.SCHEDULED, MatchStatus.ANNOUNCED, MatchStatus.IN_PROGRESS, MatchStatus.POSTPONED);

    private static final Sort BY_ROUND_AND_TIME =
            Sort.by(Sort.Order.asc("roundNumber"), Sort.Order.asc("scheduledStart"), Sort.Order.asc("matchID"));

    /** „Grupa 2" pre „Grupa 10" — abecedno bi bilo obrnuto. */
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)\\s*$");

    private final SeasonRepository seasonRepository;
    private final GroupRepository groupRepository;
    private final StandingRepository standingRepository;
    private final MatchRepository matchRepository;

    public PublicSeason currentSeason() {
        Season season = resolveSeason(null);

        List<PublicSeason.Group> groups = groupRepository.findBySeasonOrderByStageAndName(season.getSeasonID())
                .stream()
                .filter(g -> g.getStage().getStatus() != StageStatus.DRAFT)
                .sorted(Comparator.comparingInt((Group g) -> g.getStage().getOrderNo())
                        .thenComparing(Group::getName, PublicSiteService::compareNatural))
                .map(PublicSiteService::toGroup)
                .toList();

        return new PublicSeason(season.getSeasonID(), season.getName(), season.getLeague().getName(),
                season.getStartDate(), season.getEndDate(), groups);
    }

    public List<PublicStandingRow> standings(Long groupId) {
        getVisibleGroupOrThrow(groupId);
        return standingRepository.findByGroup_GroupIDOrderByPositionAsc(groupId).stream()
                .map(PublicSiteService::toRow)
                .toList();
    }

    /**
     * Trazena sezona, ako nije u pripremi; bez ID-ja aktuelna — aktivna, a ako nje nema,
     * poslednja zavrsena. Zajednicko sa javnim profilom i spiskom igraca (PublicPlayerService).
     */
    Season resolveSeason(Long seasonId) {
        if (seasonId != null) {
            return seasonRepository.findById(seasonId)
                    .filter(season -> season.getStatus() != SeasonStatus.DRAFT)
                    .orElseThrow(() -> new ResourceNotFoundException("Sezona sa ID " + seasonId + " ne postoji"));
        }
        return seasonRepository.findFirstByStatusOrderByStartDateDesc(SeasonStatus.ACTIVE)
                .or(() -> seasonRepository.findFirstByStatusOrderByStartDateDesc(SeasonStatus.FINISHED))
                .orElseThrow(() -> new ResourceNotFoundException("Nema aktivne sezone"));
    }

    /**
     * Mecevi sezone, po grupi ili svi. {@code played}: true = odigrani (rezultati),
     * false = predstojeci (raspored), null = svi. Otkazani se ne prikazuju.
     */
    public List<PublicMatch> matches(Long seasonId, Long groupId, Boolean played) {
        List<Specification<Match>> filters = new ArrayList<>();
        filters.add((root, query, cb) -> cb.equal(root.get("stage").get("season").get("seasonID"), seasonId));
        filters.add((root, query, cb) -> cb.notEqual(root.get("stage").get("status"), StageStatus.DRAFT));
        filters.add((root, query, cb) ->
                cb.notEqual(root.get("stage").get("season").get("status"), SeasonStatus.DRAFT));
        filters.add((root, query, cb) -> cb.notEqual(root.get("status"), MatchStatus.CANCELLED));
        if (groupId != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("group").get("groupID"), groupId));
        }
        if (Boolean.TRUE.equals(played)) {
            filters.add((root, query, cb) -> root.get("status").in(
                    EnumSet.of(MatchStatus.FINISHED, MatchStatus.FORFEIT, MatchStatus.WALKOVER,
                            MatchStatus.DOUBLE_FORFEIT)));
        } else if (Boolean.FALSE.equals(played)) {
            filters.add((root, query, cb) -> root.get("status").in(UPCOMING));
        }

        return matchRepository.findAll(Specification.allOf(filters), BY_ROUND_AND_TIME).stream()
                .map(PublicSiteService::toMatch)
                .toList();
    }

    private Group getVisibleGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .filter(g -> g.getStage().getStatus() != StageStatus.DRAFT
                        && g.getStage().getSeason().getStatus() != SeasonStatus.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa sa ID " + groupId + " ne postoji"));
    }

    /* Mapiranje je zajednicko sa javnim profilom (PublicPlayerService) — zato package-private. */

    static PublicSeason.Group toGroup(Group group) {
        Stage stage = group.getStage();
        return new PublicSeason.Group(group.getGroupID(), group.getName(), stage.getStageID(), stage.getName(),
                stage.getDirectAdvance(), stage.getAdditionalAdvance());
    }

    static PublicStandingRow toRow(Standing s) {
        return new PublicStandingRow(s.getPosition(), toPlayer(s.getPlayer()), s.getPlayed(), s.getWins(),
                s.getLosses(), s.getPointsFor(), s.getPointsAgainst(), s.getPointDifference(), s.getPoints(),
                s.getTieBreak());
    }

    static PublicMatch toMatch(Match m) {
        Series series = m.getSeries();
        return new PublicMatch(
                m.getMatchID(),
                m.getGroup() == null ? null : m.getGroup().getGroupID(),
                m.getGroup() == null ? null : m.getGroup().getName(),
                m.getRoundNumber(),
                toPlayer(m.getPlayer1()),
                toPlayer(m.getPlayer2()),
                m.getScorePlayer1(),
                m.getScorePlayer2(),
                m.getWinner() == null ? null : m.getWinner().getPlayerID(),
                m.getStatus(),
                m.getCourt() == null ? null : m.getCourt().getName(),
                m.getScheduledStart(),
                m.getStage().getType(),
                roundNameOf(m),
                series == null ? null : series.getSeriesID(),
                m.getGameNumber());
    }

    /** Naziv kola playoff-a za mec serije („Top 64", „Za 3. mesto"); null za grupni mec. */
    static String roundNameOf(Match m) {
        Series series = m.getSeries();
        if (series == null) {
            return null;
        }
        return series.isThirdPlace() ? "Za 3. mesto" : series.getRound().getName();
    }

    static PublicPlayer toPlayer(Player p) {
        return new PublicPlayer(p.getPlayerID(), p.getFullName(), p.getNickname());
    }

    static int compareNatural(String a, String b) {
        Matcher ma = TRAILING_NUMBER.matcher(a);
        Matcher mb = TRAILING_NUMBER.matcher(b);
        if (ma.find() && mb.find()) {
            String prefixA = a.substring(0, ma.start());
            String prefixB = b.substring(0, mb.start());
            if (prefixA.equalsIgnoreCase(prefixB)) {
                return Long.compare(Long.parseLong(ma.group(1)), Long.parseLong(mb.group(1)));
            }
        }
        return a.compareToIgnoreCase(b);
    }
}
