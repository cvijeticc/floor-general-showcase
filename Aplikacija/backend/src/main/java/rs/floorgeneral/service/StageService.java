package rs.floorgeneral.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageStatus;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.SeasonRepository;
import rs.floorgeneral.repository.StageRepository;
import rs.floorgeneral.web.dto.stage.ChangeStageStatusCmd;
import rs.floorgeneral.web.dto.stage.CreateStageCmd;
import rs.floorgeneral.web.dto.stage.StageInfo;
import rs.floorgeneral.web.dto.stage.StageResult;
import rs.floorgeneral.web.dto.stage.UpdateStageCmd;
import rs.floorgeneral.web.mapper.StageMapper;

/**
 * UC-ADM-14 (faze) + UC-ADM-15 (parametri formata). Ovde se cuva sve sto je specifikacija
 * zabranila da bude u kodu: broj grupa, velicina grupe, krugovi, prolaz i velicina drveta
 * (spec poglavlje 4, odluka D-05).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageService {

    /** Statusi meca koji znace da je mec odigran — MatchStatus.hasResult(). */
    private static final Set<MatchStatus> PLAYED = Set.of(MatchStatus.FINISHED, MatchStatus.FORFEIT, MatchStatus.WALKOVER,
            MatchStatus.DOUBLE_FORFEIT);

    private static final Map<StageStatus, Set<StageStatus>> ALLOWED_TRANSITIONS = allowedTransitions();

    private final StageRepository stageRepository;
    private final SeasonRepository seasonRepository;
    private final GroupRepository groupRepository;
    private final GroupEntryRepository groupEntryRepository;
    private final MatchRepository matchRepository;
    private final StageMapper stageMapper;

    public List<StageResult> searchBySeason(Long seasonId) {
        getSeasonOrThrow(seasonId);
        return stageRepository.findBySeason_SeasonIDOrderByOrderNoAsc(seasonId).stream()
                .map(stageMapper::toResult)
                .toList();
    }

    public StageInfo findById(Long id) {
        return stageMapper.toInfo(getStageOrThrow(id));
    }

    @Transactional
    public StageInfo create(Long seasonId, CreateStageCmd cmd) {
        Season season = getSeasonOrThrow(seasonId);

        if (stageRepository.existsBySeason_SeasonIDAndOrderNo(seasonId, cmd.orderNo())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Sezona vec ima fazu sa rednim brojem " + cmd.orderNo());
        }

        Stage stage = stageMapper.toEntity(cmd);
        stage.setSeason(season);
        stage.setStatus(StageStatus.DRAFT);
        validateFormat(stage);

        return stageMapper.toInfo(stageRepository.save(stage));
    }

    @Transactional
    public StageInfo update(Long id, UpdateStageCmd cmd) {
        Stage stage = getStageOrThrow(id);

        if (stage.getOrderNo() != cmd.orderNo()
                && stageRepository.existsBySeason_SeasonIDAndOrderNo(stage.getSeason().getSeasonID(), cmd.orderNo())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Sezona vec ima fazu sa rednim brojem " + cmd.orderNo());
        }
        // UC-ADM-14 — format se ne prekraja pod nogama odigranim mecevima.
        if (stage.getStatus() == StageStatus.ACTIVE && formatWouldChange(stage, cmd)
                && matchRepository.existsByStage_StageIDAndStatusIn(id, PLAYED)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Faza je u toku i ima odigrane meceve — parametri formata se vise ne mogu menjati");
        }

        stageMapper.updateEntityFromCmd(cmd, stage);
        validateFormat(stage);

        return stageMapper.toInfo(stage);
    }

    /** Prelazak u ACTIVE je „pokretanje faze" iz UC-ADM-15 — tu se proverava da format drzi vodu. */
    @Transactional
    public StageInfo changeStatus(Long id, ChangeStageStatusCmd cmd) {
        Stage stage = getStageOrThrow(id);

        if (!ALLOWED_TRANSITIONS.get(stage.getStatus()).contains(cmd.status())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Faza ne moze preci iz statusa " + stage.getStatus() + " u " + cmd.status());
        }
        if (cmd.status() == StageStatus.ACTIVE) {
            validateBeforeStart(stage);
        }
        stage.setStatus(cmd.status());

        return stageMapper.toInfo(stage);
    }

    @Transactional
    public void delete(Long id) {
        Stage stage = getStageOrThrow(id);

        if (groupRepository.existsByStage_StageID(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Faza " + stage.getName() + " ima grupe i ne moze se obrisati — prvo obrisite grupe");
        }
        if (matchRepository.existsByStage_StageID(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Faza " + stage.getName() + " ima meceve i ne moze se obrisati");
        }

        stageRepository.delete(stage);
    }

    private Stage getStageOrThrow(Long id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faza sa ID " + id + " ne postoji"));
    }

    private Season getSeasonOrThrow(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sezona sa ID " + id + " ne postoji"));
    }

    /** UC-ADM-15 — koji parametri su obavezni zavisi od tipa faze. */
    private static void validateFormat(Stage stage) {
        if (stage.getType() == StageType.GROUP) {
            requirePresent(stage.getGroupCount(), "Broj grupa je obavezan za grupnu fazu");
            requirePresent(stage.getGroupSize(), "Velicina grupe je obavezna za grupnu fazu");
            requirePresent(stage.getRoundCount(), "Broj krugova je obavezan za grupnu fazu");
            requirePresent(stage.getDirectAdvance(), "Direktan prolaz je obavezan za grupnu fazu");
            if (stage.getRoundCount() != 1 && stage.getRoundCount() != 2) {
                throw badRequest("Broj krugova moze biti 1 (jednokruzno) ili 2 (dvokruzno)");
            }
            if (stage.getDirectAdvance() > stage.getGroupSize()) {
                throw badRequest("Direktan prolaz ne moze biti veci od velicine grupe");
            }
            if (stage.getBracketSize() != null) {
                throw badRequest("Grupna faza nema velicinu drveta");
            }
            if (stage.getAdditionalAdvance() == null) {
                stage.setAdditionalAdvance(0);
            }
        } else {
            requirePresent(stage.getBracketSize(), "Velicina drveta je obavezna za playoff fazu");
            if (!isPowerOfTwo(stage.getBracketSize())) {
                throw badRequest("Velicina drveta mora biti stepen dvojke (npr. 16, 32, 64)");
            }
            if (stage.getGroupCount() != null || stage.getGroupSize() != null || stage.getRoundCount() != null
                    || stage.getDirectAdvance() != null || stage.getAdditionalAdvance() != null) {
                throw badRequest("Playoff faza nema grupne parametre");
            }
        }
    }

    /**
     * Provere koje imaju smisla tek pri pokretanju faze:
     * kapacitet grupa u odnosu na vec rasporedjene igrace i broj onih koji prolaze dalje (FR-4.2).
     */
    private void validateBeforeStart(Stage stage) {
        if (stage.getType() != StageType.GROUP) {
            return;
        }

        long assignedPlayers = groupEntryRepository.countByGroup_Stage_StageID(stage.getStageID());
        long capacity = (long) stage.getGroupCount() * stage.getGroupSize();
        if (capacity < assignedPlayers) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Format prima " + capacity + " igraca, a u grupe je vec rasporedjeno " + assignedPlayers);
        }

        Stage next = stageRepository
                .findFirstBySeason_SeasonIDAndOrderNoGreaterThanOrderByOrderNoAsc(
                        stage.getSeason().getSeasonID(), stage.getOrderNo())
                .orElse(null);

        // FR-4.2 vazi samo kad se iz ove faze ide pravo u drvo. Kad sledi jos jedna grupna faza
        // (200 -> 120 -> 64, odluka D-04), broj prolaznika nije ni potrebno da bude stepen dvojke.
        if (next != null && next.getType() == StageType.KNOCKOUT) {
            int advancing = stage.getGroupCount() * stage.getDirectAdvance() + stage.getAdditionalAdvance();
            if (advancing != next.getBracketSize()) {
                throw badRequest("Iz faze prolazi " + advancing + " igraca, a drvo faze "
                        + next.getName() + " prima " + next.getBracketSize()
                        + " — uskladite direktan i dodatni prolaz");
            }
        }
    }

    private static boolean formatWouldChange(Stage stage, UpdateStageCmd cmd) {
        return !Objects.equals(stage.getGroupCount(), cmd.groupCount())
                || !Objects.equals(stage.getGroupSize(), cmd.groupSize())
                || !Objects.equals(stage.getRoundCount(), cmd.roundCount())
                || !Objects.equals(stage.getDirectAdvance(), cmd.directAdvance())
                || !Objects.equals(stage.getAdditionalAdvance(), cmd.additionalAdvance())
                || !Objects.equals(stage.getBracketSize(), cmd.bracketSize());
    }

    private static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    private static void requirePresent(Integer value, String message) {
        if (value == null) {
            throw badRequest(message);
        }
    }

    private static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private static Map<StageStatus, Set<StageStatus>> allowedTransitions() {
        Map<StageStatus, Set<StageStatus>> transitions = new EnumMap<>(StageStatus.class);
        transitions.put(StageStatus.DRAFT, Set.of(StageStatus.ACTIVE));
        transitions.put(StageStatus.ACTIVE, Set.of(StageStatus.FINISHED));
        transitions.put(StageStatus.FINISHED, Set.of());
        return transitions;
    }
}
