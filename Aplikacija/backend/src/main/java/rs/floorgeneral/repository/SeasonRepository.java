package rs.floorgeneral.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rs.floorgeneral.domain.Season;
import rs.floorgeneral.domain.SeasonStatus;

public interface SeasonRepository extends JpaRepository<Season, Long>, JpaSpecificationExecutor<Season> {

    /** U jednoj ligi nema dve sezone sa istim nazivom (UC-ADM-05). */
    boolean existsByLeague_LeagueIDAndNameIgnoreCase(Long leagueID, String name);

    boolean existsByLeague_LeagueIDAndNameIgnoreCaseAndSeasonIDNot(Long leagueID, String name, Long seasonID);

    /** Javni sajt prikazuje sezonu koja je u toku; ako je nema, poslednju zavrsenu. */
    Optional<Season> findFirstByStatusOrderByStartDateDesc(SeasonStatus status);
}
