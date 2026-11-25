package rs.floorgeneral.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.StageStatus;
import rs.floorgeneral.domain.UndoAction;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.web.dto.match.BulkResultEntry;
import rs.floorgeneral.web.dto.match.CorrectResultCmd;
import rs.floorgeneral.web.dto.match.CorrectionInfo;
import rs.floorgeneral.web.dto.match.BulkResultsCmd;
import rs.floorgeneral.web.dto.match.BulkResultsInfo;
import rs.floorgeneral.web.dto.match.EnterResultCmd;
import rs.floorgeneral.web.dto.match.ForfeitCmd;
import rs.floorgeneral.web.dto.match.ForfeitInfo;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.match.MatchResult;
import rs.floorgeneral.web.dto.match.RejectedResult;
import rs.floorgeneral.web.mapper.MatchMapper;

/**
 * UC-ADM-09 — unos rezultata. Pored pojedinacnog unosa nosi i masovni: celo kolo u jednom pozivu,
 * jer se vikendom odigra ~60 meceva koje uvece jedna osoba unese odjednom.
 * <p>
 * <b>Jedan los red ne obara ceo unos.</b> Svaki red se proverava zasebno i, ako padne, zavrsi u
 * listi odbijenih sa razlogom; ostali se upisu. Zato se izuzeci reda hvataju ovde, unutar iste
 * transakcije, i nikad ne izlaze iz metode.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MatchResultService {

    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;
    private final RuleProperties ruleProperties;
    private final StandingsService standingsService;
    private final UndoService undoService;

    @Transactional
    public MatchInfo enterResult(Long matchId, EnterResultCmd cmd) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Mec sa ID " + matchId + " ne postoji"));

        UndoDraft undo = undoService.draft(UndoAction.RESULT_ENTER,
                        "Unos rezultata " + cmd.scorePlayer1() + ":" + cmd.scorePlayer2()
                                + " (" + MatchService.describe(match) + ")")
                .changed(UndoTables.MATCHES, UndoTables.MATCH_ID, matchId)
                .recompute(groupIdOf(match));

        applyToMatch(match, cmd.scorePlayer1(), cmd.scorePlayer2());
        standingsService.recomputeForMatch(match);
        undo.save();

        return matchMapper.toInfo(match);
    }

    /**
     * Masovni unos celog kola. Tabela se (kad UC-ENG-01 bude gotov) preracunava jednom, na kraju,
     * a ne posle svakog reda.
     */
    @Transactional
    public BulkResultsInfo enterResults(BulkResultsCmd cmd) {
        List<MatchResult> applied = new ArrayList<>();
        List<RejectedResult> rejected = new ArrayList<>();
        Set<Match> touchedMatches = new LinkedHashSet<>();

        // Ceo unos je JEDAN zapis u steku: jedno „Ponisti" vraca celo kolo, ne mec po mec.
        UndoDraft undo = undoService.draft(UndoAction.RESULT_BULK, "Masovni unos rezultata");

        for (BulkResultEntry entry : cmd.results()) {
            Optional<Match> found = matchRepository.findById(entry.matchId());
            if (found.isEmpty()) {
                rejected.add(new RejectedResult(entry.matchId(), "Mec sa ID " + entry.matchId() + " ne postoji"));
                continue;
            }

            Match match = found.get();
            // Snimak se uzima pre upisa, a zadrzava se tek ako je red prosao.
            RowSnapshot before = undoService.capture(RowSnapshot.RowOp.CHANGED,
                    UndoTables.MATCHES, UndoTables.MATCH_ID, entry.matchId());
            try {
                applyToMatch(match, entry.scorePlayer1(), entry.scorePlayer2());
                applied.add(matchMapper.toResult(match));
                touchedMatches.add(match);
                undo.changed(before).recompute(groupIdOf(match));
            } catch (ApiException ex) {
                rejected.add(new RejectedResult(entry.matchId(), ex.getMessage()));
            }
        }

        // Tabela se preracunava jednom, na kraju, i to samo za grupe kojih se unos tice.
        touchedMatches.stream()
                .filter(match -> match.getGroup() != null)
                .map(match -> match.getGroup().getGroupID())
                .distinct()
                .forEach(standingsService::recompute);

        undo.describe("Masovni unos rezultata: " + matchCount(applied.size())).save();

        log.info("Masovni unos rezultata: upisano {}, odbijeno {}", applied.size(), rejected.size());

        return new BulkResultsInfo(applied.size(), rejected.size(), applied, rejected);
    }

    /**
     * UC-ADM-11 — korekcija vec upisanog rezultata, uz obavezan razlog (BR-12, FR-3.2).
     * <p>
     * <b>Privremeno resenje:</b> audit tabele jos nema (UC-ADM-20 je van MVP-a), pa se stari
     * rezultat i razlog upisuju u {@code matches.note} i u log na nivou INFO. Kad audit tabela
     * stigne, ovo se menja — zapis mora da nosi i ko je menjao i kada.
     */
    @Transactional
    public CorrectionInfo correctResult(Long matchId, CorrectResultCmd cmd) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Mec sa ID " + matchId + " ne postoji"));

        MatchResultWriter.rejectSeriesGame(match);
        if (!match.getStatus().hasResult()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Mec br. " + matchId + " nema upisan rezultat — koristite unos rezultata, ne korekciju");
        }

        String previousResult = match.getScorePlayer1() + ":" + match.getScorePlayer2()
                + " (" + match.getStatus() + ")";

        UndoDraft undo = undoService.draft(UndoAction.RESULT_CORRECT,
                        "Korekcija rezultata meca br. " + matchId + ": " + previousResult
                                + " -> " + cmd.scorePlayer1() + ":" + cmd.scorePlayer2())
                .changed(UndoTables.MATCHES, UndoTables.MATCH_ID, matchId)
                .recompute(groupIdOf(match));

        MatchResultWriter.applyOrThrow(match, cmd.scorePlayer1(), cmd.scorePlayer2());
        match.setNote("Korekcija " + previousResult + " -> " + cmd.scorePlayer1() + ":" + cmd.scorePlayer2()
                + ". Razlog: " + cmd.reason());

        standingsService.recomputeForMatch(match);
        undo.save();

        log.info("Korekcija rezultata meca {}: {} -> {}:{}. Razlog: {}",
                matchId, previousResult, cmd.scorePlayer1(), cmd.scorePlayer2(), cmd.reason());

        return new CorrectionInfo(matchMapper.toInfo(match), previousResult, closedStageWarning(match));
    }

    /** AC-8 — kad je faza vec zatvorena, korekcija menja i plasman; admin to mora da vidi. */
    private static String closedStageWarning(Match match) {
        if (match.getStage().getStatus() != StageStatus.FINISHED) {
            return null;
        }
        return "Faza " + match.getStage().getName()
                + " je zatvorena — korekcija menja plasman i listu onih koji prolaze dalje";
    }

    /**
     * UC-ADM-10 — sluzbeni rezultat zbog nedolaska (WALKOVER) ili predaje (FORFEIT).
     * Rezultat se NE unosi rucno nego se cita iz parametara sezone: protivnik dobija
     * {@code forfeitWinnerScore} (11), kaznjeni {@code forfeitLoserScore} (0) — BR-10.2.
     * Bodove kaznjenog ({@code pointsForForfeit}) racuna preracun tabele, UC-ENG-01.
     * <p>
     * <b>Obostrani sluzbeni rezultat (DOUBLE_FORFEIT, D-23):</b> obojica su predala ili se nijedan
     * nije pojavio. Oba dobijaju {@code forfeitLoserScore} (0:0), poraz i {@code pointsForForfeit},
     * a mec nema pobednika — liga ide dalje bez izmisljenog pobednika.
     * <p>
     * Sluzbeni rezultat sme da zameni drugi sluzbeni rezultat (liga naknadno odluci da su obojica
     * kaznjena). Odigran mec (FINISHED) se ne dira ovde — ispravka ide kroz korekciju.
     */
    @Transactional
    public ForfeitInfo applyForfeit(Long matchId, ForfeitCmd cmd) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Mec sa ID " + matchId + " ne postoji"));
        MatchResultWriter.rejectSeriesGame(match);

        if (match.getStatus().hasResult() && !match.getStatus().isOfficial()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Mec br. " + matchId + " vec ima rezultat — ispravka ide kroz korekciju rezultata");
        }

        UndoDraft undo = undoService.draft(UndoAction.RESULT_FORFEIT,
                        "Sluzbeni rezultat na mecu br. " + matchId + ": " + MatchService.describe(match))
                .changed(UndoTables.MATCHES, UndoTables.MATCH_ID, matchId)
                .recompute(groupIdOf(match));

        Season season = match.getStage().getSeason();
        if (cmd.type().punishesBoth()) {
            match.setScorePlayer1(season.getForfeitLoserScore());
            match.setScorePlayer2(season.getForfeitLoserScore());
            match.setWinner(null);
            match.setStatus(cmd.type().toMatchStatus());
            match.setNote(cmd.note());

            standingsService.recomputeForMatch(match);
            undo.save();

            log.info("Obostrani sluzbeni rezultat na mecu {}: {}, razlog: {}",
                    matchId, MatchService.describe(match), cmd.note());
            return new ForfeitInfo(matchMapper.toInfo(match), null);
        }

        if (cmd.losingPlayerId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Izaberite igraca koji nije dosao ili je predao");
        }
        Player loser = participantOrThrow(match, cmd.losingPlayerId());
        Player winner = loser.getPlayerID().equals(match.getPlayer1().getPlayerID())
                ? match.getPlayer2()
                : match.getPlayer1();

        boolean loserIsFirst = loser.getPlayerID().equals(match.getPlayer1().getPlayerID());
        match.setScorePlayer1(loserIsFirst ? season.getForfeitLoserScore() : season.getForfeitWinnerScore());
        match.setScorePlayer2(loserIsFirst ? season.getForfeitWinnerScore() : season.getForfeitLoserScore());
        match.setWinner(winner);
        match.setStatus(cmd.type().toMatchStatus());
        match.setNote(cmd.note());

        standingsService.recomputeForMatch(match);
        undo.save();

        log.info("Sluzbeni rezultat na mecu {}: {} ({}), razlog: {}",
                matchId, loser.getFullName(), cmd.type(), cmd.note());

        return new ForfeitInfo(matchMapper.toInfo(match), walkoverWarning(match, loser));
    }

    /** „1 mec" / „3 meca" / „60 meceva" — opis stoji na dugmetu, pa mnozina mora biti tacna. */
    private static String matchCount(int count) {
        int lastTwo = count % 100;
        int last = count % 10;
        if (lastTwo >= 11 && lastTwo <= 14) {
            return count + " meceva";
        }
        return count + (last == 1 ? " mec" : last >= 2 && last <= 4 ? " meca" : " meceva");
    }

    /** Playoff mec nema grupu (od V3), pa ni tabelu koja bi se preracunavala. */
    private static Long groupIdOf(Match match) {
        return match.getGroup() == null ? null : match.getGroup().getGroupID();
    }

    /** BR-10.4 — sistem samo PREDLAZE iskljucenje; odluku donosi admin. */
    private String walkoverWarning(Match match, Player loser) {
        if (match.getStatus() != MatchStatus.WALKOVER) {
            return null;
        }
        int threshold = ruleProperties.walkoverWarningThresholdOrDefault();
        long walkovers = matchRepository.countWalkoversAgainstPlayer(
                match.getStage().getStageID(), loser.getPlayerID());

        if (walkovers < threshold) {
            return null;
        }
        return "Igrac " + loser.getFullName() + " ima " + walkovers
                + " nedolazaka u ovoj fazi — razmotrite iskljucenje (BR-10.4)";
    }

    private static Player participantOrThrow(Match match, Long playerId) {
        if (playerId.equals(match.getPlayer1().getPlayerID())) {
            return match.getPlayer1();
        }
        if (playerId.equals(match.getPlayer2().getPlayerID())) {
            return match.getPlayer2();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Igrac sa ID " + playerId + " ne igra taj mec");
    }

    /** Rezultat se upisuje samo na mec koji ga jos nema — ispravka ide kroz UC-ADM-11. */
    private void applyToMatch(Match match, int scorePlayer1, int scorePlayer2) {
        MatchResultWriter.rejectSeriesGame(match);
        if (match.getStatus().hasResult()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Mec br. " + match.getMatchID() + " vec ima rezultat — ispravka ide kroz korekciju rezultata");
        }
        MatchResultWriter.applyOrThrow(match, scorePlayer1, scorePlayer2);
    }
}
