package rs.floorgeneral.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.floorgeneral.domain.Series;

public interface SeriesRepository extends JpaRepository<Series, Long> {

    /** Sve serije faze sa igracima, pobednikom i terenom u jednom upitu (drvo bez N+1). */
    @Query("""
            select s from Series s
              join fetch s.round r
              left join fetch s.player1
              left join fetch s.player2
              left join fetch s.winner
              left join fetch s.court
            where r.stage.stageID = :stageId
            order by r.roundNo, s.thirdPlace, s.position
            """)
    List<Series> findByStageFetched(@Param("stageId") Long stageId);

    /** Serija u kolu na poziciji — sledeca u drvetu ili mec za 3. mesto. */
    @Query("""
            select s from Series s
            where s.round.stage.stageID = :stageId
              and s.round.roundNo = :roundNo
              and s.position = :position
              and s.thirdPlace = :thirdPlace
            """)
    Optional<Series> findInBracket(@Param("stageId") Long stageId, @Param("roundNo") int roundNo,
                                   @Param("position") int position, @Param("thirdPlace") boolean thirdPlace);

    List<Series> findByRound_StageRoundID(Long stageRoundId);
}
