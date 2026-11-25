package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageStatus;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.web.dto.match.BulkResultEntry;
import rs.floorgeneral.web.dto.match.BulkResultsCmd;
import rs.floorgeneral.web.dto.match.BulkResultsInfo;
import rs.floorgeneral.web.dto.match.CorrectResultCmd;
import rs.floorgeneral.web.dto.match.CorrectionInfo;
import rs.floorgeneral.web.dto.match.EnterResultCmd;
import rs.floorgeneral.web.dto.match.ForfeitCmd;
import rs.floorgeneral.web.dto.match.ForfeitInfo;
import rs.floorgeneral.web.dto.match.ForfeitType;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.match.MatchResult;
import rs.floorgeneral.web.mapper.MatchMapper;

@ExtendWith(MockitoExtension.class)
class MatchResultServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private RuleProperties ruleProperties;

    @Mock
    private StandingsService standingsService;

    @Mock
    private UndoService undoService;

    @InjectMocks
    private MatchResultService matchResultService;

    @BeforeEach
    void stubUndo() {
        UndoTestSupport.stubUndo(undoService);
    }

    @Test
    void enterResult_setsScoresWinnerAndStatus() {
        Match match = scheduledMatch(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info());

        matchResultService.enterResult(1L, new EnterResultCmd(7, 11));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(match.getScorePlayer1()).isEqualTo(7);
        assertThat(match.getWinner().getPlayerID()).isEqualTo(20L);
    }

    @Test
    void enterResult_onMatchThatAlreadyHasResult_throwsConflict() {
        Match match = scheduledMatch(1L);
        match.setStatus(MatchStatus.FINISHED);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchResultService.enterResult(1L, new EnterResultCmd(11, 5)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("korekciju")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void enterResult_draw_throwsBadRequest() {
        Match match = scheduledMatch(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchResultService.enterResult(1L, new EnterResultCmd(10, 10)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
    }

    @Test
    void enterResult_unknownMatch_throwsResourceNotFoundException() {
        when(matchRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchResultService.enterResult(404L, new EnterResultCmd(11, 5)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void enterResults_appliesEveryValidRow() {
        Match first = scheduledMatch(1L);
        Match second = scheduledMatch(2L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(first));
        when(matchRepository.findById(2L)).thenReturn(Optional.of(second));
        when(matchMapper.toResult(any(Match.class))).thenReturn(result());

        BulkResultsInfo info = matchResultService.enterResults(new BulkResultsCmd(List.of(
                new BulkResultEntry(1L, 11, 4),
                new BulkResultEntry(2L, 6, 11))));

        assertThat(info.appliedCount()).isEqualTo(2);
        assertThat(info.rejectedCount()).isZero();
        assertThat(first.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(second.getWinner().getPlayerID()).isEqualTo(20L);
    }

    /** Glavno pravilo masovnog unosa: jedan los red ne obara ostale. */
    @Test
    void enterResults_badRowIsRejectedButOthersAreApplied() {
        Match first = scheduledMatch(1L);
        Match second = scheduledMatch(2L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(first));
        when(matchRepository.findById(2L)).thenReturn(Optional.of(second));
        when(matchMapper.toResult(any(Match.class))).thenReturn(result());

        BulkResultsInfo info = matchResultService.enterResults(new BulkResultsCmd(List.of(
                new BulkResultEntry(1L, 9, 9),
                new BulkResultEntry(2L, 11, 3))));

        assertThat(info.appliedCount()).isEqualTo(1);
        assertThat(info.rejectedCount()).isEqualTo(1);
        assertThat(info.rejected().get(0).matchId()).isEqualTo(1L);
        assertThat(info.rejected().get(0).reason()).contains("neresen");
        assertThat(first.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(second.getStatus()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void enterResults_unknownMatchIsRejectedWithReason() {
        when(matchRepository.findById(404L)).thenReturn(Optional.empty());

        BulkResultsInfo info = matchResultService.enterResults(new BulkResultsCmd(List.of(
                new BulkResultEntry(404L, 11, 3))));

        assertThat(info.appliedCount()).isZero();
        assertThat(info.rejected().get(0).reason()).contains("ne postoji");
    }

    @Test
    void enterResults_matchWithExistingResultIsRejected() {
        Match match = scheduledMatch(1L);
        match.setStatus(MatchStatus.WALKOVER);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        BulkResultsInfo info = matchResultService.enterResults(new BulkResultsCmd(List.of(
                new BulkResultEntry(1L, 11, 3))));

        assertThat(info.rejectedCount()).isEqualTo(1);
        assertThat(info.rejected().get(0).reason()).contains("vec ima rezultat");
    }

    /** Gornja granica je parametar sezone — u sezoni sa maksimumom 21 prolazi i 21:19. */
    @Test
    void enterResult_respectsSeasonMaxScore() {
        Match match = scheduledMatch(1L);
        match.getStage().getSeason().setTargetScore(21);
        match.getStage().getSeason().setMaxScore(21);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info());

        matchResultService.enterResult(1L, new EnterResultCmd(21, 19));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void applyForfeit_walkover_writesScoresFromSeasonParameters() {
        Match match = scheduledMatch(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(ruleProperties.walkoverWarningThresholdOrDefault()).thenReturn(2);
        when(matchRepository.countWalkoversAgainstPlayer(3L, 10L)).thenReturn(1L);
        when(matchMapper.toInfo(match)).thenReturn(info());

        ForfeitInfo result = matchResultService.applyForfeit(1L,
                new ForfeitCmd(10L, ForfeitType.WALKOVER, "Nije se pojavio"));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.WALKOVER);
        assertThat(match.getScorePlayer1()).isZero();
        assertThat(match.getScorePlayer2()).isEqualTo(11);
        assertThat(match.getWinner().getPlayerID()).isEqualTo(20L);
        assertThat(match.getNote()).isEqualTo("Nije se pojavio");
        assertThat(result.warning()).isNull();
    }

    /** Rezultat se cita iz sezone — sezona do 21 daje 21:0, ne 11:0. */
    @Test
    void applyForfeit_usesChangedSeasonParameters() {
        Match match = scheduledMatch(1L);
        match.getStage().getSeason().setForfeitWinnerScore(21);
        match.getStage().getSeason().setForfeitLoserScore(0);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(ruleProperties.walkoverWarningThresholdOrDefault()).thenReturn(2);
        when(matchRepository.countWalkoversAgainstPlayer(3L, 20L)).thenReturn(1L);
        when(matchMapper.toInfo(match)).thenReturn(info());

        matchResultService.applyForfeit(1L, new ForfeitCmd(20L, ForfeitType.WALKOVER, "Nije dosao"));

        assertThat(match.getScorePlayer1()).isEqualTo(21);
        assertThat(match.getScorePlayer2()).isZero();
        assertThat(match.getWinner().getPlayerID()).isEqualTo(10L);
    }

    @Test
    void applyForfeit_surrender_setsForfeitStatusAndNoWarning() {
        Match match = scheduledMatch(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info());

        ForfeitInfo result = matchResultService.applyForfeit(1L,
                new ForfeitCmd(10L, ForfeitType.FORFEIT, "Predao zbog povrede"));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.FORFEIT);
        assertThat(result.warning()).isNull();
    }

    /** BR-10.4 — na pragu nedolazaka API vraca upozorenje, ali igraca ne iskljucuje. */
    @Test
    void applyForfeit_atWalkoverThreshold_returnsWarning() {
        Match match = scheduledMatch(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(ruleProperties.walkoverWarningThresholdOrDefault()).thenReturn(2);
        when(matchRepository.countWalkoversAgainstPlayer(3L, 10L)).thenReturn(2L);
        when(matchMapper.toInfo(match)).thenReturn(info());

        ForfeitInfo result = matchResultService.applyForfeit(1L,
                new ForfeitCmd(10L, ForfeitType.WALKOVER, "Opet nije dosao"));

        assertThat(result.warning()).contains("iskljucenje");
        assertThat(match.getPlayer1().getStatus()).isEqualTo(PlayerStatus.ACTIVE);
    }

    @Test
    void applyForfeit_playerOutsideMatch_throwsBadRequest() {
        Match match = scheduledMatch(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchResultService.applyForfeit(1L,
                new ForfeitCmd(99L, ForfeitType.WALKOVER, "Nije dosao")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /** D-23 — obojica kaznjena: 0:0 iz parametara sezone, bez pobednika, bez upozorenja. */
    @Test
    void applyForfeit_doubleForfeit_punishesBothWithoutWinner() {
        Match match = scheduledMatch(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info());

        ForfeitInfo result = matchResultService.applyForfeit(1L,
                new ForfeitCmd(null, ForfeitType.DOUBLE_FORFEIT, "Nijedan nije dosao"));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.DOUBLE_FORFEIT);
        assertThat(match.getScorePlayer1()).isZero();
        assertThat(match.getScorePlayer2()).isZero();
        assertThat(match.getWinner()).isNull();
        assertThat(match.getNote()).isEqualTo("Nijedan nije dosao");
        assertThat(result.warning()).isNull();
    }

    /** Liga naknadno odluci da su kaznjena obojica — sluzbeni rezultat menja sluzbeni rezultat. */
    @Test
    void applyForfeit_doubleForfeit_replacesEarlierWalkover() {
        Match match = scheduledMatch(1L);
        match.setScorePlayer1(11);
        match.setScorePlayer2(0);
        match.setWinner(match.getPlayer1());
        match.setStatus(MatchStatus.WALKOVER);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info());

        matchResultService.applyForfeit(1L, new ForfeitCmd(null, ForfeitType.DOUBLE_FORFEIT, "Odluka lige"));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.DOUBLE_FORFEIT);
        assertThat(match.getScorePlayer1()).isZero();
        assertThat(match.getWinner()).isNull();
    }

    @Test
    void applyForfeit_singleForfeitWithoutPlayer_throwsBadRequest() {
        Match match = scheduledMatch(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchResultService.applyForfeit(1L,
                new ForfeitCmd(null, ForfeitType.WALKOVER, "Nije dosao")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void applyForfeit_onMatchThatAlreadyHasResult_throwsConflict() {
        Match match = scheduledMatch(1L);
        match.setStatus(MatchStatus.FINISHED);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchResultService.applyForfeit(1L,
                new ForfeitCmd(10L, ForfeitType.WALKOVER, "Nije dosao")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void correctResult_changesScoreAndWinnerAndKeepsReasonInNote() {
        Match match = scheduledMatch(1L);
        MatchResultWriterFixture.finish(match, 11, 7);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info());

        CorrectionInfo result = matchResultService.correctResult(1L,
                new CorrectResultCmd(7, 11, "Zapisnicar je zamenio igrace"));

        assertThat(match.getScorePlayer1()).isEqualTo(7);
        assertThat(match.getWinner().getPlayerID()).isEqualTo(20L);
        assertThat(match.getNote()).contains("Korekcija", "11:7", "7:11", "Zapisnicar");
        assertThat(result.previousResult()).contains("11:7");
        assertThat(result.warning()).isNull();
    }

    @Test
    void correctResult_onMatchWithoutResult_throwsConflict() {
        Match match = scheduledMatch(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchResultService.correctResult(1L,
                new CorrectResultCmd(11, 7, "Greska")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("nema upisan rezultat")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void correctResult_withInvalidScore_throwsBadRequest() {
        Match match = scheduledMatch(1L);
        MatchResultWriterFixture.finish(match, 11, 7);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchResultService.correctResult(1L,
                new CorrectResultCmd(9, 9, "Greska")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(match.getScorePlayer1()).isEqualTo(11);
    }

    /** AC-8 — korekcija u zatvorenoj fazi menja plasman, pa odgovor nosi upozorenje. */
    @Test
    void correctResult_inFinishedStage_returnsWarning() {
        Match match = scheduledMatch(1L);
        MatchResultWriterFixture.finish(match, 11, 7);
        match.getStage().setStatus(StageStatus.FINISHED);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info());

        CorrectionInfo result = matchResultService.correctResult(1L,
                new CorrectResultCmd(7, 11, "Prigovor usvojen"));

        assertThat(result.warning()).contains("plasman");
    }

    @Test
    void correctResult_correctsForfeitIntoNormalResult() {
        Match match = scheduledMatch(1L);
        match.setScorePlayer1(0);
        match.setScorePlayer2(11);
        match.setWinner(match.getPlayer2());
        match.setStatus(MatchStatus.WALKOVER);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchMapper.toInfo(match)).thenReturn(info());

        matchResultService.correctResult(1L, new CorrectResultCmd(11, 9, "Igrac je ipak dosao"));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(match.getWinner().getPlayerID()).isEqualTo(10L);
    }

    @Test
    void correctResult_unknownMatch_throwsResourceNotFoundException() {
        when(matchRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchResultService.correctResult(404L,
                new CorrectResultCmd(11, 7, "Greska")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** Mali pomocnik: postavlja mec u stanje „odigran" bez prolaska kroz servis. */
    private static final class MatchResultWriterFixture {
        static void finish(Match match, int scorePlayer1, int scorePlayer2) {
            match.setScorePlayer1(scorePlayer1);
            match.setScorePlayer2(scorePlayer2);
            match.setWinner(scorePlayer1 > scorePlayer2 ? match.getPlayer1() : match.getPlayer2());
            match.setStatus(MatchStatus.FINISHED);
        }
    }

    private static Match scheduledMatch(Long id) {
        Season season = new Season();
        season.setSeasonID(1L);
        season.setName("2026/2027");
        season.setTargetScore(11);
        season.setMaxScore(12);
        season.setForfeitWinnerScore(11);
        season.setForfeitLoserScore(0);

        Stage stage = new Stage();
        stage.setStageID(3L);
        stage.setSeason(season);
        stage.setName("Grupna faza");
        stage.setType(StageType.GROUP);
        stage.setStatus(StageStatus.ACTIVE);

        Group group = new Group();
        group.setGroupID(1L);
        group.setName("Grupa A");
        group.setStage(stage);

        Match match = new Match();
        match.setMatchID(id);
        match.setStage(stage);
        match.setGroup(group);
        match.setRoundNumber(1);
        match.setPlayer1(player(10L));
        match.setPlayer2(player(20L));
        match.setStatus(MatchStatus.SCHEDULED);
        return match;
    }

    private static Player player(Long id) {
        Player player = new Player();
        player.setPlayerID(id);
        player.setFirstName("Igrac");
        player.setLastName(String.valueOf(id));
        player.setStatus(PlayerStatus.ACTIVE);
        return player;
    }

    private static MatchInfo info() {
        return new MatchInfo(1L, 3L, "Grupna faza", 1L, "Grupa A", 1,
                10L, "Igrac 10", 20L, "Igrac 20", 7, 11, 20L, "Igrac 20",
                MatchStatus.FINISHED, null, null, null, null);
    }

    private static MatchResult result() {
        return new MatchResult(1L, 1L, "Grupa A", 1, 10L, "Igrac 10", 20L, "Igrac 20",
                7, 11, MatchStatus.FINISHED, null, null);
    }
}
