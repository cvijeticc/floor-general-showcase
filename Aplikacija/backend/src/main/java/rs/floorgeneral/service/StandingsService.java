package rs.floorgeneral.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Standing;
import rs.floorgeneral.engine.PlayedMatch;
import rs.floorgeneral.engine.ScoringRules;
import rs.floorgeneral.engine.StandingRow;
import rs.floorgeneral.engine.StandingsCalculator;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.StandingRepository;
import rs.floorgeneral.web.dto.standing.StandingResult;

/**
 * UC-ENG-01 (preracun) + UC-ADM-12 (pregled). Racunanje radi cist {@code engine}; ovde se samo
 * skupljaju ulazi iz baze i upisuje rezultat u materijalizovanu tabelu {@code standings} (FR-6.5.1).
 * <p>
 * <b>Idempotentno (BR-12.3):</b> preracun radi <i>upsert</i> po igracu — postojeci red se azurira,
 * nov se dodaje, a red igraca koji vise nije u grupi se brise. Isti ulaz zato uvek daje isto stanje
 * tabele, koliko god puta se preracun pokrene.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandingsService {

    private final StandingRepository standingRepository;
    private final GroupRepository groupRepository;
    private final GroupEntryRepository groupEntryRepository;
    private final MatchRepository matchRepository;

    /** Cita materijalizovanu tabelu; ne racuna u letu (FR-6.5.1). */
    public List<StandingResult> findByGroup(Long groupId) {
        getGroupOrThrow(groupId);
        return standingRepository.findByGroup_GroupIDOrderByPositionAsc(groupId).stream()
                .map(StandingsService::toResult)
                .toList();
    }

    /**
     * Preracunava tabelu grupe i vraca novo stanje. Poziva se posle svake izmene rezultata,
     * a postoji i kao zaseban endpoint radi oporavka i provere.
     */
    @Transactional
    public List<StandingResult> recompute(Long groupId) {
        Group group = getGroupOrThrow(groupId);
        Season season = group.getStage().getSeason();

        Map<Long, Player> playersInGroup = new HashMap<>();
        groupEntryRepository.findByGroup_GroupID(groupId)
                .forEach(entry -> playersInGroup.put(entry.getPlayer().getPlayerID(), entry.getPlayer()));

        List<PlayedMatch> playedMatches = matchRepository.findByGroup_GroupID(groupId).stream()
                .filter(match -> match.getStatus().hasResult())
                .filter(match -> match.getScorePlayer1() != null && match.getScorePlayer2() != null)
                .map(StandingsService::toPlayedMatch)
                .toList();

        List<StandingRow> table = StandingsCalculator.compute(
                playersInGroup.keySet(),
                playedMatches,
                new ScoringRules(season.getPointsForWin(), season.getPointsForLoss(), season.getPointsForForfeit()));

        Map<Long, Standing> existing = new HashMap<>();
        standingRepository.findByGroup_GroupID(groupId)
                .forEach(standing -> existing.put(standing.getPlayer().getPlayerID(), standing));

        for (StandingRow row : table) {
            Standing standing = existing.remove(row.playerId());
            if (standing == null) {
                standing = new Standing();
                standing.setGroup(group);
                standing.setPlayer(playersInGroup.get(row.playerId()));
            }
            standing.setPosition(row.position());
            standing.setPlayed(row.played());
            standing.setWins(row.wins());
            standing.setLosses(row.losses());
            standing.setPointsFor(row.pointsFor());
            standing.setPointsAgainst(row.pointsAgainst());
            standing.setPointDifference(row.pointDifference());
            standing.setPoints(row.points());
            standing.setTieBreak(row.tieBreak() == null ? null : row.tieBreak().name());
            standingRepository.save(standing);
        }

        // Ostalo je ono sto vise nije u grupi — igrac je uklonjen iz grupe posle prethodnog preracuna.
        existing.values().forEach(standingRepository::delete);

        return findByGroup(groupId);
    }

    /** Preracun posle izmene rezultata; meceve playoffa (bez grupe) preskace. */
    @Transactional
    public void recomputeForMatch(Match match) {
        if (match.getGroup() != null) {
            recompute(match.getGroup().getGroupID());
        }
    }

    private static PlayedMatch toPlayedMatch(Match match) {
        if (match.getStatus() == MatchStatus.DOUBLE_FORFEIT) {
            return new PlayedMatch(
                    match.getPlayer1().getPlayerID(),
                    match.getPlayer2().getPlayerID(),
                    match.getScorePlayer1(),
                    match.getScorePlayer2(),
                    true,
                    true);
        }
        boolean loserForfeited = match.getStatus() == MatchStatus.FORFEIT || match.getStatus() == MatchStatus.WALKOVER;
        return new PlayedMatch(
                match.getPlayer1().getPlayerID(),
                match.getPlayer2().getPlayerID(),
                match.getScorePlayer1(),
                match.getScorePlayer2(),
                loserForfeited);
    }

    private static StandingResult toResult(Standing standing) {
        return new StandingResult(
                standing.getPosition(),
                standing.getPlayer().getPlayerID(),
                standing.getPlayer().getFullName(),
                standing.getPlayed(),
                standing.getWins(),
                standing.getLosses(),
                standing.getPointsFor(),
                standing.getPointsAgainst(),
                standing.getPointDifference(),
                standing.getPoints(),
                standing.getTieBreak());
    }

    private Group getGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa sa ID " + groupId + " ne postoji"));
    }
}
