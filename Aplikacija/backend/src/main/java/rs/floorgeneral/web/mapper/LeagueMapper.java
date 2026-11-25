package rs.floorgeneral.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import rs.floorgeneral.domain.League;
import rs.floorgeneral.web.dto.league.CreateLeagueCmd;
import rs.floorgeneral.web.dto.league.LeagueInfo;
import rs.floorgeneral.web.dto.league.LeagueResult;
import rs.floorgeneral.web.dto.league.UpdateLeagueCmd;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LeagueMapper {

    @Mapping(target = "id", source = "leagueID")
    LeagueInfo toInfo(League league);

    @Mapping(target = "id", source = "leagueID")
    LeagueResult toResult(League league);

    @Mapping(target = "leagueID", ignore = true)
    League toEntity(CreateLeagueCmd cmd);

    @Mapping(target = "leagueID", ignore = true)
    void updateEntityFromCmd(UpdateLeagueCmd cmd, @MappingTarget League league);
}
