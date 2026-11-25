package rs.floorgeneral.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.UndoAction;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.SeasonRepository;
import rs.floorgeneral.repository.StageRepository;
import rs.floorgeneral.web.dto.group.CreateGroupCmd;
import rs.floorgeneral.web.dto.group.GroupInfo;
import rs.floorgeneral.web.dto.group.GroupResult;
import rs.floorgeneral.web.dto.group.UpdateGroupCmd;
import rs.floorgeneral.web.mapper.GroupMapper;

/** UC-ADM-06 — grupe. Od migracije V3 grupa pripada Fazi; do sezone se stize preko faze. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final StageRepository stageRepository;
    private final SeasonRepository seasonRepository;
    private final GroupEntryRepository groupEntryRepository;
    private final MatchRepository matchRepository;
    private final GroupMapper groupMapper;
    private final UndoService undoService;

    public List<GroupResult> searchByStage(Long stageId) {
        getStageOrThrow(stageId);
        return toResults(groupRepository.findByStage_StageIDOrderByNameAsc(stageId));
    }

    /** Sve grupe sezone, kroz sve njene faze — redosled prati redni broj faze, pa naziv grupe. */
    public List<GroupResult> searchBySeason(Long seasonId) {
        if (!seasonRepository.existsById(seasonId)) {
            throw new ResourceNotFoundException("Sezona sa ID " + seasonId + " ne postoji");
        }
        return toResults(groupRepository.findBySeasonOrderByStageAndName(seasonId));
    }

    public GroupInfo findById(Long id) {
        Group group = getGroupOrThrow(id);
        return groupMapper.toInfo(group, groupEntryRepository.countByGroup_GroupID(id));
    }

    @Transactional
    public GroupInfo create(Long stageId, CreateGroupCmd cmd) {
        Stage stage = getStageOrThrow(stageId);

        if (groupRepository.existsByStage_StageIDAndNameIgnoreCase(stageId, cmd.name())) {
            throw duplicateName(cmd.name());
        }
        if (stage.getGroupCount() != null && groupRepository.countByStage_StageID(stageId) >= stage.getGroupCount()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Faza " + stage.getName() + " po formatu ima " + stage.getGroupCount() + " grupa i sve su napravljene");
        }

        Group group = groupMapper.toEntity(cmd);
        group.setStage(stage);

        Group saved = groupRepository.save(group);

        undoService.draft(UndoAction.GROUP_CREATE, "Dodavanje grupe " + saved.getName())
                .created(UndoTables.GROUPS, UndoTables.GROUP_ID, saved.getGroupID())
                .save();

        return groupMapper.toInfo(saved, 0L);
    }

    @Transactional
    public GroupInfo update(Long id, UpdateGroupCmd cmd) {
        Group group = getGroupOrThrow(id);

        if (!group.getName().equalsIgnoreCase(cmd.name())
                && groupRepository.existsByStage_StageIDAndNameIgnoreCase(group.getStage().getStageID(), cmd.name())) {
            throw duplicateName(cmd.name());
        }

        UndoDraft undo = undoService.draft(UndoAction.GROUP_UPDATE, "Izmena grupe " + group.getName())
                .changed(UndoTables.GROUPS, UndoTables.GROUP_ID, id);

        groupMapper.updateEntityFromCmd(cmd, group);
        undo.save();

        return groupMapper.toInfo(group, groupEntryRepository.countByGroup_GroupID(id));
    }

    @Transactional
    public void delete(Long id) {
        Group group = getGroupOrThrow(id);

        if (groupEntryRepository.existsByGroup_GroupID(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Grupa " + group.getName() + " ima igrace i ne moze se obrisati — prvo ih uklonite iz grupe");
        }
        if (matchRepository.existsByGroup_GroupID(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Grupa " + group.getName() + " ima meceve i ne moze se obrisati");
        }

        undoService.draft(UndoAction.GROUP_DELETE, "Brisanje grupe " + group.getName())
                .changed(UndoTables.GROUPS, UndoTables.GROUP_ID, id)
                .save();

        groupRepository.delete(group);
    }

    private List<GroupResult> toResults(List<Group> groups) {
        return groups.stream()
                .map(group -> groupMapper.toResult(group, groupEntryRepository.countByGroup_GroupID(group.getGroupID())))
                .toList();
    }

    private Group getGroupOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupa sa ID " + id + " ne postoji"));
    }

    private Stage getStageOrThrow(Long id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faza sa ID " + id + " ne postoji"));
    }

    private static ApiException duplicateName(String name) {
        return new ApiException(HttpStatus.CONFLICT, "Grupa sa nazivom " + name + " vec postoji u ovoj fazi");
    }
}
