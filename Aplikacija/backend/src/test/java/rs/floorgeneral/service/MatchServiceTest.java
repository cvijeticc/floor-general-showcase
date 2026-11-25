package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import rs.floorgeneral.domain.Court;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.domain.GroupEntry;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.CourtRepository;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.PlayerRepository;
import rs.floorgeneral.web.dto.match.CreateMatchCmd;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.match.PostponeMatchCmd;
import rs.floorgeneral.web.dto.match.UpdateMatchCmd;
import rs.floorgeneral.web.mapper.MatchMapper;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    private static final LocalDateTime PAST_SLOT = LocalDateTime.of(2026, 9, 19, 19, 0);

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupEntryRepository groupEntryRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private StandingsService standingsService;

    @Mock
    private UndoService undoService;

    @InjectMocks
    private MatchService matchService;

    @BeforeEach
    void stubUndo() {
        UndoTestSupport.stubUndo(undoService);
    }

    @Test
    void create_scheduledMatch_hasNoResultAndKeepsPastDate() {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 3, 10L, 20L, null, PAST_SLOT, null, null, null);
        stubGroupWithPlayers();
        when(matchRepository.findAtSlotForPlayer(any(), anyLong())).thenReturn(List.of());
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchMapper.toInfo(any(Match.class))).thenReturn(info(MatchStatus.SCHEDULED));

        matchService.create(cmd);

        Match saved = capturedSavedMatch();
        assertThat(saved.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(saved.getScheduledStart()).isEqualTo(PAST_SLOT);
        assertThat(saved.getScorePlayer1()).isNull();
        assertThat(saved.getWinner()).isNull();
        assertThat(saved.getStage()).isNotNull();
    }

    /** Odluka D-07 — 60 vec odigranih meceva ulazi ovako, u jednom pozivu. */
    @Test
    void create_withResult_finishesMatchAndDerivesWinner() {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 3, 10L, 20L, null, PAST_SLOT, 11, 7, null);
        stubGroupWithPlayers();
        when(matchRepository.findAtSlotForPlayer(any(), anyLong())).thenReturn(List.of());
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchMapper.toInfo(any(Match.class))).thenReturn(info(MatchStatus.FINISHED));

        matchService.create(cmd);

        Match saved = capturedSavedMatch();
        assertThat(saved.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(saved.getScorePlayer1()).isEqualTo(11);
        assertThat(saved.getWinner().getPlayerID()).isEqualTo(10L);
    }

    /** BR-10 — igrac koji je odustao ne dobija nov termin. */
    @Test
    void create_withdrawnPlayerWithoutResult_throwsConflict() {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 7, 10L, 20L, null, null, null, null, null);
        Group group = group();
        Player withdrawn = player(20L);
        withdrawn.setStatus(PlayerStatus.WITHDRAWN);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player(10L)));
        when(playerRepository.findById(20L)).thenReturn(Optional.of(withdrawn));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 10L))
                .thenReturn(Optional.of(entry(group, player(10L))));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 20L))
                .thenReturn(Optional.of(entry(group, withdrawn)));

        assertThatThrownBy(() -> matchService.create(cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("odustao")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(matchRepository, never()).save(any(Match.class));
    }

    /** D-07 — mec odigran pre odustanka se i dalje sme uneti retroaktivno, sa rezultatom. */
    @Test
    void create_withdrawnPlayerWithResult_isAccepted() {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 2, 10L, 20L, null, PAST_SLOT, 11, 4, null);
        Group group = group();
        Player withdrawn = player(20L);
        withdrawn.setStatus(PlayerStatus.WITHDRAWN);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player(10L)));
        when(playerRepository.findById(20L)).thenReturn(Optional.of(withdrawn));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 10L))
                .thenReturn(Optional.of(entry(group, player(10L))));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 20L))
                .thenReturn(Optional.of(entry(group, withdrawn)));
        when(matchRepository.findAtSlotForPlayer(any(), anyLong())).thenReturn(List.of());
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchMapper.toInfo(any(Match.class))).thenReturn(info(MatchStatus.FINISHED));

        matchService.create(cmd);

        assertThat(capturedSavedMatch().getStatus()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void create_samePlayerTwice_throwsBadRequest() {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 1, 10L, 10L, null, null, null, null, null);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group()));

        assertThatThrownBy(() -> matchService.create(cmd))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_playerOutsideGroup_throwsConflict() {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 1, 10L, 20L, null, null, null, null, null);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group()));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player(10L)));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.create(cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("nije u grupi")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void create_playerAlreadyPlayingAtThatSlot_throwsConflict() {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 1, 10L, 20L, null, PAST_SLOT, null, null, null);
        stubGroupWithPlayers();
        Match existing = new Match();
        existing.setMatchID(99L);
        existing.setRoundNumber(2);
        when(matchRepository.findAtSlotForPlayer(PAST_SLOT, 10L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> matchService.create(cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("99")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void create_courtAlreadyTakenAtThatSlot_throwsConflict() {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 1, 10L, 20L, 5L, PAST_SLOT, null, null, null);
        stubGroupWithPlayers();
        when(courtRepository.findById(5L)).thenReturn(Optional.of(court()));
        when(matchRepository.findAtSlotForPlayer(any(), anyLong())).thenReturn(List.of());
        Match existing = new Match();
        existing.setMatchID(77L);
        when(matchRepository.findAtSlotOnCourt(PAST_SLOT, 5L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> matchService.create(cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("zauzet")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void create_withDraw_throwsBadRequest() {
        assertResultRejected(10, 10, "neresen");
    }

    @Test
    void create_withScoreAboveMaxScore_throwsBadRequest() {
        assertResultRejected(13, 9, "ne moze biti veci");
    }

    /** D-18 — mec traje osam minuta, pa je i 9:7 ispravan rezultat. */
    @Test
    void create_whenNobodyReachedTargetScore_isAccepted() {
        stubGroupWithPlayers();
        when(matchRepository.save(any(Match.class))).thenAnswer(call -> call.getArgument(0));
        when(matchMapper.toInfo(any(Match.class))).thenReturn(info(MatchStatus.FINISHED));

        MatchInfo created = matchService.create(new CreateMatchCmd(1L, 1, 10L, 20L, null, null, 9, 7, null));

        assertThat(created).isNotNull();
    }

    @Test
    void create_withOnlyOneScore_throwsBadRequest() {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 1, 10L, 20L, null, null, 11, null, null);
        stubGroupWithPlayers();

        assertThatThrownBy(() -> matchService.create(cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("oba rezultata")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_unknownGroup_throwsResourceNotFoundException() {
        CreateMatchCmd cmd = new CreateMatchCmd(404L, 1, 10L, 20L, null, null, null, null, null);
        when(groupRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.create(cmd))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_changesSlotAndIgnoresItselfInConflictCheck() {
        Match match = scheduledMatch(50L);
        UpdateMatchCmd cmd = new UpdateMatchCmd(4, null, PAST_SLOT, "Pomeren termin");
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(matchRepository.findAtSlotForPlayer(PAST_SLOT, 10L)).thenReturn(List.of(match));
        when(matchRepository.findAtSlotForPlayer(PAST_SLOT, 20L)).thenReturn(List.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info(MatchStatus.SCHEDULED));

        matchService.update(50L, cmd);

        assertThat(match.getRoundNumber()).isEqualTo(4);
        assertThat(match.getScheduledStart()).isEqualTo(PAST_SLOT);
        assertThat(match.getNote()).isEqualTo("Pomeren termin");
    }

    @Test
    void postpone_setsStatusAndNote() {
        Match match = scheduledMatch(50L);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info(MatchStatus.POSTPONED));

        matchService.postpone(50L, new PostponeMatchCmd(PAST_SLOT.plusDays(7), "Kisa"));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.POSTPONED);
        assertThat(match.getNote()).isEqualTo("Kisa");
        assertThat(match.getScheduledStart()).isEqualTo(PAST_SLOT.plusDays(7));
    }

    @Test
    void postpone_finishedMatch_throwsConflict() {
        Match match = scheduledMatch(50L);
        match.setStatus(MatchStatus.FINISHED);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.postpone(50L, new PostponeMatchCmd(PAST_SLOT, "Kasno")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void delete_scheduledMatch_removesIt() {
        Match match = scheduledMatch(50L);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));

        matchService.delete(50L);

        verify(matchRepository).delete(match);
    }

    @Test
    void delete_finishedMatch_throwsConflict() {
        Match match = scheduledMatch(50L);
        match.setStatus(MatchStatus.WALKOVER);
        when(matchRepository.findById(50L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.delete(50L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(matchRepository, never()).delete(any(Match.class));
    }

    @Test
    void findById_unknownId_throwsResourceNotFoundException() {
        when(matchRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void assertResultRejected(int scorePlayer1, int scorePlayer2, String expectedMessagePart) {
        CreateMatchCmd cmd = new CreateMatchCmd(1L, 1, 10L, 20L, null, PAST_SLOT, scorePlayer1, scorePlayer2, null);
        stubGroupWithPlayers();
        when(matchRepository.findAtSlotForPlayer(any(), anyLong())).thenReturn(List.of());

        assertThatThrownBy(() -> matchService.create(cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(expectedMessagePart)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(matchRepository, never()).save(any(Match.class));
    }

    private Match capturedSavedMatch() {
        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(captor.capture());
        return captor.getValue();
    }

    private void stubGroupWithPlayers() {
        Group group = group();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player(10L)));
        when(playerRepository.findById(20L)).thenReturn(Optional.of(player(20L)));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 10L))
                .thenReturn(Optional.of(entry(group, player(10L))));
        when(groupEntryRepository.findByGroup_GroupIDAndPlayer_PlayerID(1L, 20L))
                .thenReturn(Optional.of(entry(group, player(20L))));
    }

    private static GroupEntry entry(Group group, Player player) {
        GroupEntry entry = new GroupEntry();
        entry.setGroup(group);
        entry.setPlayer(player);
        return entry;
    }

    private static Group group() {
        Season season = new Season();
        season.setSeasonID(1L);
        season.setName("2026/2027");
        season.setTargetScore(11);
        season.setMaxScore(12);

        Stage stage = new Stage();
        stage.setStageID(3L);
        stage.setSeason(season);
        stage.setName("Grupna faza");
        stage.setType(StageType.GROUP);
        stage.setGroupSize(10);

        Group group = new Group();
        group.setGroupID(1L);
        group.setName("Grupa A");
        group.setStage(stage);
        return group;
    }

    private static Player player(Long id) {
        Player player = new Player();
        player.setPlayerID(id);
        player.setFirstName("Igrac");
        player.setLastName(String.valueOf(id));
        player.setStatus(PlayerStatus.ACTIVE);
        return player;
    }

    private static Court court() {
        Court court = new Court();
        court.setCourtID(5L);
        court.setName("SC Šumice");
        return court;
    }

    private static Match scheduledMatch(Long id) {
        Match match = new Match();
        match.setMatchID(id);
        match.setGroup(group());
        match.setStage(group().getStage());
        match.setRoundNumber(1);
        match.setPlayer1(player(10L));
        match.setPlayer2(player(20L));
        match.setStatus(MatchStatus.SCHEDULED);
        return match;
    }

    private static MatchInfo info(MatchStatus status) {
        return new MatchInfo(50L, 3L, "Grupna faza", 1L, "Grupa A", 1,
                10L, "Igrac 10", 20L, "Igrac 20", null, null, null, null,
                status, null, null, PAST_SLOT, null);
    }
}
