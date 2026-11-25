package rs.floorgeneral.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.floorgeneral.domain.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByStage_StageIDOrderByNameAsc(Long stageID);

    boolean existsByStage_StageID(Long stageID);

    boolean existsByStage_StageIDAndNameIgnoreCase(Long stageID, String name);

    long countByStage_StageID(Long stageID);

    /** Sve grupe sezone kroz sve faze — sezona do grupe ide preko faze (od V3). */
    @Query("""
            select g from Group g
            where g.stage.season.seasonID = :seasonId
            order by g.stage.orderNo asc, g.name asc
            """)
    List<Group> findBySeasonOrderByStageAndName(@Param("seasonId") Long seasonId);
}
