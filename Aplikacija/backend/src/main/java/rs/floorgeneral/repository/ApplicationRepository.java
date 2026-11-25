package rs.floorgeneral.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.floorgeneral.domain.Application;
import rs.floorgeneral.domain.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findAllByOrderByApplicationIDDesc();

    List<Application> findByStatusOrderByApplicationIDDesc(ApplicationStatus status);

    long countByStatus(ApplicationStatus status);
}
