package rs.floorgeneral.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.floorgeneral.domain.Player;

public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    /** Aktivni igraci koji jos nisu ni u jednoj grupi zadate faze (UC-ADM-07). */
    @Query("""
            select p from Player p
            where p.status = rs.floorgeneral.domain.PlayerStatus.ACTIVE
              and not exists (
                    select 1 from GroupEntry ge
                    where ge.player = p and ge.group.stage.stageID = :stageId)
            order by p.lastName asc, p.firstName asc
            """)
    List<Player> findUnassignedForStage(@Param("stageId") Long stageId);
}
