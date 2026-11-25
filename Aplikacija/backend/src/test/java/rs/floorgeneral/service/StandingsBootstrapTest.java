package rs.floorgeneral.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.repository.GroupRepository;

@ExtendWith(MockitoExtension.class)
class StandingsBootstrapTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private StandingsService standingsService;

    @InjectMocks
    private StandingsBootstrap bootstrap;

    /** I grupa koja vec ima tabelu se preracunava — seed migracija joj je mozda dodala meceve. */
    @Test
    void run_recomputesEveryGroup() {
        when(groupRepository.findAll()).thenReturn(List.of(group(1L), group(2L), group(3L)));

        bootstrap.run(null);

        verify(standingsService).recompute(1L);
        verify(standingsService).recompute(2L);
        verify(standingsService).recompute(3L);
    }

    private static Group group(Long id) {
        Group group = new Group();
        group.setGroupID(id);
        group.setName("Grupa " + id);
        return group;
    }
}
