package rs.floorgeneral.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.floorgeneral.domain.StageRound;

public interface StageRoundRepository extends JpaRepository<StageRound, Long> {

    List<StageRound> findByStage_StageIDOrderByRoundNoAsc(Long stageId);

    Optional<StageRound> findByStage_StageIDAndRoundNo(Long stageId, int roundNo);

    boolean existsByStage_StageID(Long stageId);
}
