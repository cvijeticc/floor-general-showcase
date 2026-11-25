package rs.floorgeneral.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import rs.floorgeneral.domain.Application;
import rs.floorgeneral.web.dto.application.ApplicationResult;
import rs.floorgeneral.web.dto.application.CreateApplicationCmd;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApplicationMapper {

    @Mapping(target = "id", source = "applicationID")
    @Mapping(target = "playerId", source = "player.playerID")
    ApplicationResult toResult(Application application);

    @Mapping(target = "applicationID", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "player", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Application toEntity(CreateApplicationCmd cmd);
}
