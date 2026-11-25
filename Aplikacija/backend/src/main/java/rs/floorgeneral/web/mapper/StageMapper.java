package rs.floorgeneral.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import rs.floorgeneral.domain.Stage;
import rs.floorgeneral.web.dto.stage.CreateStageCmd;
import rs.floorgeneral.web.dto.stage.StageInfo;
import rs.floorgeneral.web.dto.stage.StageResult;
import rs.floorgeneral.web.dto.stage.UpdateStageCmd;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StageMapper {

    @Mapping(target = "id", source = "stageID")
    @Mapping(target = "seasonId", source = "season.seasonID")
    @Mapping(target = "seasonName", source = "season.name")
    StageInfo toInfo(Stage stage);

    @Mapping(target = "id", source = "stageID")
    @Mapping(target = "seasonId", source = "season.seasonID")
    StageResult toResult(Stage stage);

    @Mapping(target = "stageID", ignore = true)
    @Mapping(target = "season", ignore = true)
    @Mapping(target = "status", ignore = true)
    Stage toEntity(CreateStageCmd cmd);

    /** Tip se ne menja izmenom, pa ga ovde nema ni u ulazu ni u mapiranju. */
    @Mapping(target = "stageID", ignore = true)
    @Mapping(target = "season", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "type", ignore = true)
    void updateEntityFromCmd(UpdateStageCmd cmd, @MappingTarget Stage stage);
}
