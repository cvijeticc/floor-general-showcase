package rs.floorgeneral.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.floorgeneral.domain.AppUser;
import rs.floorgeneral.domain.UserRole;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    long countByRole(UserRole role);
}
