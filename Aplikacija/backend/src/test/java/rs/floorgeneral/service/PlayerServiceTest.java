package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.PlayerStatus;
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

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private GroupEntryRepository groupEntryRepository;

    @Mock
    private PlayerMapper playerMapper;

    @Mock
    private UndoService undoService;

    @InjectMocks
    private PlayerService playerService;

    @BeforeEach
    void stubUndo() {
        UndoTestSupport.stubUndo(undoService);
    }

    @Test
    void search_mapsEveryEntityToResult() {
        Player player = playerWithId(1L, "Marko", "Marković", PlayerStatus.ACTIVE);
        when(playerRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(player));
        when(playerMapper.toResult(player))
                .thenReturn(new PlayerResult(1L, "Marko", "Marković", "Mare", "Marko Marković",
                        "Marko Marković Mare", 1995, null, null, null, PlayerStatus.ACTIVE));

        List<PlayerResult> results = playerService.search(null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).fullName()).isEqualTo("Marko Marković");
        assertThat(results.get(0).status()).isEqualTo(PlayerStatus.ACTIVE);
    }

    @Test
    void search_sortsByLastNameThenFirstName() {
        when(playerRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        playerService.search("mark", 7L, PlayerStatus.ACTIVE);

        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(playerRepository).findAll(any(Specification.class), sort.capture());
        assertThat(sort.getValue()).isEqualTo(Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
    }

    @Test
    void findById_existingId_returnsInfo() {
        Player player = playerWithId(1L, "Petar", "Petrović", PlayerStatus.ACTIVE);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerMapper.toInfo(player)).thenReturn(info(1L, "Petar", "Petrović", PlayerStatus.ACTIVE));

        PlayerInfo result = playerService.findById(1L);

        assertThat(result.fullName()).isEqualTo("Petar Petrović");
    }

    @Test
    void findById_unknownId_throwsResourceNotFoundException() {
        when(playerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void create_alwaysSavesPlayerAsActive() {
        CreatePlayerCmd cmd = new CreatePlayerCmd("Jovan", "Jovanović", "Joca", 2000, null, null, null, null, null);
        Player mapped = new Player();
        mapped.setFirstName("Jovan");
        mapped.setLastName("Jovanović");
        when(playerMapper.toEntity(cmd)).thenReturn(mapped);
        when(playerRepository.save(mapped)).thenReturn(mapped);
        when(playerMapper.toInfo(mapped)).thenReturn(info(5L, "Jovan", "Jovanović", PlayerStatus.ACTIVE));

        PlayerInfo result = playerService.create(cmd);

        ArgumentCaptor<Player> saved = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PlayerStatus.ACTIVE);
        assertThat(result.id()).isEqualTo(5L);
    }

    @Test
    void update_existingId_appliesChangesToManagedEntity() {
        Player player = playerWithId(1L, "Marko", "Marković", PlayerStatus.ACTIVE);
        UpdatePlayerCmd cmd = new UpdatePlayerCmd("Marko", "Petrović", "Mare", 1995, "+381601111111", 190, 85, "6500din", null);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerMapper.toInfo(player)).thenReturn(info(1L, "Marko", "Petrović", PlayerStatus.ACTIVE));

        PlayerInfo result = playerService.update(1L, cmd);

        verify(playerMapper).updateEntityFromCmd(cmd, player);
        verify(playerRepository, never()).save(any(Player.class));
        assertThat(result.lastName()).isEqualTo("Petrović");
    }

    @Test
    void update_unknownId_throwsResourceNotFoundException() {
        UpdatePlayerCmd cmd = new UpdatePlayerCmd("Marko", "Marković", "Mare", null, null, null, null, null, null);
        when(playerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.update(404L, cmd))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** BR-10 — odustali igrac vise ne igra: neodigrani mecevi se otkazuju, odigrani ostaju. */
    @Test
    void changeStatus_withdrawn_cancelsOnlyUnplayedMatches() {
        Player player = playerWithId(1L, "Nikola", "Nikolić", PlayerStatus.ACTIVE);
        Match scheduled = match(7L, MatchStatus.SCHEDULED);
        Match postponed = match(8L, MatchStatus.POSTPONED);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(matchRepository.findForPlayerInStatuses(eq(1L), anyCollection()))
                .thenReturn(List.of(scheduled, postponed));
        when(playerMapper.toInfo(player)).thenReturn(info(1L, "Nikola", "Nikolić", PlayerStatus.WITHDRAWN));

        PlayerInfo result = playerService.changeStatus(1L,
                new ChangePlayerStatusCmd(PlayerStatus.WITHDRAWN, "Povreda kolena"));

        assertThat(player.getStatus()).isEqualTo(PlayerStatus.WITHDRAWN);
        assertThat(result.status()).isEqualTo(PlayerStatus.WITHDRAWN);
        assertThat(List.of(scheduled, postponed))
                .allSatisfy(match -> {
                    assertThat(match.getStatus()).isEqualTo(MatchStatus.CANCELLED);
                    assertThat(match.getNote()).isEqualTo("Otkazan: Nikola Nikolić odustao — Povreda kolena");
                });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MatchStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(matchRepository).findForPlayerInStatuses(eq(1L), statuses.capture());
        assertThat(statuses.getValue())
                .doesNotContain(MatchStatus.FINISHED, MatchStatus.FORFEIT, MatchStatus.WALKOVER, MatchStatus.CANCELLED);
    }

    @Test
    void changeStatus_backToActive_leavesMatchesUntouched() {
        Player player = playerWithId(1L, "Nikola", "Nikolić", PlayerStatus.WITHDRAWN);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerMapper.toInfo(player)).thenReturn(info(1L, "Nikola", "Nikolić", PlayerStatus.ACTIVE));

        playerService.changeStatus(1L, new ChangePlayerStatusCmd(PlayerStatus.ACTIVE, null));

        assertThat(player.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
        verify(matchRepository, never()).findForPlayerInStatuses(anyLong(), anyCollection());
    }

    @Test
    void changeStatus_unknownId_throwsResourceNotFoundException() {
        when(playerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.changeStatus(404L,
                new ChangePlayerStatusCmd(PlayerStatus.DISQUALIFIED, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_playerWithoutMatchesAndGroups_removesPlayer() {
        Player player = playerWithId(1L, "Nikola", "Nikolić", PlayerStatus.WITHDRAWN);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(matchRepository.existsByPlayer1_PlayerIDOrPlayer2_PlayerID(1L, 1L)).thenReturn(false);
        when(groupEntryRepository.existsByPlayer_PlayerID(1L)).thenReturn(false);

        playerService.delete(1L);

        verify(playerRepository).delete(player);
    }

    @Test
    void delete_playerWithMatches_throwsConflict() {
        Player player = playerWithId(1L, "Marko", "Marković", PlayerStatus.ACTIVE);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(matchRepository.existsByPlayer1_PlayerIDOrPlayer2_PlayerID(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> playerService.delete(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("meceve")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(playerRepository, never()).delete(any(Player.class));
    }

    @Test
    void delete_playerAssignedToGroup_throwsConflict() {
        Player player = playerWithId(1L, "Jovan", "Jovanović", PlayerStatus.ACTIVE);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(matchRepository.existsByPlayer1_PlayerIDOrPlayer2_PlayerID(1L, 1L)).thenReturn(false);
        when(groupEntryRepository.existsByPlayer_PlayerID(1L)).thenReturn(true);

        assertThatThrownBy(() -> playerService.delete(1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(playerRepository, never()).delete(any(Player.class));
    }

    @Test
    void delete_unknownId_throwsResourceNotFoundException() {
        when(playerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Match match(Long id, MatchStatus status) {
        Match match = new Match();
        match.setMatchID(id);
        match.setStatus(status);
        return match;
    }

    private static Player playerWithId(Long id, String firstName, String lastName, PlayerStatus status) {
        Player player = new Player();
        player.setPlayerID(id);
        player.setFirstName(firstName);
        player.setLastName(lastName);
        player.setStatus(status);
        return player;
    }

    private static PlayerInfo info(Long id, String firstName, String lastName, PlayerStatus status) {
        String fullName = firstName + " " + lastName;
        return new PlayerInfo(id, firstName, lastName, "Nadimak", fullName, fullName + " Nadimak",
                null, null, null, null, null, null, status);
    }
}
