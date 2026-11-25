package rs.floorgeneral.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.web.dto.player.CreatePlayerCmd;
import rs.floorgeneral.web.dto.player.PlayerInfo;
import rs.floorgeneral.web.dto.player.PlayerResult;
import rs.floorgeneral.web.dto.player.UpdatePlayerCmd;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PlayerMapper {

    @Mapping(target = "id", source = "playerID")
    PlayerInfo toInfo(Player player);

    @Mapping(target = "id", source = "playerID")
    PlayerResult toResult(Player player);

    /** Status se ne prima sa ulaza — nov igrac je uvek ACTIVE (BR-10). */
    @Mapping(target = "playerID", ignore = true)
    @Mapping(target = "status", ignore = true)
    Player toEntity(CreatePlayerCmd cmd);

    @Mapping(target = "playerID", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromCmd(UpdatePlayerCmd cmd, @MappingTarget Player player);
}
