package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import rs.floorgeneral.domain.League;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.SeasonStatus;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.LeagueRepository;
import rs.floorgeneral.repository.SeasonRepository;
import rs.floorgeneral.repository.StageRepository;
import rs.floorgeneral.web.dto.season.ChangeSeasonStatusCmd;
import rs.floorgeneral.web.dto.season.CreateSeasonCmd;
import rs.floorgeneral.web.dto.season.SeasonInfo;
import rs.floorgeneral.web.dto.season.UpdateSeasonCmd;
import rs.floorgeneral.web.mapper.SeasonMapper;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private StageRepository stageRepository;

    @Mock
    private SeasonMapper seasonMapper;

    @InjectMocks
    private SeasonService seasonService;

    @Test
    void create_withoutScoringParameters_keepsDefaults() {
        League league = league(1L);
        CreateSeasonCmd cmd = new CreateSeasonCmd(1L, "2026/2027",
                LocalDate.of(2026, 9, 19), LocalDate.of(2026, 12, 20), 100,
                null, null, null, null, null, null, null, null);
        Season mapped = new Season();
        mapped.setName("2026/2027");
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(seasonRepository.existsByLeague_LeagueIDAndNameIgnoreCase(1L, "2026/2027")).thenReturn(false);
        when(seasonMapper.toEntity(cmd)).thenReturn(mapped);
        when(seasonRepository.save(mapped)).thenReturn(mapped);
        when(seasonMapper.toInfo(mapped)).thenReturn(info(7L, SeasonStatus.DRAFT));

        SeasonInfo result = seasonService.create(cmd);

        ArgumentCaptor<Season> saved = ArgumentCaptor.forClass(Season.class);
        verify(seasonRepository).save(saved.capture());
        Season season = saved.getValue();
        assertThat(season.getStatus()).isEqualTo(SeasonStatus.DRAFT);
        assertThat(season.getPointsForWin()).isEqualTo(2);
        assertThat(season.getPointsForLoss()).isEqualTo(1);
        assertThat(season.getPointsForForfeit()).isZero();
        assertThat(season.getForfeitWinnerScore()).isEqualTo(11);
        assertThat(season.getForfeitLoserScore()).isZero();
        assertThat(season.getTargetScore()).isEqualTo(11);
        assertThat(result.id()).isEqualTo(7L);
    }

    @Test
    void create_withCustomScoring_overridesDefaults() {
        League league = league(1L);
        CreateSeasonCmd cmd = new CreateSeasonCmd(1L, "Rekreativna 2027", null, null, 40,
                3, 1, 0, 21, 0, 21, 21, 10);
        Season mapped = new Season();
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(seasonRepository.existsByLeague_LeagueIDAndNameIgnoreCase(1L, "Rekreativna 2027")).thenReturn(false);
        when(seasonMapper.toEntity(cmd)).thenReturn(mapped);
        when(seasonRepository.save(mapped)).thenReturn(mapped);
        when(seasonMapper.toInfo(mapped)).thenReturn(info(8L, SeasonStatus.DRAFT));

        seasonService.create(cmd);

        assertThat(mapped.getPointsForWin()).isEqualTo(3);
        assertThat(mapped.getTargetScore()).isEqualTo(21);
    }

    @Test
    void create_unknownLeague_throwsResourceNotFoundException() {
        CreateSeasonCmd cmd = new CreateSeasonCmd(404L, "2026/2027", null, null, 100,
                null, null, null, null, null, null, null, null);
        when(leagueRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seasonService.create(cmd))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_endDateBeforeStartDate_throwsBadRequest() {
        CreateSeasonCmd cmd = new CreateSeasonCmd(1L, "2026/2027",
                LocalDate.of(2026, 12, 20), LocalDate.of(2026, 9, 19), 100,
                null, null, null, null, null, null, null, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league(1L)));

        assertThatThrownBy(() -> seasonService.create(cmd))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_duplicateNameInSameLeague_throwsConflict() {
        CreateSeasonCmd cmd = new CreateSeasonCmd(1L, "2026/2027", null, null, 100,
                null, null, null, null, null, null, null, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league(1L)));
        when(seasonRepository.existsByLeague_LeagueIDAndNameIgnoreCase(1L, "2026/2027")).thenReturn(true);

        assertThatThrownBy(() -> seasonService.create(cmd))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(seasonRepository, never()).save(any(Season.class));
    }

    @Test
    void create_forfeitWinnerNotGreaterThanLoser_throwsBadRequest() {
        CreateSeasonCmd cmd = new CreateSeasonCmd(1L, "2026/2027", null, null, 100,
                null, null, null, 0, 11, null, null, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league(1L)));
        when(seasonRepository.existsByLeague_LeagueIDAndNameIgnoreCase(1L, "2026/2027")).thenReturn(false);
        when(seasonMapper.toEntity(cmd)).thenReturn(new Season());

        assertThatThrownBy(() -> seasonService.create(cmd))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_scoringInDraft_isApplied() {
        Season season = season(1L, SeasonStatus.DRAFT);
        UpdateSeasonCmd cmd = new UpdateSeasonCmd("2026/2027", null, null, 100,
                3, 1, 0, null, null, null, null, null);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(seasonRepository.existsByLeague_LeagueIDAndNameIgnoreCaseAndSeasonIDNot(5L, "2026/2027", 1L))
                .thenReturn(false);
        when(seasonMapper.toInfo(season)).thenReturn(info(1L, SeasonStatus.DRAFT));

        seasonService.update(1L, cmd);

        assertThat(season.getPointsForWin()).isEqualTo(3);
    }

    @Test
    void update_scoringInActiveSeason_throwsConflict() {
        Season season = season(1L, SeasonStatus.ACTIVE);
        UpdateSeasonCmd cmd = new UpdateSeasonCmd("2026/2027", null, null, 100,
                3, null, null, null, null, null, null, null);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(seasonRepository.existsByLeague_LeagueIDAndNameIgnoreCaseAndSeasonIDNot(5L, "2026/2027", 1L))
                .thenReturn(false);

        assertThatThrownBy(() -> seasonService.update(1L, cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("DRAFT")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(season.getPointsForWin()).isEqualTo(2);
    }

    @Test
    void update_activeSeasonWithUnchangedScoring_isAllowed() {
        Season season = season(1L, SeasonStatus.ACTIVE);
        UpdateSeasonCmd cmd = new UpdateSeasonCmd("2026/2027 - ispravljen naziv", null, null, 100,
                2, 1, 0, 11, 0, 11, 12, 8);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(seasonRepository.existsByLeague_LeagueIDAndNameIgnoreCaseAndSeasonIDNot(any(), anyString(), any()))
                .thenReturn(false);
        when(seasonMapper.toInfo(season)).thenReturn(info(1L, SeasonStatus.ACTIVE));

        seasonService.update(1L, cmd);

        verify(seasonMapper).updateEntityFromCmd(cmd, season);
    }

    @Test
    void update_unknownId_throwsResourceNotFoundException() {
        UpdateSeasonCmd cmd = new UpdateSeasonCmd("2026/2027", null, null, 100,
                null, null, null, null, null, null, null, null);
        when(seasonRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seasonService.update(404L, cmd))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStatus_draftToActive_isAllowed() {
        Season season = season(1L, SeasonStatus.DRAFT);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(seasonMapper.toInfo(season)).thenReturn(info(1L, SeasonStatus.ACTIVE));

        seasonService.changeStatus(1L, new ChangeSeasonStatusCmd(SeasonStatus.ACTIVE));

        assertThat(season.getStatus()).isEqualTo(SeasonStatus.ACTIVE);
    }

    @Test
    void changeStatus_activeBackToDraft_throwsConflict() {
        Season season = season(1L, SeasonStatus.ACTIVE);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));

        assertThatThrownBy(() -> seasonService.changeStatus(1L, new ChangeSeasonStatusCmd(SeasonStatus.DRAFT)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(season.getStatus()).isEqualTo(SeasonStatus.ACTIVE);
    }

    @Test
    void delete_seasonWithoutStages_removesSeason() {
        Season season = season(1L, SeasonStatus.DRAFT);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(stageRepository.existsBySeason_SeasonID(1L)).thenReturn(false);

        seasonService.delete(1L);

        verify(seasonRepository).delete(season);
    }

    @Test
    void delete_seasonWithStages_throwsConflict() {
        Season season = season(1L, SeasonStatus.ACTIVE);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(stageRepository.existsBySeason_SeasonID(1L)).thenReturn(true);

        assertThatThrownBy(() -> seasonService.delete(1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(seasonRepository, never()).delete(any(Season.class));
    }

    private static League league(Long id) {
        League league = new League();
        league.setLeagueID(id);
        league.setName("Floor General 1v1 Liga");
        return league;
    }

    private static Season season(Long id, SeasonStatus status) {
        Season season = new Season();
        season.setSeasonID(id);
        season.setName("2026/2027");
        season.setStatus(status);
        season.setPlayerCount(100);
        season.setLeague(league(5L));
        return season;
    }

    private static SeasonInfo info(Long id, SeasonStatus status) {
        return new SeasonInfo(id, 5L, "Floor General 1v1 Liga", "2026/2027", null, null, 100, status,
                2, 1, 0, 11, 0, 11, 12, 8);
    }
}
