package rs.floorgeneral.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import rs.floorgeneral.domain.Court;
import rs.floorgeneral.web.dto.court.CourtInfo;
import rs.floorgeneral.web.dto.court.CourtResult;
import rs.floorgeneral.web.dto.court.CreateCourtCmd;
import rs.floorgeneral.web.dto.court.UpdateCourtCmd;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CourtMapper {

    @Mapping(target = "id", source = "courtID")
    CourtInfo toInfo(Court court);

    @Mapping(target = "id", source = "courtID")
    CourtResult toResult(Court court);

    @Mapping(target = "courtID", ignore = true)
    Court toEntity(CreateCourtCmd cmd);

    @Mapping(target = "courtID", ignore = true)
    void updateEntityFromCmd(UpdateCourtCmd cmd, @MappingTarget Court court);
}
