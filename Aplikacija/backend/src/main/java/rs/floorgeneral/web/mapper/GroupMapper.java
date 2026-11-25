package rs.floorgeneral.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.web.dto.group.CreateGroupCmd;
import rs.floorgeneral.web.dto.group.GroupInfo;
import rs.floorgeneral.web.dto.group.GroupResult;
import rs.floorgeneral.web.dto.group.UpdateGroupCmd;

/** Broj popunjenih mesta se racuna u servisu i dolazi kao zaseban izvor mapiranja. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GroupMapper {

    @Mapping(target = "id", source = "group.groupID")
    @Mapping(target = "stageId", source = "group.stage.stageID")
    @Mapping(target = "stageName", source = "group.stage.name")
    @Mapping(target = "seasonId", source = "group.stage.season.seasonID")
    @Mapping(target = "seasonName", source = "group.stage.season.name")
    @Mapping(target = "name", source = "group.name")
    @Mapping(target = "groupSize", source = "group.stage.groupSize")
    @Mapping(target = "playerCount", source = "playerCount")
    GroupInfo toInfo(Group group, long playerCount);

    @Mapping(target = "id", source = "group.groupID")
    @Mapping(target = "stageId", source = "group.stage.stageID")
    @Mapping(target = "stageName", source = "group.stage.name")
    @Mapping(target = "name", source = "group.name")
    @Mapping(target = "groupSize", source = "group.stage.groupSize")
    @Mapping(target = "playerCount", source = "playerCount")
    GroupResult toResult(Group group, long playerCount);

    @Mapping(target = "groupID", ignore = true)
    @Mapping(target = "stage", ignore = true)
    Group toEntity(CreateGroupCmd cmd);

    @Mapping(target = "groupID", ignore = true)
    @Mapping(target = "stage", ignore = true)
    void updateEntityFromCmd(UpdateGroupCmd cmd, @MappingTarget Group group);
}
