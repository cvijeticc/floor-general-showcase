package rs.floorgeneral.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.match.MatchResult;

/**
 * Meceve ne pravi maper nego servis — previse veza (grupa, faza, igraci, teren) mora prvo
 * da se proveri, pa se ovde samo cita iz entiteta u DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MatchMapper {

    @Mapping(target = "id", source = "matchID")
    @Mapping(target = "stageId", source = "stage.stageID")
    @Mapping(target = "stageName", source = "stage.name")
    @Mapping(target = "groupId", source = "group.groupID")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "player1Id", source = "player1.playerID")
    @Mapping(target = "player1Name", source = "player1.fullName")
    @Mapping(target = "player2Id", source = "player2.playerID")
    @Mapping(target = "player2Name", source = "player2.fullName")
    @Mapping(target = "winnerId", source = "winner.playerID")
    @Mapping(target = "winnerName", source = "winner.fullName")
    @Mapping(target = "courtId", source = "court.courtID")
    @Mapping(target = "courtName", source = "court.name")
    MatchInfo toInfo(Match match);

    @Mapping(target = "id", source = "matchID")
    @Mapping(target = "groupId", source = "group.groupID")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "player1Id", source = "player1.playerID")
    @Mapping(target = "player1Name", source = "player1.fullName")
    @Mapping(target = "player2Id", source = "player2.playerID")
    @Mapping(target = "player2Name", source = "player2.fullName")
    @Mapping(target = "courtName", source = "court.name")
    MatchResult toResult(Match match);
}
