package rs.floorgeneral.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.floorgeneral.domain.GroupEntry;

public interface GroupEntryRepository extends JpaRepository<GroupEntry, Long> {

    boolean existsByPlayer_PlayerID(Long playerID);

    boolean existsByGroup_GroupID(Long groupID);

    long countByGroup_GroupID(Long groupID);

    List<GroupEntry> findByGroup_GroupID(Long groupID);

    /** Koliko je igraca vec rasporedjeno u grupe jedne faze (UC-ADM-15, provera kapaciteta). */
    long countByGroup_Stage_StageID(Long stageID);

    Optional<GroupEntry> findByGroup_GroupIDAndPlayer_PlayerID(Long groupID, Long playerID);

    /** Igrac sme biti u tacno jednoj grupi po fazi — bazi to ne mozemo nametnuti bez stage_id. */
    @Query("""
            select ge from GroupEntry ge
            where ge.player.playerID = :playerId and ge.group.stage.stageID = :stageId
            """)
    Optional<GroupEntry> findByPlayerInStage(@Param("playerId") Long playerId, @Param("stageId") Long stageId);

    /** Igraci u grupama faza sezone koje nisu u pripremi — javni spisak igraca (UC-PUB-10). */
    @Query("""
            select ge from GroupEntry ge
              join fetch ge.player
              join fetch ge.group g
              join fetch g.stage st
            where st.season.seasonID = :seasonId
              and st.status <> rs.floorgeneral.domain.StageStatus.DRAFT
            """)
    List<GroupEntry> findVisibleInSeason(@Param("seasonId") Long seasonId);

    /** Isto, za jednog igraca — javni profil (UC-PUB-06). */
    @Query("""
            select ge from GroupEntry ge
              join fetch ge.group g
              join fetch g.stage st
            where st.season.seasonID = :seasonId
              and st.status <> rs.floorgeneral.domain.StageStatus.DRAFT
              and ge.player.playerID = :playerId
            """)
    List<GroupEntry> findVisibleInSeasonForPlayer(@Param("seasonId") Long seasonId, @Param("playerId") Long playerId);
}
