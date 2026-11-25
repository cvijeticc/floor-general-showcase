package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageStatus;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.SeasonRepository;
import rs.floorgeneral.repository.StageRepository;
import rs.floorgeneral.web.dto.stage.ChangeStageStatusCmd;
import rs.floorgeneral.web.dto.stage.CreateStageCmd;
import rs.floorgeneral.web.dto.stage.StageInfo;
import rs.floorgeneral.web.dto.stage.UpdateStageCmd;
import rs.floorgeneral.web.mapper.StageMapper;

@ExtendWith(MockitoExtension.class)
class StageServiceTest {

    @Mock
    private StageRepository stageRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupEntryRepository groupEntryRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private StageMapper stageMapper;

    @InjectMocks
    private StageService stageService;

    @Test
    void create_groupStage_keepsFormatAndStartsAsDraft() {
        CreateStageCmd cmd = groupCmd(1, 10, 10, 2, 6, 4);
        Stage mapped = groupStage(null, 1, 10, 10, 2, 6, 4);
        stubCreate(cmd, mapped);

        stageService.create(1L, cmd);

        assertThat(mapped.getStatus()).isEqualTo(StageStatus.DRAFT);
        assertThat(mapped.getGroupCount()).isEqualTo(10);
        assertThat(mapped.getAdditionalAdvance()).isEqualTo(4);
        verify(stageRepository).save(mapped);
    }

    @Test
    void create_groupStageWithoutAdditionalAdvance_defaultsToZero() {
        CreateStageCmd cmd = groupCmd(1, 20, 10, 1, 6, null);
        Stage mapped = groupStage(null, 1, 20, 10, 1, 6, null);
        stubCreate(cmd, mapped);

        stageService.create(1L, cmd);

        assertThat(mapped.getAdditionalAdvance()).isZero();
    }

    @Test
    void create_duplicateOrderNo_throwsConflict() {
        CreateStageCmd cmd = groupCmd(1, 10, 10, 2, 6, 4);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season()));
        when(stageRepository.existsBySeason_SeasonIDAndOrderNo(1L, 1)).thenReturn(true);

        assertThatThrownBy(() -> stageService.create(1L, cmd))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(stageRepository, never()).save(any(Stage.class));
    }

    @Test
    void create_unknownSeason_throwsResourceNotFoundException() {
        CreateStageCmd cmd = groupCmd(1, 10, 10, 2, 6, 4);
        when(seasonRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stageService.create(404L, cmd))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_groupStageWithoutGroupCount_throwsBadRequest() {
        CreateStageCmd cmd = groupCmd(1, null, 10, 2, 6, 4);
        stubCreateWithoutSave(cmd, groupStage(null, 1, null, 10, 2, 6, 4));

        assertThatThrownBy(() -> stageService.create(1L, cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Broj grupa")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_groupStageWithRoundCountThree_throwsBadRequest() {
        CreateStageCmd cmd = groupCmd(1, 10, 10, 3, 6, 4);
        stubCreateWithoutSave(cmd, groupStage(null, 1, 10, 10, 3, 6, 4));

        assertThatThrownBy(() -> stageService.create(1L, cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("krugova");
    }

    @Test
    void create_groupStageWithDirectAdvanceOverGroupSize_throwsBadRequest() {
        CreateStageCmd cmd = groupCmd(1, 10, 10, 2, 11, 0);
        stubCreateWithoutSave(cmd, groupStage(null, 1, 10, 10, 2, 11, 0));

        assertThatThrownBy(() -> stageService.create(1L, cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Direktan prolaz");
    }

    @Test
    void create_knockoutWithoutBracketSize_throwsBadRequest() {
        CreateStageCmd cmd = knockoutCmd(2, null);
        stubCreateWithoutSave(cmd, knockoutStage(null, 2, null));

        assertThatThrownBy(() -> stageService.create(1L, cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("drveta");
    }

    @Test
    void create_knockoutWithBracketSizeNotPowerOfTwo_throwsBadRequest() {
        CreateStageCmd cmd = knockoutCmd(2, 48);
        stubCreateWithoutSave(cmd, knockoutStage(null, 2, 48));

        assertThatThrownBy(() -> stageService.create(1L, cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("stepen dvojke");
    }

    @Test
    void create_knockoutWithGroupParameters_throwsBadRequest() {
        CreateStageCmd cmd = new CreateStageCmd("Playoff", StageType.KNOCKOUT, 2, 10, null, null, null, null, 64);
        Stage mapped = knockoutStage(null, 2, 64);
        mapped.setGroupCount(10);
        stubCreateWithoutSave(cmd, mapped);

        assertThatThrownBy(() -> stageService.create(1L, cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("grupne parametre");
    }

    @Test
    void update_activeStageWithPlayedMatches_throwsConflict() {
        Stage stage = groupStage(3L, 1, 10, 10, 2, 6, 4);
        stage.setStatus(StageStatus.ACTIVE);
        UpdateStageCmd cmd = new UpdateStageCmd("Grupna faza", 1, 12, 10, 2, 6, 4, null);
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(matchRepository.existsByStage_StageIDAndStatusIn(anyLong(), any())).thenReturn(true);

        assertThatThrownBy(() -> stageService.update(3L, cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("odigrane meceve")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void update_activeStageWithoutPlayedMatches_isApplied() {
        Stage stage = groupStage(3L, 1, 10, 10, 2, 6, 4);
        stage.setStatus(StageStatus.ACTIVE);
        UpdateStageCmd cmd = new UpdateStageCmd("Grupna faza", 1, 12, 10, 2, 6, 4, null);
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(matchRepository.existsByStage_StageIDAndStatusIn(anyLong(), any())).thenReturn(false);
        when(stageMapper.toInfo(stage)).thenReturn(info(3L, StageStatus.ACTIVE));

        stageService.update(3L, cmd);

        verify(stageMapper).updateEntityFromCmd(cmd, stage);
    }

    @Test
    void changeStatus_activatingGroupStageBeforeKnockout_requiresMatchingAdvancingCount() {
        Stage stage = groupStage(3L, 1, 10, 10, 2, 6, 0);
        Stage knockout = knockoutStage(4L, 2, 64);
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(groupEntryRepository.countByGroup_Stage_StageID(3L)).thenReturn(100L);
        when(stageRepository.findFirstBySeason_SeasonIDAndOrderNoGreaterThanOrderByOrderNoAsc(anyLong(), anyInt()))
                .thenReturn(Optional.of(knockout));

        assertThatThrownBy(() -> stageService.changeStatus(3L, new ChangeStageStatusCmd(StageStatus.ACTIVE)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("60")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(stage.getStatus()).isEqualTo(StageStatus.DRAFT);
    }

    @Test
    void changeStatus_activatingGroupStageThatFills64_isAllowed() {
        Stage stage = groupStage(3L, 1, 10, 10, 2, 6, 4);
        Stage knockout = knockoutStage(4L, 2, 64);
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(groupEntryRepository.countByGroup_Stage_StageID(3L)).thenReturn(100L);
        when(stageRepository.findFirstBySeason_SeasonIDAndOrderNoGreaterThanOrderByOrderNoAsc(anyLong(), anyInt()))
                .thenReturn(Optional.of(knockout));
        when(stageMapper.toInfo(stage)).thenReturn(info(3L, StageStatus.ACTIVE));

        stageService.changeStatus(3L, new ChangeStageStatusCmd(StageStatus.ACTIVE));

        assertThat(stage.getStatus()).isEqualTo(StageStatus.ACTIVE);
    }

    /** 200 -> 120 -> 64 (odluka D-04): iz prve grupne faze prolazi 120, i to je u redu. */
    @Test
    void changeStatus_activatingGroupStageFollowedByAnotherGroupStage_skipsBracketCheck() {
        Stage stage = groupStage(3L, 1, 20, 10, 1, 6, 0);
        Stage secondGroupStage = groupStage(4L, 2, 12, 10, 1, 5, 4);
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(groupEntryRepository.countByGroup_Stage_StageID(3L)).thenReturn(200L);
        when(stageRepository.findFirstBySeason_SeasonIDAndOrderNoGreaterThanOrderByOrderNoAsc(anyLong(), anyInt()))
                .thenReturn(Optional.of(secondGroupStage));
        when(stageMapper.toInfo(stage)).thenReturn(info(3L, StageStatus.ACTIVE));

        stageService.changeStatus(3L, new ChangeStageStatusCmd(StageStatus.ACTIVE));

        assertThat(stage.getStatus()).isEqualTo(StageStatus.ACTIVE);
    }

    @Test
    void changeStatus_activatingStageWithMorePlayersThanCapacity_throwsConflict() {
        Stage stage = groupStage(3L, 1, 10, 10, 2, 6, 4);
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(groupEntryRepository.countByGroup_Stage_StageID(3L)).thenReturn(101L);

        assertThatThrownBy(() -> stageService.changeStatus(3L, new ChangeStageStatusCmd(StageStatus.ACTIVE)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void changeStatus_backwards_throwsConflict() {
        Stage stage = groupStage(3L, 1, 10, 10, 2, 6, 4);
        stage.setStatus(StageStatus.ACTIVE);
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage));

        assertThatThrownBy(() -> stageService.changeStatus(3L, new ChangeStageStatusCmd(StageStatus.DRAFT)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void delete_stageWithGroups_throwsConflict() {
        Stage stage = groupStage(3L, 1, 10, 10, 2, 6, 4);
        when(stageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(groupRepository.existsByStage_StageID(3L)).thenReturn(true);

        assertThatThrownBy(() -> stageService.delete(3L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(stageRepository, never()).delete(any(Stage.class));
    }

    @Test
    void delete_emptyStage_removesStage() {
        Stage stage = knockoutStage(4L, 2, 64);
        when(stageRepository.findById(4L)).thenReturn(Optional.of(stage));
        when(groupRepository.existsByStage_StageID(4L)).thenReturn(false);
        when(matchRepository.existsByStage_StageID(4L)).thenReturn(false);

        stageService.delete(4L);

        verify(stageRepository).delete(stage);
    }

    @Test
    void findById_unknownId_throwsResourceNotFoundException() {
        when(stageRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stageService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubCreate(CreateStageCmd cmd, Stage mapped) {
        stubCreateWithoutSave(cmd, mapped);
        when(stageRepository.save(mapped)).thenReturn(mapped);
        when(stageMapper.toInfo(mapped)).thenReturn(info(3L, StageStatus.DRAFT));
    }

    private void stubCreateWithoutSave(CreateStageCmd cmd, Stage mapped) {
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season()));
        when(stageRepository.existsBySeason_SeasonIDAndOrderNo(1L, cmd.orderNo())).thenReturn(false);
        when(stageMapper.toEntity(cmd)).thenReturn(mapped);
    }

    private static CreateStageCmd groupCmd(int orderNo, Integer groupCount, Integer groupSize, Integer roundCount,
                                           Integer directAdvance, Integer additionalAdvance) {
        return new CreateStageCmd("Grupna faza", StageType.GROUP, orderNo,
                groupCount, groupSize, roundCount, directAdvance, additionalAdvance, null);
    }

    private static CreateStageCmd knockoutCmd(int orderNo, Integer bracketSize) {
        return new CreateStageCmd("Playoff", StageType.KNOCKOUT, orderNo,
                null, null, null, null, null, bracketSize);
    }

    private static Stage groupStage(Long id, int orderNo, Integer groupCount, Integer groupSize, Integer roundCount,
                                    Integer directAdvance, Integer additionalAdvance) {
        Stage stage = new Stage();
        stage.setStageID(id);
        stage.setSeason(season());
        stage.setName("Grupna faza");
        stage.setType(StageType.GROUP);
        stage.setOrderNo(orderNo);
        stage.setStatus(StageStatus.DRAFT);
        stage.setGroupCount(groupCount);
        stage.setGroupSize(groupSize);
        stage.setRoundCount(roundCount);
        stage.setDirectAdvance(directAdvance);
        stage.setAdditionalAdvance(additionalAdvance);
        return stage;
    }

    private static Stage knockoutStage(Long id, int orderNo, Integer bracketSize) {
        Stage stage = new Stage();
        stage.setStageID(id);
        stage.setSeason(season());
        stage.setName("Playoff");
        stage.setType(StageType.KNOCKOUT);
        stage.setOrderNo(orderNo);
        stage.setStatus(StageStatus.DRAFT);
        stage.setBracketSize(bracketSize);
        return stage;
    }

    private static Season season() {
        Season season = new Season();
        season.setSeasonID(1L);
        season.setName("2026/2027");
        return season;
    }

    private static StageInfo info(Long id, StageStatus status) {
        return new StageInfo(id, 1L, "2026/2027", "Grupna faza", StageType.GROUP, 1, status,
                10, 10, 2, 6, 4, null);
    }
}
