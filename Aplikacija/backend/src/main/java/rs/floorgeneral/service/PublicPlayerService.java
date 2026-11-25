package rs.floorgeneral.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.GroupEntry;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Standing;
import rs.floorgeneral.engine.PlayerForm;
import rs.floorgeneral.engine.PlayerGame;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.PlayerRepository;
import rs.floorgeneral.repository.StandingRepository;
import rs.floorgeneral.web.dto.site.PublicHeadToHead;
import rs.floorgeneral.web.dto.site.PublicPlayer;
import rs.floorgeneral.web.dto.site.PublicPlayerProfile;
import rs.floorgeneral.web.dto.site.PublicPlayerSummary;
import rs.floorgeneral.web.dto.site.PublicStandingRow;

/**
 * Javni spisak igraca i profil igraca (UC-PUB-10, UC-PUB-06) — samo citanje, bez prijave.
 * <p>
 * <b>Samo statistika (odluka D-21):</b> visina, tezina, godiste, telefon, uplata i napomena se
 * nikad ne objavljuju — u ligi igraju i zene, a to su osetljivi podaci. DTO-ovi zato ta polja nemaju.
 * <p>
 * <b>Odakle su brojke:</b> ko je na spisku odredjuje prijava u grupu; pobede, porazi, kos-razlika,
 * bodovi i mesto su red materijalizovane tabele grupe — isti kao na tabu „Tabele"; forma, nizovi
 * i medjusobni skor se racunaju iz odigranih meceva ({@link PlayerForm}).
 * <p>
 * Vidljivost je ista kao u {@link PublicSiteService}: sezona i faza nisu u pripremi, otkazan mec
 * se ne prikazuje. Igrac koji nije ni u jednoj vidljivoj grupi i nema vidljiv mec (npr. tek
 * primljena prijava) nema javni profil.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicPlayerService {

    private final PublicSiteService publicSiteService;
    private final PlayerRepository playerRepository;
    private final GroupEntryRepository groupEntryRepository;
    private final StandingRepository standingRepository;
    private final MatchRepository matchRepository;

    /** Svi igraci iz grupa sezone — po fazi, grupi i mestu u tabeli. Bez {@code seasonId}: aktuelna sezona. */
    public List<PublicPlayerSummary> players(Long seasonId) {
        Season season = publicSiteService.resolveSeason(seasonId);

        Map<Long, GroupEntry> entries = latestEntryByPlayer(groupEntryRepository.findVisibleInSeason(season.getSeasonID()));
        Set<Long> groupIds = entries.values().stream()
                .map(entry -> entry.getGroup().getGroupID())
                .collect(Collectors.toSet());

        // Red tabele samo iz grupe u kojoj je igrac sada (poslednja faza), ne iz ranijih faza.
        Map<Long, Standing> standings = new HashMap<>();
        for (Standing standing : standingRepository.findByGroup_GroupIDIn(groupIds)) {
            GroupEntry entry = entries.get(standing.getPlayer().getPlayerID());
            if (entry != null && entry.getGroup().getGroupID().equals(standing.getGroup().getGroupID())) {
                standings.put(standing.getPlayer().getPlayerID(), standing);
            }
        }

        Map<Long, List<Match>> results = PlayerResults.byPlayer(
                PlayerResults.chronological(matchRepository.findPublicInSeason(season.getSeasonID())));

        Comparator<GroupEntry> order = Comparator
                .comparingInt(PublicPlayerService::stageOrder)
                .thenComparing(entry -> entry.getGroup().getName(), PublicSiteService::compareNatural)
                .thenComparingInt(entry -> positionOf(standings.get(entry.getPlayer().getPlayerID())))
                .thenComparing(entry -> entry.getPlayer().getNickname(), String.CASE_INSENSITIVE_ORDER);

        return entries.values().stream()
                .sorted(order)
                .map(entry -> {
                    Long playerId = entry.getPlayer().getPlayerID();
                    return toSummary(entry, standings.get(playerId), results.getOrDefault(playerId, List.of()));
                })
                .toList();
    }

    /** Profil igraca u sezoni; bez {@code seasonId}: aktuelna sezona. */
    public PublicPlayerProfile profile(Long playerId, Long seasonId) {
        Season season = publicSiteService.resolveSeason(seasonId);
        Player player = playerRepository.findById(playerId).orElseThrow(() -> playerNotFound(playerId));

        Optional<GroupEntry> entry = groupEntryRepository
                .findVisibleInSeasonForPlayer(season.getSeasonID(), playerId).stream()
                .max(Comparator.comparingInt(PublicPlayerService::stageOrder));
        List<Match> matches = PlayerResults.chronological(
                matchRepository.findPublicInSeasonForPlayer(season.getSeasonID(), playerId));
        if (entry.isEmpty() && matches.isEmpty()) {
            throw playerNotFound(playerId);
        }

        List<Match> results = matches.stream().filter(PlayerResults::hasResult).toList();
        List<PlayerGame> games = PlayerResults.games(results, playerId);

        List<PublicStandingRow> groupStandings = entry
                .map(e -> standingRepository.findByGroup_GroupIDOrderByPositionAsc(e.getGroup().getGroupID()).stream()
                        .map(PublicSiteService::toRow)
                        .toList())
                .orElse(List.of());

        // Protivnici su vec ucitani uz meceve (join fetch) — H2H ih samo preuzima po ID-ju.
        Map<Long, PublicPlayer> opponents = new LinkedHashMap<>();
        results.forEach(match -> {
            Player opponent = PlayerResults.opponent(match, playerId);
            opponents.putIfAbsent(opponent.getPlayerID(), PublicSiteService.toPlayer(opponent));
        });

        return new PublicPlayerProfile(
                PublicSiteService.toPlayer(player),
                entry.map(e -> PublicSiteService.toGroup(e.getGroup())).orElse(null),
                groupStandings,
                PlayerResults.form(results, playerId, PlayerResults.PROFILE_FORM),
                PlayerForm.currentStreak(games),
                PlayerForm.longestWinStreak(games),
                PlayerForm.biggestWin(games).map(PlayerGame::matchId).orElse(null),
                PlayerForm.heaviestLoss(games).map(PlayerGame::matchId).orElse(null),
                matches.stream().map(PublicSiteService::toMatch).toList(),
                PlayerForm.headToHead(games).stream()
                        .map(h -> new PublicHeadToHead(opponents.get(h.opponentId()), h.played(), h.wins(),
                                h.losses(), h.pointsFor(), h.pointsAgainst()))
                        .toList());
    }

    private static PublicPlayerSummary toSummary(GroupEntry entry, Standing standing, List<Match> results) {
        long playerId = entry.getPlayer().getPlayerID();
        List<PlayerGame> games = PlayerResults.games(results, playerId);
        return new PublicPlayerSummary(
                PublicSiteService.toPlayer(entry.getPlayer()),
                entry.getGroup().getGroupID(),
                entry.getGroup().getName(),
                standing == null ? null : standing.getPosition(),
                standing == null ? 0 : standing.getPlayed(),
                standing == null ? 0 : standing.getWins(),
                standing == null ? 0 : standing.getLosses(),
                standing == null ? 0 : standing.getPointsFor(),
                standing == null ? 0 : standing.getPointsAgainst(),
                standing == null ? 0 : standing.getPointDifference(),
                standing == null ? 0 : standing.getPoints(),
                PlayerResults.form(results, playerId, PlayerResults.SHORT_FORM),
                PlayerForm.currentStreak(games),
                PlayerForm.longestWinStreak(games),
                PlayerForm.biggestWin(games)
                        .map(game -> PlayerResults.formItem(results, playerId, game.matchId()))
                        .orElse(null));
    }

    /** Sezona sa vise grupnih faza (D-04): igrac se prikazuje u grupi iz poslednje faze u kojoj igra. */
    private static Map<Long, GroupEntry> latestEntryByPlayer(List<GroupEntry> entries) {
        Map<Long, GroupEntry> latest = new HashMap<>();
        for (GroupEntry entry : entries) {
            latest.merge(entry.getPlayer().getPlayerID(), entry,
                    (current, candidate) -> stageOrder(candidate) > stageOrder(current) ? candidate : current);
        }
        return latest;
    }

    private static int stageOrder(GroupEntry entry) {
        return entry.getGroup().getStage().getOrderNo();
    }

    private static int positionOf(Standing standing) {
        return standing == null ? Integer.MAX_VALUE : standing.getPosition();
    }

    private static ResourceNotFoundException playerNotFound(Long playerId) {
        return new ResourceNotFoundException("Igrac sa ID " + playerId + " ne igra u ovoj sezoni");
    }
}
