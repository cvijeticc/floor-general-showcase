package rs.floorgeneral.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.domain.GroupEntry;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.UndoAction;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.PlayerRepository;
import rs.floorgeneral.repository.StageRepository;
import rs.floorgeneral.web.dto.group.GroupInfo;
import rs.floorgeneral.web.dto.groupentry.AssignPlayersCmd;
import rs.floorgeneral.web.dto.player.PlayerResult;
import rs.floorgeneral.web.mapper.GroupMapper;
import rs.floorgeneral.web.mapper.PlayerMapper;

/**
 * UC-ADM-07 — rucno rasporedjivanje igraca u grupe (zreb se za 2026/2027 ne pravi, odluka D-06).
 * Pravilo „igrac je u tacno jednoj grupi PO FAZI" zivi ovde: baza ga ne moze nametnuti bez
 * dupliranja stage_id u group_entries, a bas ono cini sezonu sa dve grupne faze mogucom (D-04).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupEntryService {

    private final GroupEntryRepository groupEntryRepository;
    private final GroupRepository groupRepository;
    private final StageRepository stageRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final GroupMapper groupMapper;
    private final PlayerMapper playerMapper;
    private final UndoService undoService;

    /** Aktivni igraci koji u toj fazi jos nisu ni u jednoj grupi. */
    public List<PlayerResult> findUnassignedForStage(Long stageId) {
        if (!stageRepository.existsById(stageId)) {
            throw new ResourceNotFoundException("Faza sa ID " + stageId + " ne postoji");
        }
        return playerRepository.findUnassignedForStage(stageId).stream()
                .map(playerMapper::toResult)
                .toList();
    }

    @Transactional
    public GroupInfo assignPlayers(Long groupId, AssignPlayersCmd cmd) {
        Group group = getGroupOrThrow(groupId);
        Stage stage = group.getStage();

        List<Long> playerIds = cmd.playerIds().stream().distinct().toList();
        List<Player> players = new ArrayList<>();
        for (Long playerId : playerIds) {
            players.add(validateAssignable(playerId, group, stage));
        }

        long alreadyIn = groupEntryRepository.countByGroup_GroupID(groupId);
        if (stage.getGroupSize() != null && alreadyIn + players.size() > stage.getGroupSize()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Grupa " + group.getName() + " prima " + stage.getGroupSize() + " igraca, a u njoj je vec "
                            + alreadyIn + " — ne moze se dodati jos " + players.size());
        }

        UndoDraft undo = undoService.draft(UndoAction.GROUP_ASSIGN,
                "Rasporedjivanje " + players.size() + " igraca u grupu " + group.getName());

        for (Player player : players) {
            GroupEntry entry = new GroupEntry();
            entry.setGroup(group);
            entry.setPlayer(player);
            groupEntryRepository.save(entry);
            undo.created(UndoTables.GROUP_ENTRIES, UndoTables.GROUP_ENTRY_ID, entry.getGroupEntryID());
        }

        undo.recompute(groupId).save();

        return groupMapper.toInfo(group, groupEntryRepository.countByGroup_GroupID(groupId));
    }

    /** Uklanjanje je dozvoljeno samo dok igrac nema meceve u toj fazi. */
    @Transactional
    public GroupInfo removePlayer(Long groupId, Long playerId) {
        Group group = getGroupOrThrow(groupId);

        GroupEntry entry = groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(groupId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Igrac sa ID " + playerId + " nije u grupi " + group.getName()));

        if (matchRepository.existsInStageForPlayer(group.getStage().getStageID(), playerId)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Igrac " + entry.getPlayer().getFullName()
                            + " ima meceve u ovoj fazi i ne moze se ukloniti iz grupe");
        }

        undoService.draft(UndoAction.GROUP_REMOVE,
                        "Uklanjanje igraca " + entry.getPlayer().getFullName() + " iz grupe " + group.getName())
                .changed(UndoTables.GROUP_ENTRIES, UndoTables.GROUP_ENTRY_ID, entry.getGroupEntryID())
                .recompute(groupId)
                .save();

        groupEntryRepository.delete(entry);

        return groupMapper.toInfo(group, groupEntryRepository.countByGroup_GroupID(groupId));
    }

    private Player validateAssignable(Long playerId, Group group, Stage stage) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Igrac sa ID " + playerId + " ne postoji"));

        if (player.getStatus() != PlayerStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Igrac " + player.getFullName() + " nije aktivan (status " + player.getStatus()
                            + ") i ne moze se rasporediti");
        }

        groupEntryRepository.findByPlayerInStage(playerId, stage.getStageID()).ifPresent(existing -> {
            if (existing.getGroup().getGroupID().equals(group.getGroupID())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Igrac " + player.getFullName() + " je vec u grupi " + group.getName());
            }
            throw new ApiException(HttpStatus.CONFLICT,
                    "Igrac " + player.getFullName() + " je vec u grupi " + existing.getGroup().getName()
                            + " u ovoj fazi — prvo ga uklonite odande");
        });

        return player;
    }

    private Group getGroupOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa sa ID " + id + " ne postoji"));
    }
}
