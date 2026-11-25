package rs.floorgeneral.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.floorgeneral.domain.League;

public interface LeagueRepository extends JpaRepository<League, Long> {
}
