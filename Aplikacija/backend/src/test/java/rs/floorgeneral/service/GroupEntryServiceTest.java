package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import rs.floorgeneral.domain.GroupEntry;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageType;
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

@ExtendWith(MockitoExtension.class)
class GroupEntryServiceTest {

    @Mock
    private GroupEntryRepository groupEntryRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private StageRepository stageRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private PlayerMapper playerMapper;

    @Mock
    private UndoService undoService;

    @InjectMocks
    private GroupEntryService groupEntryService;

    @BeforeEach
    void stubUndo() {
        UndoTestSupport.stubUndo(undoService);
    }

    @Test
    void findUnassignedForStage_mapsPlayers() {
        Player player = player(1L, PlayerStatus.ACTIVE);
        when(stageRepository.existsById(3L)).thenReturn(true);
        when(playerRepository.findUnassignedForStage(3L)).thenReturn(List.of(player));
        when(playerMapper.toResult(player))
                .thenReturn(new PlayerResult(1L, "Marko", "Marković", "Mare", "Marko Marković",
                        "Marko Marković Mare", 1995, null, null, null, PlayerStatus.ACTIVE));

        List<PlayerResult> results = groupEntryService.findUnassignedForStage(3L);

        assertThat(results).hasSize(1);
    }

    @Test
    void findUnassignedForStage_unknownStage_throwsResourceNotFoundException() {
        when(stageRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> groupEntryService.findUnassignedForStage(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignPlayers_savesEntryForEveryPlayer() {
        Group group = group(1L, 10);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        stubAssignable(1L);
        stubAssignable(2L);
        when(groupEntryRepository.countByGroup_GroupID(1L)).thenReturn(0L, 2L);
        when(groupMapper.toInfo(group, 2L)).thenReturn(info(2L));

        GroupInfo result = groupEntryService.assignPlayers(1L, new AssignPlayersCmd(List.of(1L, 2L)));

        verify(groupEntryRepository, times(2)).save(any(GroupEntry.class));
        assertThat(result.playerCount()).isEqualTo(2L);
    }

    @Test
    void assignPlayers_ignoresRepeatedIdsInSameRequest() {
        Group group = group(1L, 10);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        stubAssignable(1L);
        when(groupEntryRepository.countByGroup_GroupID(1L)).thenReturn(0L, 1L);
        when(groupMapper.toInfo(group, 1L)).thenReturn(info(1L));

        groupEntryService.assignPlayers(1L, new AssignPlayersCmd(List.of(1L, 1L)));

        verify(groupEntryRepository, times(1)).save(any(GroupEntry.class));
    }

    @Test
    void assignPlayers_withdrawnPlayer_throwsConflict() {
        Group group = group(1L, 10);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L, PlayerStatus.WITHDRAWN)));

        assertThatThrownBy(() -> groupEntryService.assignPlayers(1L, new AssignPlayersCmd(List.of(1L))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("nije aktivan")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(groupEntryRepository, never()).save(any(GroupEntry.class));
    }

    @Test
    void assignPlayers_playerAlreadyInAnotherGroupOfSameStage_throwsConflict() {
        Group group = group(1L, 10);
        Group otherGroup = group(2L, 10);
        otherGroup.setName("Grupa B");
        GroupEntry existing = new GroupEntry();
        existing.setGroup(otherGroup);
        existing.setPlayer(player(1L, PlayerStatus.ACTIVE));

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L, PlayerStatus.ACTIVE)));
        when(groupEntryRepository.findByPlayerInStage(1L, 3L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> groupEntryService.assignPlayers(1L, new AssignPlayersCmd(List.of(1L))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Grupa B")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void assignPlayers_playerAlreadyInThisGroup_throwsConflict() {
        Group group = group(1L, 10);
        GroupEntry existing = new GroupEntry();
        existing.setGroup(group);
        existing.setPlayer(player(1L, PlayerStatus.ACTIVE));

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L, PlayerStatus.ACTIVE)));
        when(groupEntryRepository.findByPlayerInStage(1L, 3L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> groupEntryService.assignPlayers(1L, new AssignPlayersCmd(List.of(1L))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("vec u grupi");
    }

    @Test
    void assignPlayers_overGroupSize_throwsConflict() {
        Group group = group(1L, 2);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        stubAssignable(1L);
        stubAssignable(2L);
        when(groupEntryRepository.countByGroup_GroupID(1L)).thenReturn(1L);

        assertThatThrownBy(() -> groupEntryService.assignPlayers(1L, new AssignPlayersCmd(List.of(1L, 2L))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("prima 2 igraca")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(groupEntryRepository, never()).save(any(GroupEntry.class));
    }

    @Test
    void assignPlayers_unknownPlayer_throwsResourceNotFoundException() {
        Group group = group(1L, 10);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(playerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupEntryService.assignPlayers(1L, new AssignPlayersCmd(List.of(404L))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignPlayers_unknownGroup_throwsResourceNotFoundException() {
        when(groupRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupEntryService.assignPlayers(404L, new AssignPlayersCmd(List.of(1L))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removePlayer_withoutMatches_deletesEntry() {
        Group group = group(1L, 10);
        GroupEntry entry = new GroupEntry();
        entry.setGroup(group);
        entry.setPlayer(player(1L, PlayerStatus.ACTIVE));

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 1L)).thenReturn(Optional.of(entry));
        when(matchRepository.existsInStageForPlayer(3L, 1L)).thenReturn(false);
        when(groupEntryRepository.countByGroup_GroupID(1L)).thenReturn(0L);
        when(groupMapper.toInfo(group, 0L)).thenReturn(info(0L));

        groupEntryService.removePlayer(1L, 1L);

        verify(groupEntryRepository).delete(entry);
    }

    @Test
    void removePlayer_withMatchesInStage_throwsConflict() {
        Group group = group(1L, 10);
        GroupEntry entry = new GroupEntry();
        entry.setGroup(group);
        entry.setPlayer(player(1L, PlayerStatus.ACTIVE));

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 1L)).thenReturn(Optional.of(entry));
        when(matchRepository.existsInStageForPlayer(3L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> groupEntryService.removePlayer(1L, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("meceve")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(groupEntryRepository, never()).delete(any(GroupEntry.class));
    }

    @Test
    void removePlayer_playerNotInGroup_throwsResourceNotFoundException() {
        Group group = group(1L, 10);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupEntryService.removePlayer(1L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubAssignable(Long playerId) {
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player(playerId, PlayerStatus.ACTIVE)));
        when(groupEntryRepository.findByPlayerInStage(playerId, 3L)).thenReturn(Optional.empty());
    }

    private static Stage stage(Integer groupSize) {
        Season season = new Season();
        season.setSeasonID(1L);
        season.setName("2026/2027");

        Stage stage = new Stage();
        stage.setStageID(3L);
        stage.setSeason(season);
        stage.setName("Grupna faza");
        stage.setType(StageType.GROUP);
        stage.setGroupCount(10);
        stage.setGroupSize(groupSize);
        return stage;
    }

    private static Group group(Long id, Integer groupSize) {
        Group group = new Group();
        group.setGroupID(id);
        group.setName("Grupa A");
        group.setStage(stage(groupSize));
        return group;
    }

    private static Player player(Long id, PlayerStatus status) {
        Player player = new Player();
        player.setPlayerID(id);
        player.setFirstName("Marko");
        player.setLastName("Marković");
        player.setStatus(status);
        return player;
    }

    private static GroupInfo info(long playerCount) {
        return new GroupInfo(1L, 3L, "Grupna faza", 1L, "2026/2027", "Grupa A", 10, playerCount);
    }
}
