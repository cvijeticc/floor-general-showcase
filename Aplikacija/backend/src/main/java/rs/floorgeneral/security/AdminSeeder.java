package rs.floorgeneral.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.floorgeneral.domain.AppUser;
import rs.floorgeneral.domain.UserRole;
import rs.floorgeneral.repository.AppUserRepository;

/** Kreira podrazumevani admin nalog na prvom pokretanju, dok tabela app_users jos nema nijedan red. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            return;
        }

        // @ConfigurationProperties ne puca na nerazresen placeholder -- bez ovoga bi prod
        // (app.admin.password=${ADMIN_PASSWORD}) napravio admina sa doslovnom lozinkom "${ADMIN_PASSWORD}".
        String password = adminProperties.password();
        if (password == null || password.isBlank() || password.startsWith("${")) {
            throw new IllegalStateException("Lozinka za prvi admin nalog nije podesena (ADMIN_PASSWORD).");
        }

        AppUser admin = new AppUser();
        admin.setUsername(adminProperties.username());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        appUserRepository.save(admin);

        log.warn("Kreiran podrazumevani admin nalog '{}' — promeniti lozinku posle prve prijave.", admin.getUsername());
    }
}
