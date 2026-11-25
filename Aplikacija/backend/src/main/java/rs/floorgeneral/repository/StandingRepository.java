package rs.floorgeneral.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.floorgeneral.domain.Standing;

public interface StandingRepository extends JpaRepository<Standing, Long> {

    List<Standing> findByGroup_GroupIDOrderByPositionAsc(Long groupID);

    List<Standing> findByGroup_GroupID(Long groupID);

    /** Redovi svih grupa odjednom — javni spisak igraca (UC-PUB-10). */
    List<Standing> findByGroup_GroupIDIn(Collection<Long> groupIDs);

    Optional<Standing> findByGroup_GroupIDAndPlayer_PlayerID(Long groupID, Long playerID);
}
