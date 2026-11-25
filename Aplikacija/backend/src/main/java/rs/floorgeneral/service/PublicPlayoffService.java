package rs.floorgeneral.service;

import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.domain.StageStatus;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.StageRepository;
import rs.floorgeneral.repository.StageRoundRepository;
import rs.floorgeneral.web.dto.playoff.BracketInfo;
import rs.floorgeneral.web.dto.playoff.SeriesInfo;
import rs.floorgeneral.web.dto.site.PublicBracket;

/**
 * UC-PLA-01 — javno drvo playoff-a, bez prijave. Vidi se playoff faza sezone koja nije u pripremi
 * (DRAFT) i ima drvo; bez toga 404, pa sajt prikaze „Playoff jos nije objavljen".
 * Interne napomene serija (razlog sluzbenog rezultata) ne izlaze.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicPlayoffService {

    private final PublicSiteService publicSiteService;
    private final PlayoffService playoffService;
    private final StageRepository stageRepository;
    private final StageRoundRepository stageRoundRepository;

    public PublicBracket bracket(Long seasonId) {
        Season season = publicSiteService.resolveSeason(seasonId);
        Stage stage = stageRepository.findBySeason_SeasonIDOrderByOrderNoAsc(season.getSeasonID()).stream()
                .filter(s -> s.getType() == StageType.KNOCKOUT && s.getStatus() != StageStatus.DRAFT)
                .filter(s -> stageRoundRepository.existsByStage_StageID(s.getStageID()))
                .max(Comparator.comparingInt(Stage::getOrderNo))
                .orElseThrow(() -> new ResourceNotFoundException("Playoff sezone " + season.getName() + " jos nije objavljen"));

        BracketInfo bracket = playoffService.bracket(stage.getStageID());
        return new PublicBracket(bracket.stageId(), bracket.stageName(), bracket.bracketSize(),
                bracket.rounds().stream()
                        .map(round -> new PublicBracket.PublicRound(round.roundNo(), round.name(), round.winsRequired(),
                                round.targetScore(), round.maxScore(), round.durationMinutes(),
                                round.series().stream().map(PublicPlayoffService::toPublic).toList()))
                        .toList());
    }

    private static PublicBracket.PublicSeries toPublic(SeriesInfo s) {
        return new PublicBracket.PublicSeries(s.id(), s.number(), s.position(), s.thirdPlace(), s.player1(), s.player2(),
                s.seed1(), s.seed2(), s.wins1(), s.wins2(), s.winnerId(), s.walkover(), s.status(),
                s.scheduledStart(), s.courtName(),
                s.games().stream()
                        .map(g -> new PublicBracket.PublicGame(g.matchId(), g.gameNumber(), g.score1(), g.score2(),
                                g.winnerId(), g.scheduledStart(), g.courtName()))
                        .toList());
    }
}
