package rs.floorgeneral.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.floorgeneral.domain.Stage;

public interface StageRepository extends JpaRepository<Stage, Long> {

    boolean existsBySeason_SeasonID(Long seasonID);

    List<Stage> findBySeason_SeasonIDOrderByOrderNoAsc(Long seasonID);

    boolean existsBySeason_SeasonIDAndOrderNo(Long seasonID, int orderNo);

    /** Sledeca faza po redosledu — koristi se za proveru FR-4.2 pri pokretanju grupne faze. */
    Optional<Stage> findFirstBySeason_SeasonIDAndOrderNoGreaterThanOrderByOrderNoAsc(Long seasonID, int orderNo);
}
