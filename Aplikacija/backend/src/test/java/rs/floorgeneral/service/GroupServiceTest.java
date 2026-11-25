package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageType;
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

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private StageRepository stageRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private GroupEntryRepository groupEntryRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private UndoService undoService;

    @InjectMocks
    private GroupService groupService;

    @BeforeEach
    void stubUndo() {
        UndoTestSupport.stubUndo(undoService);
    }

    @Test
    void searchByStage_addsPlayerCountToEveryGroup() {
        Group group = group(1L, "Grupa A");
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage(10)));
        when(groupRepository.findByStage_StageIDOrderByNameAsc(3L)).thenReturn(List.of(group));
        when(groupEntryRepository.countByGroup_GroupID(1L)).thenReturn(7L);
        when(groupMapper.toResult(group, 7L)).thenReturn(result(1L, "Grupa A", 7L));

        List<GroupResult> results = groupService.searchByStage(3L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).playerCount()).isEqualTo(7L);
    }

    @Test
    void searchByStage_unknownStage_throwsResourceNotFoundException() {
        when(stageRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.searchByStage(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchBySeason_unknownSeason_throwsResourceNotFoundException() {
        when(seasonRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> groupService.searchBySeason(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_persistsGroupInStage() {
        CreateGroupCmd cmd = new CreateGroupCmd("Grupa A");
        Stage stage = stage(10);
        Group mapped = new Group();
        mapped.setName("Grupa A");
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(groupRepository.existsByStage_StageIDAndNameIgnoreCase(3L, "Grupa A")).thenReturn(false);
        when(groupRepository.countByStage_StageID(3L)).thenReturn(4L);
        when(groupMapper.toEntity(cmd)).thenReturn(mapped);
        when(groupRepository.save(mapped)).thenReturn(mapped);
        when(groupMapper.toInfo(mapped, 0L)).thenReturn(info(1L, "Grupa A", 0L));

        GroupInfo created = groupService.create(3L, cmd);

        assertThat(mapped.getStage()).isSameAs(stage);
        assertThat(created.playerCount()).isZero();
    }

    @Test
    void create_duplicateNameInSameStage_throwsConflict() {
        CreateGroupCmd cmd = new CreateGroupCmd("Grupa A");
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage(10)));
        when(groupRepository.existsByStage_StageIDAndNameIgnoreCase(3L, "Grupa A")).thenReturn(true);

        assertThatThrownBy(() -> groupService.create(3L, cmd))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(groupRepository, never()).save(any(Group.class));
    }

    /** Broj grupa je parametar faze (UC-ADM-15) — preko njega se ne ide. */
    @Test
    void create_moreGroupsThanFormatAllows_throwsConflict() {
        CreateGroupCmd cmd = new CreateGroupCmd("Grupa K");
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage(10)));
        when(groupRepository.existsByStage_StageIDAndNameIgnoreCase(3L, "Grupa K")).thenReturn(false);
        when(groupRepository.countByStage_StageID(3L)).thenReturn(10L);

        assertThatThrownBy(() -> groupService.create(3L, cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("10")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void create_unknownStage_throwsResourceNotFoundException() {
        when(stageRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.create(404L, new CreateGroupCmd("Grupa A")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_renamesGroup() {
        Group group = group(1L, "Grupa A");
        UpdateGroupCmd cmd = new UpdateGroupCmd("Grupa A1");
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByStage_StageIDAndNameIgnoreCase(3L, "Grupa A1")).thenReturn(false);
        when(groupEntryRepository.countByGroup_GroupID(1L)).thenReturn(3L);
        when(groupMapper.toInfo(group, 3L)).thenReturn(info(1L, "Grupa A1", 3L));

        groupService.update(1L, cmd);

        verify(groupMapper).updateEntityFromCmd(cmd, group);
    }

    @Test
    void update_toExistingNameInSameStage_throwsConflict() {
        Group group = group(1L, "Grupa A");
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByStage_StageIDAndNameIgnoreCase(3L, "Grupa B")).thenReturn(true);

        assertThatThrownBy(() -> groupService.update(1L, new UpdateGroupCmd("Grupa B")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void update_keepingOwnName_isAllowed() {
        Group group = group(1L, "Grupa A");
        UpdateGroupCmd cmd = new UpdateGroupCmd("Grupa A");
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.countByGroup_GroupID(1L)).thenReturn(0L);
        when(groupMapper.toInfo(group, 0L)).thenReturn(info(1L, "Grupa A", 0L));

        groupService.update(1L, cmd);

        verify(groupRepository, never()).existsByStage_StageIDAndNameIgnoreCase(anyLong(), any());
    }

    @Test
    void delete_emptyGroup_removesGroup() {
        Group group = group(1L, "Grupa A");
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.existsByGroup_GroupID(1L)).thenReturn(false);
        when(matchRepository.existsByGroup_GroupID(1L)).thenReturn(false);

        groupService.delete(1L);

        verify(groupRepository).delete(group);
    }

    @Test
    void delete_groupWithPlayers_throwsConflict() {
        Group group = group(1L, "Grupa A");
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.existsByGroup_GroupID(1L)).thenReturn(true);

        assertThatThrownBy(() -> groupService.delete(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("igrace")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(groupRepository, never()).delete(any(Group.class));
    }

    @Test
    void delete_groupWithMatches_throwsConflict() {
        Group group = group(1L, "Grupa A");
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.existsByGroup_GroupID(1L)).thenReturn(false);
        when(matchRepository.existsByGroup_GroupID(1L)).thenReturn(true);

        assertThatThrownBy(() -> groupService.delete(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("meceve");
    }

    @Test
    void findById_unknownId_throwsResourceNotFoundException() {
        when(groupRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Stage stage(Integer groupCount) {
        Season season = new Season();
        season.setSeasonID(1L);
        season.setName("2026/2027");

        Stage stage = new Stage();
        stage.setStageID(3L);
        stage.setSeason(season);
        stage.setName("Grupna faza");
        stage.setType(StageType.GROUP);
        stage.setGroupCount(groupCount);
        stage.setGroupSize(10);
        return stage;
    }

    private static Group group(Long id, String name) {
        Group group = new Group();
        group.setGroupID(id);
        group.setName(name);
        group.setStage(stage(10));
        return group;
    }

    private static GroupInfo info(Long id, String name, long playerCount) {
        return new GroupInfo(id, 3L, "Grupna faza", 1L, "2026/2027", name, 10, playerCount);
    }

    private static GroupResult result(Long id, String name, long playerCount) {
        return new GroupResult(id, 3L, "Grupna faza", name, 10, playerCount);
    }
}
