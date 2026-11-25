package rs.floorgeneral.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.web.dto.season.CreateSeasonCmd;
import rs.floorgeneral.web.dto.season.SeasonInfo;
import rs.floorgeneral.web.dto.season.SeasonResult;
import rs.floorgeneral.web.dto.season.UpdateSeasonCmd;

/**
 * Parametri bodovanja se NE mapiraju automatski: na ulazu su opcioni (Integer), pa ih servis
 * postavlja sam — podrazumevanim vrednostima pri kreiranju, odnosno samo ako su poslati pri izmeni.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SeasonMapper {

    @Mapping(target = "id", source = "seasonID")
    @Mapping(target = "leagueId", source = "league.leagueID")
    @Mapping(target = "leagueName", source = "league.name")
    SeasonInfo toInfo(Season season);

    @Mapping(target = "id", source = "seasonID")
    @Mapping(target = "leagueId", source = "league.leagueID")
    @Mapping(target = "leagueName", source = "league.name")
    SeasonResult toResult(Season season);

    @Mapping(target = "seasonID", ignore = true)
    @Mapping(target = "league", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "pointsForWin", ignore = true)
    @Mapping(target = "pointsForLoss", ignore = true)
    @Mapping(target = "pointsForForfeit", ignore = true)
    @Mapping(target = "forfeitWinnerScore", ignore = true)
    @Mapping(target = "forfeitLoserScore", ignore = true)
    @Mapping(target = "targetScore", ignore = true)
    @Mapping(target = "maxScore", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    Season toEntity(CreateSeasonCmd cmd);

    @Mapping(target = "seasonID", ignore = true)
    @Mapping(target = "league", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "pointsForWin", ignore = true)
    @Mapping(target = "pointsForLoss", ignore = true)
    @Mapping(target = "pointsForForfeit", ignore = true)
    @Mapping(target = "forfeitWinnerScore", ignore = true)
    @Mapping(target = "forfeitLoserScore", ignore = true)
    @Mapping(target = "targetScore", ignore = true)
    @Mapping(target = "maxScore", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    void updateEntityFromCmd(UpdateSeasonCmd cmd, @MappingTarget Season season);
}
