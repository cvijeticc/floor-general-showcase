package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import rs.floorgeneral.domain.League;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.LeagueRepository;
import rs.floorgeneral.web.dto.league.CreateLeagueCmd;
import rs.floorgeneral.web.dto.league.LeagueInfo;
import rs.floorgeneral.web.dto.league.LeagueResult;
import rs.floorgeneral.web.dto.league.UpdateLeagueCmd;
import rs.floorgeneral.web.mapper.LeagueMapper;

@ExtendWith(MockitoExtension.class)
class LeagueServiceTest {

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private LeagueMapper leagueMapper;

    @InjectMocks
    private LeagueService leagueService;

    @Test
    void search_mapsEveryEntityToResult() {
        League league1 = leagueWithId(1L, "Floor General 1v1 Liga");
        League league2 = leagueWithId(2L, "Floor General 1v1 Liga - Rekreativna divizija");
        when(leagueRepository.findAll(any(Sort.class))).thenReturn(List.of(league1, league2));
        when(leagueMapper.toResult(league1)).thenReturn(new LeagueResult(1L, "Floor General 1v1 Liga"));
        when(leagueMapper.toResult(league2)).thenReturn(new LeagueResult(2L, "Floor General 1v1 Liga - Rekreativna divizija"));

        List<LeagueResult> results = leagueService.search();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo(1L);
        assertThat(results.get(0).name()).isEqualTo("Floor General 1v1 Liga");
    }

    @Test
    void findById_existingId_returnsInfo() {
        League league = leagueWithId(1L, "Floor General 1v1 Liga");
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMapper.toInfo(league)).thenReturn(new LeagueInfo(1L, "Floor General 1v1 Liga"));

        LeagueInfo info = leagueService.findById(1L);

        assertThat(info.id()).isEqualTo(1L);
        assertThat(info.name()).isEqualTo("Floor General 1v1 Liga");
    }

    @Test
    void findById_unknownId_throwsResourceNotFoundException() {
        when(leagueRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void create_savesMappedEntityAndReturnsInfo() {
        CreateLeagueCmd cmd = new CreateLeagueCmd("Nova Liga");
        League toSave = new League();
        toSave.setName("Nova Liga");
        League saved = leagueWithId(5L, "Nova Liga");

        when(leagueMapper.toEntity(cmd)).thenReturn(toSave);
        when(leagueRepository.save(toSave)).thenReturn(saved);
        when(leagueMapper.toInfo(saved)).thenReturn(new LeagueInfo(5L, "Nova Liga"));

        LeagueInfo info = leagueService.create(cmd);

        assertThat(info.id()).isEqualTo(5L);
        assertThat(info.name()).isEqualTo("Nova Liga");
        verify(leagueRepository).save(toSave);
    }

    @Test
    void update_existingId_appliesChangesAndReturnsInfo() {
        UpdateLeagueCmd cmd = new UpdateLeagueCmd("Izmenjen naziv");
        League league = leagueWithId(1L, "Stari naziv");
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMapper.toInfo(league)).thenReturn(new LeagueInfo(1L, "Izmenjen naziv"));

        LeagueInfo info = leagueService.update(1L, cmd);

        verify(leagueMapper).updateEntityFromCmd(cmd, league);
        assertThat(info.name()).isEqualTo("Izmenjen naziv");
    }

    @Test
    void update_unknownId_throwsResourceNotFoundException() {
        UpdateLeagueCmd cmd = new UpdateLeagueCmd("Bilo koji naziv");
        when(leagueRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.update(404L, cmd))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(leagueMapper, never()).updateEntityFromCmd(any(), any());
    }

    @Test
    void delete_existingId_deletesEntity() {
        League league = leagueWithId(1L, "Za brisanje");
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

        leagueService.delete(1L);

        verify(leagueRepository, times(1)).delete(league);
    }

    @Test
    void delete_unknownId_throwsResourceNotFoundExceptionAndNeverDeletes() {
        when(leagueRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(leagueRepository, never()).delete(any());
    }

    private League leagueWithId(Long id, String name) {
        League league = new League();
        league.setLeagueID(id);
        league.setName(name);
        return league;
    }
}
