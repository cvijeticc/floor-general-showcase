package rs.floorgeneral.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Court;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.domain.UndoAction;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.CourtRepository;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.PlayerRepository;
import rs.floorgeneral.web.dto.match.CreateMatchCmd;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.match.MatchResult;
import rs.floorgeneral.web.dto.match.PostponeMatchCmd;
import rs.floorgeneral.web.dto.match.UpdateMatchCmd;
import rs.floorgeneral.web.mapper.MatchMapper;

/**
 * UC-ADM-08 — zakazivanje meca i unos vec odigranog meca.
 * <p>
 * Retroaktivan unos je normalan slucaj (odluka D-07): datum u proslosti se ne odbija, a mec sme
 * odmah da dobije rezultat, bez prolaska kroz SCHEDULED. Rezultat proverava i upisuje
 * {@link MatchResultWriter} (BR-11).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private static final Sort BY_SCHEDULE = Sort.by(Sort.Order.asc("roundNumber"), Sort.Order.asc("scheduledStart"));

    private final MatchRepository matchRepository;
    private final GroupRepository groupRepository;
    private final GroupEntryRepository groupEntryRepository;
    private final PlayerRepository playerRepository;
    private final CourtRepository courtRepository;
    private final MatchMapper matchMapper;
    private final StandingsService standingsService;
    private final UndoService undoService;

    public List<MatchResult> search(Long groupId, Long playerId, Long courtId, LocalDate date,
                                    MatchStatus status, Integer round) {
        List<Specification<Match>> filters = new ArrayList<>();
        if (groupId != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("group").get("groupID"), groupId));
        }
        if (playerId != null) {
            filters.add((root, query, cb) -> cb.or(
                    cb.equal(root.get("player1").get("playerID"), playerId),
                    cb.equal(root.get("player2").get("playerID"), playerId)));
        }
        if (courtId != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("court").get("courtID"), courtId));
        }
        if (date != null) {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.plusDays(1).atStartOfDay();
            filters.add((root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("scheduledStart"), from),
                    cb.lessThan(root.get("scheduledStart"), to)));
        }
        if (status != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (round != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("roundNumber"), round));
        }

        return matchRepository.findAll(Specification.allOf(filters), BY_SCHEDULE).stream()
                .map(matchMapper::toResult)
                .toList();
    }

    public MatchInfo findById(Long id) {
        return matchMapper.toInfo(getMatchOrThrow(id));
    }

    @Transactional
    public MatchInfo create(CreateMatchCmd cmd) {
        Group group = groupRepository.findById(cmd.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupa sa ID " + cmd.groupId() + " ne postoji"));

        if (cmd.player1Id().equals(cmd.player2Id())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Igrac ne moze igrati sam protiv sebe");
        }

        Player player1 = getPlayerInGroupOrThrow(cmd.player1Id(), group);
        Player player2 = getPlayerInGroupOrThrow(cmd.player2Id(), group);
        boolean alreadyPlayed = cmd.scorePlayer1() != null || cmd.scorePlayer2() != null;
        if (!alreadyPlayed) {
            checkStillPlaying(player1);
            checkStillPlaying(player2);
        }
        Court court = findCourt(cmd.courtId());

        Match match = new Match();
        match.setStage(group.getStage());
        match.setGroup(group);
        match.setRoundNumber(cmd.roundNumber());
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setCourt(court);
        match.setScheduledStart(cmd.scheduledStart());
        match.setNote(cmd.note());
        match.setStatus(MatchStatus.SCHEDULED);

        checkSlotIsFree(match, null);
        applyResultIfPresent(match, cmd.scorePlayer1(), cmd.scorePlayer2());

        Match saved = matchRepository.save(match);
        if (saved.getStatus().hasResult()) {
            standingsService.recomputeForMatch(saved);
        }

        undoService.draft(UndoAction.MATCH_CREATE, "Unos meca: " + describe(saved))
                .created(UndoTables.MATCHES, UndoTables.MATCH_ID, saved.getMatchID())
                .recompute(group.getGroupID())
                .save();

        return matchMapper.toInfo(saved);
    }

    @Transactional
    public MatchInfo update(Long id, UpdateMatchCmd cmd) {
        Match match = getMatchOrThrow(id);
        MatchResultWriter.rejectSeriesGame(match);

        UndoDraft undo = undoService.draft(UndoAction.MATCH_UPDATE, "Izmena meca br. " + id + ": " + describe(match))
                .changed(UndoTables.MATCHES, UndoTables.MATCH_ID, id);

        match.setRoundNumber(cmd.roundNumber());
        match.setCourt(findCourt(cmd.courtId()));
        match.setScheduledStart(cmd.scheduledStart());
        match.setNote(cmd.note());

        checkSlotIsFree(match, id);
        undo.save();

        return matchMapper.toInfo(match);
    }

    /** Odlaganje: nov termin + obavezan razlog; status prelazi u POSTPONED (UC-ADM-08). */
    @Transactional
    public MatchInfo postpone(Long id, PostponeMatchCmd cmd) {
        Match match = getMatchOrThrow(id);
        MatchResultWriter.rejectSeriesGame(match);

        if (match.getStatus().hasResult()) {
            throw new ApiException(HttpStatus.CONFLICT, "Odigran mec se ne moze odloziti");
        }

        UndoDraft undo = undoService.draft(UndoAction.MATCH_POSTPONE, "Odlaganje meca br. " + id + ": " + describe(match))
                .changed(UndoTables.MATCHES, UndoTables.MATCH_ID, id);

        match.setScheduledStart(cmd.scheduledStart());
        match.setNote(cmd.note());
        match.setStatus(MatchStatus.POSTPONED);

        checkSlotIsFree(match, id);
        undo.save();

        return matchMapper.toInfo(match);
    }

    @Transactional
    public void delete(Long id) {
        Match match = getMatchOrThrow(id);
        MatchResultWriter.rejectSeriesGame(match);

        if (match.getStatus().hasResult()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Mec sa upisanim rezultatom se ne brise — ispravite rezultat ili ga otkazite");
        }

        undoService.draft(UndoAction.MATCH_DELETE, "Brisanje meca br. " + id + ": " + describe(match))
                .changed(UndoTables.MATCHES, UndoTables.MATCH_ID, id)
                .save();

        matchRepository.delete(match);
    }

    /** Kratak opis meca za stek ponistavanja: „Marko Markovic - Petar Petrovic (kolo 3)". */
    static String describe(Match match) {
        return match.getPlayer1().getFullName() + " - " + match.getPlayer2().getFullName()
                + " (kolo " + match.getRoundNumber() + ")";
    }

    /** Unos rezultata odmah pri kreiranju (retroaktivan unos). Pravila su u MatchResultWriter. */
    private void applyResultIfPresent(Match match, Integer scorePlayer1, Integer scorePlayer2) {
        if (scorePlayer1 == null && scorePlayer2 == null) {
            return;
        }
        if (scorePlayer1 == null || scorePlayer2 == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Unesite oba rezultata ili nijedan — mec ne moze imati samo jedan upisan rezultat");
        }

        MatchResultWriter.applyOrThrow(match, scorePlayer1, scorePlayer2);
    }

    /**
     * Prost preklop iz UC-ADM-08: igrac nema dva meca u istom terminu, a teren nije dvostruko zauzet.
     * Pun proracun sa pauzom izmedju meceva (BR-07.2) dolazi u UC-ENG-07.
     */
    private void checkSlotIsFree(Match match, Long excludedMatchId) {
        LocalDateTime slot = match.getScheduledStart();
        if (slot == null || match.getStatus() == MatchStatus.CANCELLED || match.getStatus() == MatchStatus.POSTPONED) {
            return;
        }

        for (Player player : List.of(match.getPlayer1(), match.getPlayer2())) {
            matchRepository.findAtSlotForPlayer(slot, player.getPlayerID()).stream()
                    .filter(other -> !other.getMatchID().equals(excludedMatchId))
                    .findFirst()
                    .ifPresent(other -> {
                        throw new ApiException(HttpStatus.CONFLICT,
                                "Igrac " + player.getFullName() + " vec ima mec u tom terminu (mec br. "
                                        + other.getMatchID() + ", kolo " + other.getRoundNumber() + ")");
                    });
        }

        if (match.getCourt() != null) {
            matchRepository.findAtSlotOnCourt(slot, match.getCourt().getCourtID()).stream()
                    .filter(other -> !other.getMatchID().equals(excludedMatchId))
                    .findFirst()
                    .ifPresent(other -> {
                        throw new ApiException(HttpStatus.CONFLICT,
                                "Teren " + match.getCourt().getName() + " je u tom terminu vec zauzet (mec br. "
                                        + other.getMatchID() + ")");
                    });
        }
    }

    /**
     * BR-10 — igrac koji je odustao ili je iskljucen ne dobija nov termin. Mec sa rezultatom se ipak
     * prima: to je retroaktivan unos meca odigranog pre odustanka (odluka D-07).
     */
    private static void checkStillPlaying(Player player) {
        if (player.getStatus() != PlayerStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Igrac " + player.getFullName()
                    + (player.getStatus() == PlayerStatus.DISQUALIFIED ? " je iskljucen" : " je odustao")
                    + " i ne moze dobiti nov mec");
        }
    }

    private Player getPlayerInGroupOrThrow(Long playerId, Group group) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Igrac sa ID " + playerId + " ne postoji"));

        if (groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(group.getGroupID(), playerId).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Igrac " + player.getFullName() + " nije u grupi " + group.getName());
        }
        return player;
    }

    private Court findCourt(Long courtId) {
        if (courtId == null) {
            return null;
        }
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Teren sa ID " + courtId + " ne postoji"));
    }

    private Match getMatchOrThrow(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mec sa ID " + id + " ne postoji"));
    }
}
