package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.domain.GroupEntry;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.domain.Standing;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.StandingRepository;
import rs.floorgeneral.web.dto.standing.StandingResult;

@ExtendWith(MockitoExtension.class)
class StandingsServiceTest {

    @Mock
    private StandingRepository standingRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupEntryRepository groupEntryRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private StandingsService standingsService;

    @Test
    void recompute_writesOneRowPerPlayer() {
        Group group = group();
        List<Standing> saved = captureSavedStandings();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.findByGroup_GroupID(1L))
                .thenReturn(List.of(entry(group, player(10L)), entry(group, player(20L))));
        when(matchRepository.findByGroup_GroupID(1L))
                .thenReturn(List.of(finishedMatch(group, 10L, 20L, 11, 7, MatchStatus.FINISHED)));
        when(standingRepository.findByGroup_GroupID(1L)).thenReturn(new ArrayList<>());
        when(standingRepository.findByGroup_GroupIDOrderByPositionAsc(1L)).thenReturn(List.of());

        standingsService.recompute(1L);

        assertThat(saved).hasSize(2);
        Standing winner = saved.stream().filter(s -> s.getPlayer().getPlayerID() == 10L).findFirst().orElseThrow();
        assertThat(winner.getPosition()).isEqualTo(1);
        assertThat(winner.getPoints()).isEqualTo(2);
        assertThat(winner.getPointDifference()).isEqualTo(4);
        assertThat(winner.getPlayed()).isEqualTo(1);
    }

    /** BR-12.3 — drugi preracun ne pravi nove redove nego azurira postojece. */
    @Test
    void recompute_updatesExistingRowsInsteadOfCreatingNewOnes() {
        Group group = group();
        Standing existing = new Standing();
        existing.setStandingID(500L);
        existing.setGroup(group);
        existing.setPlayer(player(10L));
        List<Standing> saved = captureSavedStandings();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.findByGroup_GroupID(1L)).thenReturn(List.of(entry(group, player(10L))));
        when(matchRepository.findByGroup_GroupID(1L)).thenReturn(List.of());
        when(standingRepository.findByGroup_GroupID(1L)).thenReturn(new ArrayList<>(List.of(existing)));
        when(standingRepository.findByGroup_GroupIDOrderByPositionAsc(1L)).thenReturn(List.of(existing));

        standingsService.recompute(1L);

        assertThat(saved).containsExactly(existing);
        verify(standingRepository, never()).delete(any(Standing.class));
    }

    /** Igrac uklonjen iz grupe gubi svoj red — tabela prati sastav grupe. */
    @Test
    void recompute_removesRowsOfPlayersNoLongerInGroup() {
        Group group = group();
        Standing leftover = new Standing();
        leftover.setStandingID(600L);
        leftover.setGroup(group);
        leftover.setPlayer(player(99L));

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.findByGroup_GroupID(1L)).thenReturn(List.of(entry(group, player(10L))));
        when(matchRepository.findByGroup_GroupID(1L)).thenReturn(List.of());
        when(standingRepository.findByGroup_GroupID(1L)).thenReturn(new ArrayList<>(List.of(leftover)));
        when(standingRepository.findByGroup_GroupIDOrderByPositionAsc(1L)).thenReturn(List.of());

        standingsService.recompute(1L);

        verify(standingRepository).delete(leftover);
    }

    @Test
    void recompute_countsOnlyMatchesWithResult() {
        Group group = group();
        List<Standing> saved = captureSavedStandings();
        Match scheduled = finishedMatch(group, 10L, 20L, 0, 0, MatchStatus.SCHEDULED);
        scheduled.setScorePlayer1(null);
        scheduled.setScorePlayer2(null);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.findByGroup_GroupID(1L))
                .thenReturn(List.of(entry(group, player(10L)), entry(group, player(20L))));
        when(matchRepository.findByGroup_GroupID(1L)).thenReturn(List.of(scheduled));
        when(standingRepository.findByGroup_GroupID(1L)).thenReturn(new ArrayList<>());
        when(standingRepository.findByGroup_GroupIDOrderByPositionAsc(1L)).thenReturn(List.of());

        standingsService.recompute(1L);

        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(standing -> {
            assertThat(standing.getPlayed()).isZero();
            assertThat(standing.getPoints()).isZero();
        });
    }

    /** BR-10.2 — nedolazak nosi 0 bodova kaznjenom, a protivniku pobedu. */
    @Test
    void recompute_walkoverGivesForfeitPointsToLoser() {
        Group group = group();
        List<Standing> saved = captureSavedStandings();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupEntryRepository.findByGroup_GroupID(1L))
                .thenReturn(List.of(entry(group, player(10L)), entry(group, player(20L))));
        when(matchRepository.findByGroup_GroupID(1L))
                .thenReturn(List.of(finishedMatch(group, 10L, 20L, 11, 0, MatchStatus.WALKOVER)));
        when(standingRepository.findByGroup_GroupID(1L)).thenReturn(new ArrayList<>());
        when(standingRepository.findByGroup_GroupIDOrderByPositionAsc(1L)).thenReturn(List.of());

        standingsService.recompute(1L);

        Standing punished = saved.stream().filter(s -> s.getPlayer().getPlayerID() == 20L).findFirst().orElseThrow();
        assertThat(punished.getPoints()).isZero();
        assertThat(punished.getLosses()).isEqualTo(1);
    }

    @Test
    void findByGroup_readsMaterializedTable() {
        Group group = group();
        Standing standing = new Standing();
        standing.setGroup(group);
        standing.setPlayer(player(10L));
        standing.setPosition(1);
        standing.setPoints(4);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(standingRepository.findByGroup_GroupIDOrderByPositionAsc(1L)).thenReturn(List.of(standing));

        List<StandingResult> table = standingsService.findByGroup(1L);

        assertThat(table).hasSize(1);
        assertThat(table.get(0).playerName()).isEqualTo("Igrac 10");
        assertThat(table.get(0).points()).isEqualTo(4);
        verify(matchRepository, never()).findByGroup_GroupID(1L);
    }

    @Test
    void findByGroup_unknownGroup_throwsResourceNotFoundException() {
        when(groupRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> standingsService.findByGroup(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recomputeForMatch_skipsMatchWithoutGroup() {
        Match playoffMatch = new Match();

        standingsService.recomputeForMatch(playoffMatch);

        verify(groupRepository, never()).findById(any());
    }

    private List<Standing> captureSavedStandings() {
        List<Standing> saved = new ArrayList<>();
        when(standingRepository.save(any(Standing.class))).thenAnswer(invocation -> {
            saved.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        return saved;
    }

    private static Group group() {
        Season season = new Season();
        season.setSeasonID(1L);
        season.setName("2026/2027");
        season.setPointsForWin(2);
        season.setPointsForLoss(1);
        season.setPointsForForfeit(0);

        Stage stage = new Stage();
        stage.setStageID(3L);
        stage.setSeason(season);
        stage.setName("Grupna faza");
        stage.setType(StageType.GROUP);

        Group group = new Group();
        group.setGroupID(1L);
        group.setName("Grupa A");
        group.setStage(stage);
        return group;
    }

    private static GroupEntry entry(Group group, Player player) {
        GroupEntry entry = new GroupEntry();
        entry.setGroup(group);
        entry.setPlayer(player);
        return entry;
    }

    private static Player player(long id) {
        Player player = new Player();
        player.setPlayerID(id);
        player.setFirstName("Igrac");
        player.setLastName(String.valueOf(id));
        player.setStatus(PlayerStatus.ACTIVE);
        return player;
    }

    private static Match finishedMatch(Group group, long player1Id, long player2Id,
                                       int scorePlayer1, int scorePlayer2, MatchStatus status) {
        Match match = new Match();
        match.setMatchID(100L + player1Id);
        match.setGroup(group);
        match.setStage(group.getStage());
        match.setRoundNumber(1);
        match.setPlayer1(player(player1Id));
        match.setPlayer2(player(player2Id));
        match.setScorePlayer1(scorePlayer1);
        match.setScorePlayer2(scorePlayer2);
        match.setStatus(status);
        return match;
    }
}
