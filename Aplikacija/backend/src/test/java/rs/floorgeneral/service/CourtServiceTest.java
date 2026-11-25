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
import rs.floorgeneral.domain.Court;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.CourtRepository;
import rs.floorgeneral.web.dto.court.CourtInfo;
import rs.floorgeneral.web.dto.court.CourtResult;
import rs.floorgeneral.web.dto.court.CreateCourtCmd;
import rs.floorgeneral.web.dto.court.UpdateCourtCmd;
import rs.floorgeneral.web.mapper.CourtMapper;

@ExtendWith(MockitoExtension.class)
class CourtServiceTest {

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private CourtMapper courtMapper;

    @InjectMocks
    private CourtService courtService;

    @Test
    void search_mapsEveryEntityToResult() {
        Court court = courtWithId(1L, "Hala Pinki", "Trg Nikole Pašića 1, Zemun");
        when(courtRepository.findAll(any(Sort.class))).thenReturn(List.of(court));
        when(courtMapper.toResult(court)).thenReturn(new CourtResult(1L, "Hala Pinki", "Trg Nikole Pašića 1, Zemun"));

        List<CourtResult> results = courtService.search();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(1L);
        assertThat(results.get(0).name()).isEqualTo("Hala Pinki");
        assertThat(results.get(0).address()).isEqualTo("Trg Nikole Pašića 1, Zemun");
    }

    @Test
    void findById_existingId_returnsInfo() {
        Court court = courtWithId(1L, "SC Šumice", "Ustanička 125, Beograd");
        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));
        when(courtMapper.toInfo(court)).thenReturn(new CourtInfo(1L, "SC Šumice", "Ustanička 125, Beograd"));

        CourtInfo info = courtService.findById(1L);

        assertThat(info.name()).isEqualTo("SC Šumice");
        assertThat(info.address()).isEqualTo("Ustanička 125, Beograd");
    }

    @Test
    void findById_unknownId_throwsResourceNotFoundException() {
        when(courtRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void create_savesMappedEntityAndReturnsInfo() {
        CreateCourtCmd cmd = new CreateCourtCmd("Teren Kalemegdan", "Kalemegdan bb");
        Court toSave = new Court();
        toSave.setName("Teren Kalemegdan");
        toSave.setAddress("Kalemegdan bb");
        Court saved = courtWithId(7L, "Teren Kalemegdan", "Kalemegdan bb");

        when(courtMapper.toEntity(cmd)).thenReturn(toSave);
        when(courtRepository.save(toSave)).thenReturn(saved);
        when(courtMapper.toInfo(saved)).thenReturn(new CourtInfo(7L, "Teren Kalemegdan", "Kalemegdan bb"));

        CourtInfo info = courtService.create(cmd);

        assertThat(info.id()).isEqualTo(7L);
        assertThat(info.name()).isEqualTo("Teren Kalemegdan");
        verify(courtRepository).save(toSave);
    }

    @Test
    void update_existingId_appliesChangesAndReturnsInfo() {
        UpdateCourtCmd cmd = new UpdateCourtCmd("SC Šumice", "Nova adresa");
        Court court = courtWithId(1L, "SC Šumice", "Stara adresa");
        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));
        when(courtMapper.toInfo(court)).thenReturn(new CourtInfo(1L, "SC Šumice", "Nova adresa"));

        CourtInfo info = courtService.update(1L, cmd);

        verify(courtMapper).updateEntityFromCmd(cmd, court);
        assertThat(info.address()).isEqualTo("Nova adresa");
    }

    @Test
    void update_unknownId_throwsResourceNotFoundException() {
        UpdateCourtCmd cmd = new UpdateCourtCmd("Bilo koji naziv", "Bilo koja adresa");
        when(courtRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtService.update(404L, cmd))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(courtMapper, never()).updateEntityFromCmd(any(), any());
    }

    @Test
    void delete_existingId_deletesEntity() {
        Court court = courtWithId(1L, "Za brisanje", "Adresa");
        when(courtRepository.findById(1L)).thenReturn(Optional.of(court));

        courtService.delete(1L);

        verify(courtRepository, times(1)).delete(court);
    }

    @Test
    void delete_unknownId_throwsResourceNotFoundExceptionAndNeverDeletes() {
        when(courtRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtService.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(courtRepository, never()).delete(any());
    }

    private Court courtWithId(Long id, String name, String address) {
        Court court = new Court();
        court.setCourtID(id);
        court.setName(name);
        court.setAddress(address);
        return court;
    }
}
