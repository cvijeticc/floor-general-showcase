package rs.floorgeneral.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.floorgeneral.domain.Court;

public interface CourtRepository extends JpaRepository<Court, Long> {
}
