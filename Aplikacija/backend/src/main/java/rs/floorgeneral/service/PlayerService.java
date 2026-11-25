package rs.floorgeneral.service;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.GroupEntry;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.domain.UndoAction;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.PlayerRepository;
import rs.floorgeneral.web.dto.player.ChangePlayerStatusCmd;
import rs.floorgeneral.web.dto.player.CreatePlayerCmd;
import rs.floorgeneral.web.dto.player.PlayerInfo;
import rs.floorgeneral.web.dto.player.PlayerResult;
import rs.floorgeneral.web.dto.player.UpdatePlayerCmd;
import rs.floorgeneral.web.mapper.PlayerMapper;

/** UC-ADM-04 — sifarnik igraca. Igrac postoji nezavisno od grupe (od V3, preko GroupEntry). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlayerService {

    private static final Sort BY_NAME = Sort.by(Sort.Direction.ASC, "lastName", "firstName");

    /** Mecevi koji jos nisu odigrani — njih odustanak otkazuje. Odigrani se ne diraju (BR-10.5, N-14a). */
    private static final EnumSet<MatchStatus> NOT_PLAYED = EnumSet.of(
            MatchStatus.SCHEDULED, MatchStatus.ANNOUNCED, MatchStatus.IN_PROGRESS, MatchStatus.POSTPONED);

    private static final int NOTE_MAX = 500;

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final GroupEntryRepository groupEntryRepository;
    private final PlayerMapper playerMapper;
    private final UndoService undoService;

    /** Pretraga po delu imena, prezimena ili nadimka, filter po grupi i po statusu — svi su opcioni. */
    public List<PlayerResult> search(String search, Long groupId, PlayerStatus status) {
        List<Specification<Player>> filters = new ArrayList<>();
        addIfPresent(filters, nameContains(search));
        addIfPresent(filters, inGroup(groupId));
        addIfPresent(filters, hasStatus(status));

        return playerRepository.findAll(Specification.allOf(filters), BY_NAME).stream()
                .map(playerMapper::toResult)
                .toList();
    }

    public PlayerInfo findById(Long id) {
        return playerMapper.toInfo(getPlayerOrThrow(id));
    }

    @Transactional
    public PlayerInfo create(CreatePlayerCmd cmd) {
        Player player = playerMapper.toEntity(cmd);
        player.setStatus(PlayerStatus.ACTIVE);
        Player saved = playerRepository.save(player);

        undoService.draft(UndoAction.PLAYER_CREATE, "Dodavanje igraca " + saved.getDisplayName())
                .created(UndoTables.PLAYERS, UndoTables.PLAYER_ID, saved.getPlayerID())
                .save();

        return playerMapper.toInfo(saved);
    }

    @Transactional
    public PlayerInfo update(Long id, UpdatePlayerCmd cmd) {
        Player player = getPlayerOrThrow(id);

        UndoDraft undo = undoService.draft(UndoAction.PLAYER_UPDATE, "Izmena igraca " + player.getDisplayName())
                .changed(UndoTables.PLAYERS, UndoTables.PLAYER_ID, id);

        playerMapper.updateEntityFromCmd(cmd, player);
        undo.save();

        return playerMapper.toInfo(player);
    }

    /**
     * BR-10 — promena statusa je zasebna operacija jer povlaci posledice na meceve.
     * <p>
     * Igrac koji odustane ili bude iskljucen vise ne igra: svi njegovi mecevi koji jos nisu odigrani
     * prelaze u CANCELLED, sa razlogom u napomeni. Vec odigrani ostaju kakvi jesu — sta se sa njima
     * radi jos nije odluceno (BR-10.5, N-14a). Sve je jedan zapis u steku, pa „Poništi" vraca i
     * status i otkazane meceve. Povratak u ACTIVE ne vraca otkazane meceve sam od sebe.
     */
    @Transactional
    public PlayerInfo changeStatus(Long id, ChangePlayerStatusCmd cmd) {
        Player player = getPlayerOrThrow(id);
        PlayerStatus previous = player.getStatus();

        UndoDraft undo = undoService.draft(UndoAction.PLAYER_STATUS,
                        "Status igraca " + player.getDisplayName() + ": " + previous + " -> " + cmd.status())
                .changed(UndoTables.PLAYERS, UndoTables.PLAYER_ID, id);

        player.setStatus(cmd.status());

        int cancelled = 0;
        if (cmd.status() != PlayerStatus.ACTIVE) {
            String note = cancellationNote(player, cmd);
            for (Match match : matchRepository.findForPlayerInStatuses(id, NOT_PLAYED)) {
                undo.changed(UndoTables.MATCHES, UndoTables.MATCH_ID, match.getMatchID());
                match.setStatus(MatchStatus.CANCELLED);
                match.setNote(note);
                cancelled++;
            }
            if (cancelled > 0) {
                undo.describe(player.getDisplayName() + " " + verb(cmd.status())
                        + ", otkazano meceva: " + cancelled);
            }
        }
        undo.save();

        log.info("Status igraca {} ({}): {} -> {}, otkazano meceva: {}. Razlog: {}",
                player.getPlayerID(), player.getDisplayName(), previous, cmd.status(), cancelled,
                cmd.note() == null || cmd.note().isBlank() ? "(nije naveden)" : cmd.note());

        return playerMapper.toInfo(player);
    }

    /** „Otkazan: Kočo odustao — povreda kolena" (Match.note ima najvise 500 znakova). */
    private static String cancellationNote(Player player, ChangePlayerStatusCmd cmd) {
        String note = "Otkazan: " + player.getFullName() + " " + verb(cmd.status());
        if (cmd.note() != null && !cmd.note().isBlank()) {
            note += " — " + cmd.note().trim();
        }
        return note.length() > NOTE_MAX ? note.substring(0, NOTE_MAX) : note;
    }

    private static String verb(PlayerStatus status) {
        return status == PlayerStatus.DISQUALIFIED ? "iskljucen" : "odustao";
    }

    /** Igrac koji je odigrao mec ili je prijavljen u grupu se ne brise — dobija status WITHDRAWN. */
    @Transactional
    public void delete(Long id) {
        Player player = getPlayerOrThrow(id);

        if (matchRepository.existsByPlayer1_PlayerIDOrPlayer2_PlayerID(id, id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Igrac " + player.getDisplayName() + " ima meceve i ne moze se obrisati — postavite mu status ODUSTAO");
        }
        if (groupEntryRepository.existsByPlayer_PlayerID(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Igrac " + player.getDisplayName() + " je rasporedjen u grupu — prvo ga uklonite iz grupe");
        }

        undoService.draft(UndoAction.PLAYER_DELETE, "Brisanje igraca " + player.getDisplayName())
                .changed(UndoTables.PLAYERS, UndoTables.PLAYER_ID, id)
                .save();

        playerRepository.delete(player);
    }

    private static void addIfPresent(List<Specification<Player>> filters, Specification<Player> filter) {
        if (filter != null) {
            filters.add(filter);
        }
    }

    private Player getPlayerOrThrow(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Igrac sa ID " + id + " ne postoji"));
    }

    private static Specification<Player> nameContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        // Nadimak je ravnopravan sa imenom: admin cesce zna „Laki" nego „Lazar Ristic".
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(root.get("nickname")), pattern));
    }

    private static Specification<Player> hasStatus(PlayerStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Pripadnost grupi je od V3 u group_entries, pa filter ide kroz podupit. */
    private static Specification<Player> inGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> entries = query.subquery(Long.class);
            Root<GroupEntry> entry = entries.from(GroupEntry.class);
            entries.select(cb.literal(1L))
                    .where(cb.equal(entry.get("player"), root),
                            cb.equal(entry.get("group").get("groupID"), groupId));
            return cb.exists(entries);
        };
    }
}
