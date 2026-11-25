package rs.floorgeneral.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Court;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.Series;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageRound;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.domain.UndoAction;
import rs.floorgeneral.engine.BracketMath;
import rs.floorgeneral.engine.MatchResultRules;
import rs.floorgeneral.engine.SeriesRules;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.CourtRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.PlayerRepository;
import rs.floorgeneral.repository.SeriesRepository;
import rs.floorgeneral.repository.StageRepository;
import rs.floorgeneral.repository.StageRoundRepository;
import rs.floorgeneral.web.dto.playoff.AddSeriesGameCmd;
import rs.floorgeneral.web.dto.playoff.BracketInfo;
import rs.floorgeneral.web.dto.playoff.CorrectSeriesGameCmd;
import rs.floorgeneral.web.dto.playoff.CreateBracketCmd;
import rs.floorgeneral.web.dto.playoff.GameInfo;
import rs.floorgeneral.web.dto.playoff.RoundInfo;
import rs.floorgeneral.web.dto.playoff.ScheduleSeriesCmd;
import rs.floorgeneral.web.dto.playoff.SeriesInfo;
import rs.floorgeneral.web.dto.playoff.SeriesWalkoverCmd;
import rs.floorgeneral.web.dto.playoff.SetSeriesPlayersCmd;
import rs.floorgeneral.web.dto.playoff.UpdateStageRoundCmd;

/**
 * Rok 3 — playoff (BR-08, BR-09, UC-PLA): drvo, serije i unos meceva serije.
 * <p>
 * <b>Kako tece:</b> admin unese rezultat sledeceg meca serije; servis izracuna stanje serije
 * ({@link SeriesRules}) i, cim neko dostigne potreban broj pobeda kola, upise pobednika i postavi
 * ga u seriju sledeceg kola ({@link BracketMath}). Porazeni iz polufinala ide u mec za 3. mesto.
 * Isto se desava unazad: ispravka ili brisanje meca koji menja pobednika menja i igraca u sledecem
 * kolu — ali samo dok ta serija nije pocela; posle toga se prvo brisu njeni mecevi (409).
 * <p>
 * Svaka izmena je jedan zapis „Ponisti" (D-17): pamte se dirnute serije i mecevi, pa Ctrl+Z vraca
 * i drvo, ne samo rezultat. Tabele grupa playoff ne dira (mecevi serije nemaju grupu).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlayoffService {

    static final String PENDING = "PENDING";
    static final String READY = "READY";
    static final String IN_PROGRESS = "IN_PROGRESS";
    static final String FINISHED = "FINISHED";

    private final StageRepository stageRepository;
    private final StageRoundRepository stageRoundRepository;
    private final SeriesRepository seriesRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final CourtRepository courtRepository;
    private final UndoService undoService;

    /* ------------------------------------------------------------------ citanje */

    public BracketInfo bracket(Long stageId) {
        Stage stage = getKnockoutStageOrThrow(stageId);
        return toBracket(stage);
    }

    /** Serije faze sa mecevima, grupisane po kolu — zajednicko za admin i javno drvo. */
    Map<Long, List<Match>> gamesBySeries(Long stageId) {
        Map<Long, List<Match>> games = new HashMap<>();
        for (Match match : matchRepository.findSeriesGamesOfStage(stageId)) {
            games.computeIfAbsent(match.getSeries().getSeriesID(), id -> new ArrayList<>()).add(match);
        }
        return games;
    }

    /* ------------------------------------------------------------------ drvo */

    /**
     * Pravi kola i sve serije za KNOCKOUT fazu prema {@code bracketSize} (sledece sezone; za
     * 2026/2027 drvo je ubacila migracija V18). Kola dobijaju podrazumevane nazive i 1 potrebnu
     * pobedu — admin ih posle podesava (BR-08: broj pobeda je po kolu konfigurabilan).
     */
    @Transactional
    public BracketInfo createBracket(Long stageId, CreateBracketCmd cmd) {
        Stage stage = getKnockoutStageOrThrow(stageId);
        Integer size = stage.getBracketSize();
        if (size == null || !BracketMath.isValidSize(size)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Velicina drveta faze mora biti stepen dvojke (2, 4, 8 … 64), a ne " + size);
        }
        if (stageRoundRepository.existsByStage_StageID(stageId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Drvo faze „" + stage.getName() + "\" vec postoji");
        }
        List<SetSeriesPlayersCmd> pairs = cmd == null || cmd.firstRound() == null ? List.of() : cmd.firstRound();
        int firstRoundSeries = BracketMath.seriesInRound(size, 1);
        if (!pairs.isEmpty() && pairs.size() != firstRoundSeries) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Prvo kolo ima " + firstRoundSeries + " parova, a poslato je " + pairs.size());
        }

        UndoDraft undo = undoService.draft(UndoAction.PLAYOFF_BRACKET, "Pravljenje drveta: " + stage.getName());

        int rounds = BracketMath.roundCount(size);
        List<Long> usedInFirstRound = new ArrayList<>();
        for (int roundNo = 1; roundNo <= rounds; roundNo++) {
            StageRound round = new StageRound();
            round.setStage(stage);
            round.setRoundNo(roundNo);
            round.setName(BracketMath.defaultRoundName(size, roundNo));
            round.setWinsRequired(1);
            stageRoundRepository.saveAndFlush(round);
            undo.created(UndoTables.STAGE_ROUNDS, UndoTables.STAGE_ROUND_ID, round.getStageRoundID());

            for (int position = 1; position <= BracketMath.seriesInRound(size, roundNo); position++) {
                Series series = new Series();
                series.setRound(round);
                series.setPosition(position);
                if (roundNo == 1 && !pairs.isEmpty()) {
                    SetSeriesPlayersCmd pair = pairs.get(position - 1);
                    series.setPlayer1(findPlayerOrNull(pair.player1Id()));
                    series.setPlayer2(findPlayerOrNull(pair.player2Id()));
                    series.setSeed1(blankToNull(pair.seed1()));
                    series.setSeed2(blankToNull(pair.seed2()));
                    requireDistinct(series.getPlayer1(), series.getPlayer2());
                    for (Player player : new Player[] {series.getPlayer1(), series.getPlayer2()}) {
                        if (player != null) {
                            if (usedInFirstRound.contains(player.getPlayerID())) {
                                throw new ApiException(HttpStatus.BAD_REQUEST,
                                        player.getDisplayName() + " je u prvom kolu vise od jednom");
                            }
                            usedInFirstRound.add(player.getPlayerID());
                        }
                    }
                }
                seriesRepository.saveAndFlush(series);
                undo.created(UndoTables.SERIES, UndoTables.SERIES_ID, series.getSeriesID());
            }
            if (roundNo == rounds && rounds >= 2) {
                Series thirdPlace = new Series();
                thirdPlace.setRound(round);
                thirdPlace.setPosition(1);
                thirdPlace.setThirdPlace(true);
                seriesRepository.saveAndFlush(thirdPlace);
                undo.created(UndoTables.SERIES, UndoTables.SERIES_ID, thirdPlace.getSeriesID());
            }
        }
        undo.save();

        log.info("Napravljeno drvo faze {} ({} igraca, {} kola)", stage.getName(), size, rounds);
        return toBracket(stage);
    }

    /**
     * Naziv i format kola. Broj potrebnih pobeda se ne menja dok kolo ima odigrane meceve —
     * promena bi naknadno menjala vec resene serije.
     */
    @Transactional
    public BracketInfo updateRound(Long roundId, UpdateStageRoundCmd cmd) {
        StageRound round = stageRoundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Kolo sa ID " + roundId + " ne postoji"));

        if (cmd.winsRequired() != round.getWinsRequired()) {
            boolean played = seriesRepository.findByRound_StageRoundID(roundId).stream()
                    .anyMatch(series -> !games(series).isEmpty() || series.isWalkover());
            if (played) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Kolo „" + round.getName() + "\" vec ima odigrane meceve — broj pobeda se vise ne menja");
            }
        }
        if (cmd.targetScore() != null && cmd.maxScore() != null && cmd.maxScore() < cmd.targetScore()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Najveci rezultat ne moze biti manji od granice poena");
        }

        undoService.draft(UndoAction.PLAYOFF_ROUND, "Izmena kola „" + round.getName() + "\"")
                .changed(UndoTables.STAGE_ROUNDS, UndoTables.STAGE_ROUND_ID, roundId)
                .save();

        round.setName(cmd.name().trim());
        round.setWinsRequired(cmd.winsRequired());
        round.setTargetScore(cmd.targetScore());
        round.setMaxScore(cmd.maxScore());
        round.setDurationMinutes(cmd.durationMinutes());
        stageRoundRepository.save(round);

        return toBracket(round.getStage());
    }

    /* ------------------------------------------------------------------ serija */

    /** Rezultat sledeceg meca serije (BR-08). Kad serija bude resena, pobednik ide dalje u drvetu. */
    @Transactional
    public SeriesInfo addGame(Long seriesId, AddSeriesGameCmd cmd) {
        Series series = getSeriesOrThrow(seriesId);
        StageRound round = series.getRound();

        if (!series.hasBothPlayers()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Serija " + label(series) + " jos nema oba igraca — ceka se pobednik iz prethodnog kola");
        }
        if (series.isWalkover()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Serija " + label(series) + " je resena sluzbeno — za nov mec prvo ponistite sluzbeni rezultat");
        }
        List<Match> games = games(series);
        if (SeriesRules.canAddGame(round.getWinsRequired(), toGames(series, games)).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Serija " + label(series) + " je vec resena (" + score(series, games) + ") — novi mec se ne igra");
        }
        validateScore(round, cmd.score1(), cmd.score2());
        List<SeriesRules.Game> after = new ArrayList<>(toGames(series, games));
        after.add(new SeriesRules.Game(cmd.score1(), cmd.score2()));
        ensureCanPropagate(series, winnerOf(series, after));

        UndoDraft undo = undoService.draft(UndoAction.PLAYOFF_GAME,
                "Playoff " + label(series) + ", " + (games.size() + 1) + ". mec " + cmd.score1() + ":" + cmd.score2()
                        + " (" + series.getPlayer1().getNickname() + " - " + series.getPlayer2().getNickname() + ")");
        captureBracketAround(undo, series);

        Match match = new Match();
        match.setStage(round.getStage());
        match.setGroup(null);
        match.setRoundNumber(round.getRoundNo());
        match.setSeries(series);
        match.setGameNumber(games.size() + 1);
        match.setPlayer1(series.getPlayer1());
        match.setPlayer2(series.getPlayer2());
        match.setCourt(cmd.courtId() != null ? findCourt(cmd.courtId()) : series.getCourt());
        match.setScheduledStart(cmd.scheduledStart() != null ? cmd.scheduledStart() : series.getScheduledStart());
        applyScore(match, cmd.score1(), cmd.score2());
        matchRepository.saveAndFlush(match);

        List<Match> updated = new ArrayList<>(games);
        updated.add(match);
        settle(series, updated);

        undo.created(UndoTables.MATCHES, UndoTables.MATCH_ID, match.getMatchID()).save();
        return toSeriesInfo(series, updated);
    }

    /** Ispravka rezultata meca serije; ako se time menja pobednik, menja se i igrac u sledecem kolu. */
    @Transactional
    public SeriesInfo correctGame(Long seriesId, Long matchId, CorrectSeriesGameCmd cmd) {
        Series series = getSeriesOrThrow(seriesId);
        List<Match> games = games(series);
        Match match = games.stream()
                .filter(game -> game.getMatchID().equals(matchId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mec sa ID " + matchId + " ne pripada seriji " + label(series)));
        validateScore(series.getRound(), cmd.score1(), cmd.score2());
        List<SeriesRules.Game> after = games.stream()
                .map(game -> game == match ? new SeriesRules.Game(cmd.score1(), cmd.score2()) : toGame(series, game))
                .toList();
        if (!SeriesRules.isConsistent(series.getRound().getWinsRequired(), after)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Posle ove ispravke serija je resena pre poslednjeg meca — prvo obrisite visak meca");
        }
        if (!series.isWalkover()) {
            ensureCanPropagate(series, winnerOf(series, after));
        }

        UndoDraft undo = undoService.draft(UndoAction.PLAYOFF_GAME,
                "Ispravka: playoff " + label(series) + ", " + match.getGameNumber() + ". mec "
                        + match.getScorePlayer1() + ":" + match.getScorePlayer2() + " -> " + cmd.score1() + ":" + cmd.score2());
        captureBracketAround(undo, series);
        undo.changed(UndoTables.MATCHES, UndoTables.MATCH_ID, matchId);

        String previous = match.getScorePlayer1() + ":" + match.getScorePlayer2();
        applyScore(match, cmd.score1(), cmd.score2());
        String reason = blankToNull(cmd.reason());
        match.setNote(trimNote("Ispravljeno sa " + previous + (reason == null ? "" : " — " + reason)));
        settle(series, games);
        undo.save();
        return toSeriesInfo(series, games);
    }

    /** Brise se samo poslednji mec serije — inace bi se redni brojevi meceva pomerili. */
    @Transactional
    public void deleteGame(Long seriesId, Long matchId) {
        Series series = getSeriesOrThrow(seriesId);
        List<Match> games = games(series);
        if (games.isEmpty() || !games.get(games.size() - 1).getMatchID().equals(matchId)) {
            boolean belongs = games.stream().anyMatch(game -> game.getMatchID().equals(matchId));
            if (!belongs) {
                throw new ResourceNotFoundException("Mec sa ID " + matchId + " ne pripada seriji " + label(series));
            }
            throw new ApiException(HttpStatus.CONFLICT, "Brise se samo poslednji mec serije — ostale ispravite");
        }
        Match match = games.get(games.size() - 1);
        List<Match> remaining = games.subList(0, games.size() - 1);
        if (!series.isWalkover()) {
            ensureCanPropagate(series, winnerOf(series, toGames(series, remaining)));
        }

        UndoDraft undo = undoService.draft(UndoAction.PLAYOFF_GAME,
                "Brisanje: playoff " + label(series) + ", " + match.getGameNumber() + ". mec "
                        + match.getScorePlayer1() + ":" + match.getScorePlayer2());
        undo.changed(UndoTables.MATCHES, UndoTables.MATCH_ID, matchId);
        captureBracketAround(undo, series);

        matchRepository.delete(match);
        matchRepository.flush();
        // Sluzbeni rezultat (walkover) ostaje kakav je — settle ga ne dira.
        settle(series, remaining);
        undo.save();
    }

    /**
     * Serija resena bez (svih) meceva — igrac se nije pojavio ili je predao (BR-08, BR-10).
     * Vec odigrani mecevi ostaju; pobednik ide dalje u drvetu.
     */
    @Transactional
    public SeriesInfo walkover(Long seriesId, SeriesWalkoverCmd cmd) {
        Series series = getSeriesOrThrow(seriesId);
        if (!series.hasBothPlayers()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Serija " + label(series) + " jos nema oba igraca — sluzbeni rezultat ceka protivnika");
        }
        Player winner = participantOrThrow(series, cmd.winnerId());
        List<Match> games = games(series);
        if (SeriesRules.evaluate(series.getRound().getWinsRequired(), toGames(series, games)).decided()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Serija " + label(series) + " je vec resena na terenu (" + score(series, games) + ")");
        }

        ensureCanPropagate(series, winner);

        UndoDraft undo = undoService.draft(UndoAction.PLAYOFF_WALKOVER,
                "Sluzbeni rezultat: playoff " + label(series) + ", dalje ide " + winner.getNickname());
        captureBracketAround(undo, series);

        series.setWalkover(true);
        String reason = blankToNull(cmd.reason());
        series.setNote(trimNote(reason == null ? "Sluzbeni rezultat — dalje ide " + winner.getNickname() : reason));
        setWinnerAndPropagate(series, winner);
        undo.save();
        return toSeriesInfo(series, games);
    }

    /** Ponistava sluzbeni rezultat: serija se vraca na stanje iz meceva (ako sledece kolo nije pocelo). */
    @Transactional
    public SeriesInfo reopen(Long seriesId) {
        Series series = getSeriesOrThrow(seriesId);
        if (!series.isWalkover()) {
            throw new ApiException(HttpStatus.CONFLICT, "Serija " + label(series) + " nema sluzbeni rezultat");
        }
        List<Match> games = games(series);
        ensureCanPropagate(series, winnerOf(series, toGames(series, games)));

        UndoDraft undo = undoService.draft(UndoAction.PLAYOFF_WALKOVER,
                "Ponisten sluzbeni rezultat: playoff " + label(series));
        captureBracketAround(undo, series);

        series.setWalkover(false);
        series.setNote(null);
        settle(series, games);
        undo.save();
        return toSeriesInfo(series, games);
    }

    /**
     * Rucno postavljanje para — kostur prvog kola ili zamena igraca koji je odustao. Dozvoljeno dok
     * serija nema nijedan mec ni pobednika.
     */
    @Transactional
    public SeriesInfo setPlayers(Long seriesId, SetSeriesPlayersCmd cmd) {
        Series series = getSeriesOrThrow(seriesId);
        if (!games(series).isEmpty() || series.getWinner() != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Serija " + label(series) + " je vec pocela — par se vise ne menja");
        }
        Player first = findPlayerOrNull(cmd.player1Id());
        Player second = findPlayerOrNull(cmd.player2Id());
        requireDistinct(first, second);
        for (Player player : new Player[] {first, second}) {
            if (player != null) {
                requireNotElsewhereInRound(series, player);
            }
        }

        undoService.draft(UndoAction.PLAYOFF_PLAYERS, "Par u seriji " + label(series) + ": "
                        + nickname(first) + " - " + nickname(second))
                .changed(UndoTables.SERIES, UndoTables.SERIES_ID, seriesId)
                .save();

        series.setPlayer1(first);
        series.setPlayer2(second);
        series.setSeed1(blankToNull(cmd.seed1()));
        series.setSeed2(blankToNull(cmd.seed2()));
        seriesRepository.save(series);
        return toSeriesInfo(series, List.of());
    }

    /** Termin i teren serije; mecevi bez sopstvenog termina ga nasledjuju pri unosu. */
    @Transactional
    public SeriesInfo schedule(Long seriesId, ScheduleSeriesCmd cmd) {
        Series series = getSeriesOrThrow(seriesId);
        Court court = findCourt(cmd.courtId());

        undoService.draft(UndoAction.PLAYOFF_SCHEDULE, "Termin serije " + label(series))
                .changed(UndoTables.SERIES, UndoTables.SERIES_ID, seriesId)
                .save();

        series.setScheduledStart(cmd.scheduledStart());
        series.setCourt(court);
        seriesRepository.save(series);
        return toSeriesInfo(series, games(series));
    }

    /* ------------------------------------------------------------------ pravila */

    /** Stanje serije iz meceva; pobednik (ili njegovo brisanje) se prenosi u sledece kolo. */
    private void settle(Series series, List<Match> games) {
        if (series.isWalkover()) {
            return;
        }
        setWinnerAndPropagate(series, winnerOf(series, toGames(series, games)));
    }

    /** Pobednik serije posle zadatih meceva, ili null dok serija traje. */
    private static Player winnerOf(Series series, List<SeriesRules.Game> games) {
        return SeriesRules.evaluate(series.getRound().getWinsRequired(), games).winner()
                .map(side -> side == SeriesRules.Side.FIRST ? series.getPlayer1() : series.getPlayer2())
                .orElse(null);
    }

    /**
     * Provera PRE izmene: ako bi novi pobednik (ili njegov nestanak) promenio igraca u seriji
     * sledeceg kola ili u mecu za 3. mesto, ta serija ne sme da je pocela. Sve se proverava pre
     * prvog upisa, da odbijena izmena ne ostavi nista za sobom.
     */
    private void ensureCanPropagate(Series series, Player newWinner) {
        if (series.isThirdPlace() || samePlayer(series.getWinner(), newWinner)) {
            return;
        }
        StageRound round = series.getRound();
        Stage stage = round.getStage();
        int size = stage.getBracketSize();
        if (round.getRoundNo() >= BracketMath.roundCount(size)) {
            return;
        }
        int nextRound = round.getRoundNo() + 1;
        seriesRepository.findInBracket(stage.getStageID(), nextRound, BracketMath.nextPosition(series.getPosition()), false)
                .ifPresent(next -> requireNotStarted(next, series));
        if (BracketMath.isSemiFinal(size, round.getRoundNo())) {
            seriesRepository.findInBracket(stage.getStageID(), nextRound, 1, true)
                    .ifPresent(third -> requireNotStarted(third, series));
        }
    }

    private void requireNotStarted(Series target, Series from) {
        if (!games(target).isEmpty() || target.getWinner() != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Serija " + label(target) + " je vec pocela — ova izmena bi promenila njenog igraca. "
                            + "Prvo obrisite meceve serije " + label(target) + ", pa onda menjajte " + label(from));
        }
    }

    private void setWinnerAndPropagate(Series series, Player winner) {
        series.setWinner(winner);
        seriesRepository.save(series);

        if (series.isThirdPlace()) {
            return;
        }
        StageRound round = series.getRound();
        Stage stage = round.getStage();
        int size = stage.getBracketSize();
        if (round.getRoundNo() >= BracketMath.roundCount(size)) {
            return;
        }

        int nextRound = round.getRoundNo() + 1;
        seriesRepository.findInBracket(stage.getStageID(), nextRound, BracketMath.nextPosition(series.getPosition()), false)
                .ifPresent(next -> place(next, BracketMath.goesToFirstSlot(series.getPosition()), winner, series));

        if (BracketMath.isSemiFinal(size, round.getRoundNo())) {
            Player loser = winner == null ? null
                    : winner.getPlayerID().equals(series.getPlayer1().getPlayerID()) ? series.getPlayer2() : series.getPlayer1();
            seriesRepository.findInBracket(stage.getStageID(), nextRound, 1, true)
                    .ifPresent(third -> place(third, series.getPosition() == 1, loser, series));
        }
    }

    /** Postavlja igraca u mesto serije sledeceg kola; zabranjeno ako je ta serija vec pocela. */
    private void place(Series target, boolean firstSlot, Player player, Series from) {
        Player current = firstSlot ? target.getPlayer1() : target.getPlayer2();
        if (samePlayer(current, player)) {
            return;
        }
        // Proverava se i ovde (ensureCanPropagate je vec prosao) — poslednja linija odbrane drveta.
        requireNotStarted(target, from);
        if (firstSlot) {
            target.setPlayer1(player);
            target.setSeed1(null);
        } else {
            target.setPlayer2(player);
            target.setSeed2(null);
        }
        seriesRepository.save(target);
    }

    /** Pamti seriju, njenu sledecu i mec za 3. mesto — sve sto jedna izmena moze da dirne. */
    private void captureBracketAround(UndoDraft undo, Series series) {
        undo.changed(UndoTables.SERIES, UndoTables.SERIES_ID, series.getSeriesID());
        if (series.isThirdPlace()) {
            return;
        }
        StageRound round = series.getRound();
        Stage stage = round.getStage();
        int size = stage.getBracketSize();
        if (round.getRoundNo() >= BracketMath.roundCount(size)) {
            return;
        }
        int nextRound = round.getRoundNo() + 1;
        seriesRepository.findInBracket(stage.getStageID(), nextRound, BracketMath.nextPosition(series.getPosition()), false)
                .ifPresent(next -> undo.changed(UndoTables.SERIES, UndoTables.SERIES_ID, next.getSeriesID()));
        if (BracketMath.isSemiFinal(size, round.getRoundNo())) {
            seriesRepository.findInBracket(stage.getStageID(), nextRound, 1, true)
                    .ifPresent(third -> undo.changed(UndoTables.SERIES, UndoTables.SERIES_ID, third.getSeriesID()));
        }
    }

    /** BR-11 za playoff: najveci rezultat je iz kola (Final Four do 16 → najvise 17), inace iz sezone. */
    private static void validateScore(StageRound round, int score1, int score2) {
        int maxScore = round.effectiveMaxScore();
        MatchResultRules.validate(maxScore, score1, score2).ifPresent(violation -> {
            throw new ApiException(HttpStatus.BAD_REQUEST, MatchResultWriter.describe(violation, maxScore)
                    .replace("u ovoj sezoni", "u ovom kolu"));
        });
    }

    private static void applyScore(Match match, int score1, int score2) {
        match.setScorePlayer1(score1);
        match.setScorePlayer2(score2);
        match.setWinner(MatchResultRules.firstPlayerWon(score1, score2) ? match.getPlayer1() : match.getPlayer2());
        match.setStatus(MatchStatus.FINISHED);
    }

    private static List<SeriesRules.Game> toGames(Series series, List<Match> games) {
        return games.stream().map(game -> toGame(series, game)).toList();
    }

    /** Rezultat iz ugla serije — mec je uvek upisan sa prvim igracem serije kao prvim. */
    private static SeriesRules.Game toGame(Series series, Match game) {
        boolean sameOrder = series.getPlayer1() == null
                || game.getPlayer1().getPlayerID().equals(series.getPlayer1().getPlayerID());
        return sameOrder
                ? new SeriesRules.Game(game.getScorePlayer1(), game.getScorePlayer2())
                : new SeriesRules.Game(game.getScorePlayer2(), game.getScorePlayer1());
    }

    /* ------------------------------------------------------------------ mapiranje */

    private BracketInfo toBracket(Stage stage) {
        List<StageRound> rounds = stageRoundRepository.findByStage_StageIDOrderByRoundNoAsc(stage.getStageID());
        List<Series> all = seriesRepository.findByStageFetched(stage.getStageID());
        Map<Long, List<Match>> games = gamesBySeries(stage.getStageID());

        List<RoundInfo> roundInfos = new ArrayList<>();
        for (StageRound round : rounds) {
            List<SeriesInfo> series = all.stream()
                    .filter(s -> s.getRound().getStageRoundID().equals(round.getStageRoundID()))
                    .sorted(Comparator.comparing(Series::isThirdPlace).thenComparingInt(Series::getPosition))
                    .map(s -> toSeriesInfo(s, games.getOrDefault(s.getSeriesID(), List.of())))
                    .toList();
            roundInfos.add(new RoundInfo(round.getStageRoundID(), round.getRoundNo(), round.getName(),
                    round.getWinsRequired(), round.effectiveTargetScore(), round.effectiveMaxScore(),
                    round.effectiveDurationMinutes(),
                    round.getTargetScore() != null || round.getMaxScore() != null || round.getDurationMinutes() != null,
                    series));
        }
        return new BracketInfo(stage.getStageID(), stage.getName(), stage.getStatus(),
                stage.getBracketSize() == null ? 0 : stage.getBracketSize(), roundInfos);
    }

    SeriesInfo toSeriesInfo(Series series, List<Match> games) {
        SeriesRules.State state = SeriesRules.evaluate(series.getRound().getWinsRequired(), toGames(series, games));
        List<GameInfo> gameInfos = games.stream()
                .sorted(Comparator.comparing(Match::getGameNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(game -> {
                    SeriesRules.Game g = toGame(series, game);
                    return new GameInfo(game.getMatchID(), game.getGameNumber() == null ? 0 : game.getGameNumber(),
                            g.scoreFirst(), g.scoreSecond(),
                            game.getWinner() == null ? null : game.getWinner().getPlayerID(),
                            game.getStatus(), game.getScheduledStart(),
                            game.getCourt() == null ? null : game.getCourt().getName());
                })
                .toList();
        return new SeriesInfo(
                series.getSeriesID(),
                number(series),
                series.getRound().getRoundNo(),
                series.getPosition(),
                series.isThirdPlace(),
                series.getPlayer1() == null ? null : PublicSiteService.toPlayer(series.getPlayer1()),
                series.getPlayer2() == null ? null : PublicSiteService.toPlayer(series.getPlayer2()),
                series.getSeed1(),
                series.getSeed2(),
                state.winsFirst(),
                state.winsSecond(),
                series.getWinner() == null ? null : series.getWinner().getPlayerID(),
                series.isWalkover(),
                status(series, games),
                series.getScheduledStart(),
                series.getCourt() == null ? null : series.getCourt().getCourtID(),
                series.getCourt() == null ? null : series.getCourt().getName(),
                series.getNote(),
                gameInfos);
    }

    static String status(Series series, List<Match> games) {
        if (series.getWinner() != null) {
            return FINISHED;
        }
        if (!series.hasBothPlayers()) {
            return PENDING;
        }
        return games.isEmpty() ? READY : IN_PROGRESS;
    }

    static int number(Series series) {
        Stage stage = series.getRound().getStage();
        int size = stage.getBracketSize() == null ? 0 : stage.getBracketSize();
        if (!BracketMath.isValidSize(size)) {
            return series.getPosition();
        }
        return BracketMath.number(size, series.getRound().getRoundNo(), series.getPosition(), series.isThirdPlace());
    }

    /* ------------------------------------------------------------------ pomocne */

    private List<Match> games(Series series) {
        if (series.getSeriesID() == null) {
            return List.of();
        }
        return new ArrayList<>(matchRepository.findBySeries_SeriesIDOrderByGameNumberAsc(series.getSeriesID()));
    }

    private Stage getKnockoutStageOrThrow(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Faza sa ID " + stageId + " ne postoji"));
        if (stage.getType() != StageType.KNOCKOUT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Faza „" + stage.getName() + "\" nije playoff (KNOCKOUT)");
        }
        return stage;
    }

    private Series getSeriesOrThrow(Long seriesId) {
        return seriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResourceNotFoundException("Serija sa ID " + seriesId + " ne postoji"));
    }

    private Player findPlayerOrNull(Long playerId) {
        if (playerId == null) {
            return null;
        }
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Igrac sa ID " + playerId + " ne postoji"));
    }

    private Court findCourt(Long courtId) {
        if (courtId == null) {
            return null;
        }
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Teren sa ID " + courtId + " ne postoji"));
    }

    private void requireNotElsewhereInRound(Series series, Player player) {
        boolean elsewhere = seriesRepository.findByRound_StageRoundID(series.getRound().getStageRoundID()).stream()
                .filter(other -> !other.getSeriesID().equals(series.getSeriesID()))
                .filter(other -> other.isThirdPlace() == series.isThirdPlace())
                .anyMatch(other -> samePlayer(other.getPlayer1(), player) || samePlayer(other.getPlayer2(), player));
        if (elsewhere) {
            throw new ApiException(HttpStatus.CONFLICT,
                    player.getDisplayName() + " vec igra u drugoj seriji kola „" + series.getRound().getName() + "\"");
        }
    }

    private static void requireDistinct(Player first, Player second) {
        if (first != null && second != null && first.getPlayerID().equals(second.getPlayerID())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Igrac ne moze igrati sam protiv sebe");
        }
    }

    private static Player participantOrThrow(Series series, Long playerId) {
        if (playerId.equals(series.getPlayer1().getPlayerID())) {
            return series.getPlayer1();
        }
        if (playerId.equals(series.getPlayer2().getPlayerID())) {
            return series.getPlayer2();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Igrac sa ID " + playerId + " ne igra u seriji " + label(series));
    }

    private static boolean samePlayer(Player a, Player b) {
        return Objects.equals(a == null ? null : a.getPlayerID(), b == null ? null : b.getPlayerID());
    }

    /** „M05 (Top 64)" — kako je na kosturu lige. */
    static String label(Series series) {
        String name = series.isThirdPlace() ? "za 3. mesto" : series.getRound().getName();
        return String.format("M%02d (%s)", number(series), name);
    }

    private static String score(Series series, List<Match> games) {
        SeriesRules.State state = SeriesRules.evaluate(series.getRound().getWinsRequired(), toGames(series, games));
        return state.winsFirst() + ":" + state.winsSecond();
    }

    private static String nickname(Player player) {
        return player == null ? "?" : player.getNickname();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trimNote(String note) {
        return note.length() <= 500 ? note : note.substring(0, 497) + "...";
    }
}
