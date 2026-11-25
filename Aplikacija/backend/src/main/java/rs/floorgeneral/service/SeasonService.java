package rs.floorgeneral.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import rs.floorgeneral.web.dto.season.SeasonResult;
import rs.floorgeneral.web.dto.season.UpdateSeasonCmd;
import rs.floorgeneral.web.mapper.SeasonMapper;

/**
 * UC-ADM-05 — sezone i parametri bodovanja. Sezona nosi SAMO bodovanje; parametri formata
 * (broj grupa, velicina grupe, krugovi, prolaz, drvo) zive na Fazi — odluka D-05.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("startDate"), Sort.Order.asc("name"));

    /** Dozvoljeni prelazi statusa: DRAFT -> ACTIVE -> FINISHED, bez vracanja unazad. */
    private static final Map<SeasonStatus, Set<SeasonStatus>> ALLOWED_TRANSITIONS = allowedTransitions();

    private final SeasonRepository seasonRepository;
    private final LeagueRepository leagueRepository;
    private final StageRepository stageRepository;
    private final SeasonMapper seasonMapper;

    public List<SeasonResult> search(Long leagueId, SeasonStatus status) {
        List<Specification<Season>> filters = new ArrayList<>();
        if (leagueId != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("league").get("leagueID"), leagueId));
        }
        if (status != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return seasonRepository.findAll(Specification.allOf(filters), NEWEST_FIRST).stream()
                .map(seasonMapper::toResult)
                .toList();
    }

    public SeasonInfo findById(Long id) {
        return seasonMapper.toInfo(getSeasonOrThrow(id));
    }

    @Transactional
    public SeasonInfo create(CreateSeasonCmd cmd) {
        League league = leagueRepository.findById(cmd.leagueId())
                .orElseThrow(() -> new ResourceNotFoundException("Liga sa ID " + cmd.leagueId() + " ne postoji"));

        validateDates(cmd.startDate(), cmd.endDate());
        if (seasonRepository.existsByLeague_LeagueIDAndNameIgnoreCase(league.getLeagueID(), cmd.name())) {
            throw duplicateName(cmd.name());
        }

        Season season = seasonMapper.toEntity(cmd);
        season.setLeague(league);
        season.setStatus(SeasonStatus.DRAFT);

        // Izostavljen parametar znaci "podrazumevana vrednost" — vrednosti su vec na entitetu.
        applyScoringIfPresent(season, Scoring.of(cmd));
        validateScoring(season);

        return seasonMapper.toInfo(seasonRepository.save(season));
    }

    @Transactional
    public SeasonInfo update(Long id, UpdateSeasonCmd cmd) {
        Season season = getSeasonOrThrow(id);

        validateDates(cmd.startDate(), cmd.endDate());
        if (seasonRepository.existsByLeague_LeagueIDAndNameIgnoreCaseAndSeasonIDNot(
                season.getLeague().getLeagueID(), cmd.name(), season.getSeasonID())) {
            throw duplicateName(cmd.name());
        }
        // FR-4.1 — bodovanje se menja samo dok je sezona DRAFT.
        if (season.getStatus() != SeasonStatus.DRAFT && scoringWouldChange(season, cmd)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Parametri bodovanja se mogu menjati samo dok je sezona u statusu DRAFT (trenutno: "
                            + season.getStatus() + ")");
        }

        seasonMapper.updateEntityFromCmd(cmd, season);
        applyScoringIfPresent(season, Scoring.of(cmd));
        validateScoring(season);

        return seasonMapper.toInfo(season);
    }

    @Transactional
    public SeasonInfo changeStatus(Long id, ChangeSeasonStatusCmd cmd) {
        Season season = getSeasonOrThrow(id);

        if (!ALLOWED_TRANSITIONS.get(season.getStatus()).contains(cmd.status())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Sezona ne moze preci iz statusa " + season.getStatus() + " u " + cmd.status());
        }
        season.setStatus(cmd.status());

        return seasonMapper.toInfo(season);
    }

    @Transactional
    public void delete(Long id) {
        Season season = getSeasonOrThrow(id);

        if (stageRepository.existsBySeason_SeasonID(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Sezona " + season.getName() + " ima faze i ne moze se obrisati — prvo obrisite faze");
        }

        seasonRepository.delete(season);
    }

    private Season getSeasonOrThrow(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sezona sa ID " + id + " ne postoji"));
    }

    private static void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Datum zavrsetka ne moze biti pre datuma pocetka");
        }
    }

    /** BR-10.2 — kaznjeni igrac dobija manje poena od protivnika, inace sluzbeni rezultat nema smisla. */
    private static void validateScoring(Season season) {
        if (season.getForfeitWinnerScore() <= season.getForfeitLoserScore()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Sluzbeni rezultat pobednika mora biti veci od rezultata kaznjenog igraca");
        }
        // Odluka D-18 — gornja granica je iznad granice za rani zavrsetak, jer sut spolja vredi dva poena.
        if (season.getMaxScore() < season.getTargetScore()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Najveci moguci rezultat (" + season.getMaxScore() + ") ne sme biti manji od "
                            + season.getTargetScore() + " — toliko je potrebno za rani zavrsetak meca");
        }
        if (season.getForfeitWinnerScore() > season.getMaxScore()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Sluzbeni rezultat pobednika ne sme biti veci od najveceg moguceg rezultata ("
                            + season.getMaxScore() + ")");
        }
    }

    private static void applyScoringIfPresent(Season season, Scoring scoring) {
        if (scoring.pointsForWin() != null) {
            season.setPointsForWin(scoring.pointsForWin());
        }
        if (scoring.pointsForLoss() != null) {
            season.setPointsForLoss(scoring.pointsForLoss());
        }
        if (scoring.pointsForForfeit() != null) {
            season.setPointsForForfeit(scoring.pointsForForfeit());
        }
        if (scoring.forfeitWinnerScore() != null) {
            season.setForfeitWinnerScore(scoring.forfeitWinnerScore());
        }
        if (scoring.forfeitLoserScore() != null) {
            season.setForfeitLoserScore(scoring.forfeitLoserScore());
        }
        if (scoring.targetScore() != null) {
            season.setTargetScore(scoring.targetScore());
        }
        if (scoring.maxScore() != null) {
            season.setMaxScore(scoring.maxScore());
        }
        if (scoring.durationMinutes() != null) {
            season.setDurationMinutes(scoring.durationMinutes());
        }
    }

    /**
     * Parametri bodovanja i formata meca iz zahteva. Svi su opcioni: {@code null} znaci
     * „ne diraj", pa se pri kreiranju zadrzava podrazumevana vrednost sa entiteta.
     */
    private record Scoring(Integer pointsForWin, Integer pointsForLoss, Integer pointsForForfeit,
                           Integer forfeitWinnerScore, Integer forfeitLoserScore, Integer targetScore,
                           Integer maxScore, Integer durationMinutes) {

        static Scoring of(CreateSeasonCmd cmd) {
            return new Scoring(cmd.pointsForWin(), cmd.pointsForLoss(), cmd.pointsForForfeit(),
                    cmd.forfeitWinnerScore(), cmd.forfeitLoserScore(), cmd.targetScore(),
                    cmd.maxScore(), cmd.durationMinutes());
        }

        static Scoring of(UpdateSeasonCmd cmd) {
            return new Scoring(cmd.pointsForWin(), cmd.pointsForLoss(), cmd.pointsForForfeit(),
                    cmd.forfeitWinnerScore(), cmd.forfeitLoserScore(), cmd.targetScore(),
                    cmd.maxScore(), cmd.durationMinutes());
        }
    }

    /** Poslat parametar koji je jednak postojecoj vrednosti nije izmena — takav PUT sme i van DRAFT-a. */
    private static boolean scoringWouldChange(Season season, UpdateSeasonCmd cmd) {
        return differs(cmd.pointsForWin(), season.getPointsForWin())
                || differs(cmd.pointsForLoss(), season.getPointsForLoss())
                || differs(cmd.pointsForForfeit(), season.getPointsForForfeit())
                || differs(cmd.forfeitWinnerScore(), season.getForfeitWinnerScore())
                || differs(cmd.forfeitLoserScore(), season.getForfeitLoserScore())
                || differs(cmd.targetScore(), season.getTargetScore())
                || differs(cmd.maxScore(), season.getMaxScore())
                || differs(cmd.durationMinutes(), season.getDurationMinutes());
    }

    private static boolean differs(Integer sent, int current) {
        return sent != null && sent != current;
    }

    private static ApiException duplicateName(String name) {
        return new ApiException(HttpStatus.CONFLICT, "Sezona sa nazivom " + name + " vec postoji u ovoj ligi");
    }

    private static Map<SeasonStatus, Set<SeasonStatus>> allowedTransitions() {
        Map<SeasonStatus, Set<SeasonStatus>> transitions = new EnumMap<>(SeasonStatus.class);
        transitions.put(SeasonStatus.DRAFT, Set.of(SeasonStatus.ACTIVE));
        transitions.put(SeasonStatus.ACTIVE, Set.of(SeasonStatus.FINISHED));
        transitions.put(SeasonStatus.FINISHED, Set.of());
        return transitions;
    }
}
