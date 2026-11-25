package rs.floorgeneral.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.AppUser;
import rs.floorgeneral.domain.UserRole;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.AppUserRepository;
import rs.floorgeneral.web.dto.user.ChangeOwnPasswordCmd;
import rs.floorgeneral.web.dto.user.CreateUserCmd;
import rs.floorgeneral.web.dto.user.ResetPasswordCmd;
import rs.floorgeneral.web.dto.user.UserInfo;
import rs.floorgeneral.web.dto.user.UserResult;

/**
 * UC-ADM-13 — admin nalozi. Lozinka se cuva iskljucivo kao BCrypt hash i nikad se ne vraca
 * kroz API. Minimalna duzina je parametar ({@code app.rules.min-password-length}), ne konstanta.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AppUserService {

    private static final Sort BY_USERNAME = Sort.by(Sort.Direction.ASC, "username");

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final RuleProperties ruleProperties;

    public List<UserResult> search() {
        return appUserRepository.findAll(BY_USERNAME).stream()
                .map(user -> new UserResult(user.getAppUserID(), user.getUsername(), user.getEmail(), user.getRole()))
                .toList();
    }

    @Transactional
    public UserInfo create(CreateUserCmd cmd) {
        if (appUserRepository.findByUsername(cmd.username()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Nalog sa korisnickim imenom " + cmd.username() + " vec postoji");
        }
        validatePassword(cmd.password());

        AppUser user = new AppUser();
        user.setUsername(cmd.username());
        user.setPasswordHash(passwordEncoder.encode(cmd.password()));
        user.setEmail(cmd.email());
        user.setRole(cmd.role() == null ? UserRole.ADMIN : cmd.role());

        AppUser saved = appUserRepository.save(user);
        log.info("Kreiran nalog '{}' sa ulogom {}", saved.getUsername(), saved.getRole());

        return toInfo(saved);
    }

    /** Promena sopstvene lozinke — stara lozinka je obavezna i mora da se poklopi. */
    @Transactional
    public UserInfo changeOwnPassword(String username, ChangeOwnPasswordCmd cmd) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Nalog " + username + " ne postoji"));

        if (!passwordEncoder.matches(cmd.oldPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Stara lozinka nije tacna");
        }
        validatePassword(cmd.newPassword());

        user.setPasswordHash(passwordEncoder.encode(cmd.newPassword()));
        log.info("Nalog '{}' je promenio lozinku", username);

        return toInfo(user);
    }

    /** Reset tudje lozinke — radi ga administrator, pa se stara lozinka ne trazi. */
    @Transactional
    public UserInfo resetPassword(Long id, ResetPasswordCmd cmd) {
        AppUser user = getUserOrThrow(id);
        validatePassword(cmd.newPassword());

        user.setPasswordHash(passwordEncoder.encode(cmd.newPassword()));
        log.info("Administrator je resetovao lozinku naloga '{}'", user.getUsername());

        return toInfo(user);
    }

    /** Poslednji preostali ADMIN se ne brise — inace bi sistem ostao bez ijednog naloga za prijavu. */
    @Transactional
    public void delete(Long id) {
        AppUser user = getUserOrThrow(id);

        if (user.getRole() == UserRole.ADMIN && appUserRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Poslednji administratorski nalog se ne moze obrisati");
        }

        appUserRepository.delete(user);
        log.info("Obrisan nalog '{}'", user.getUsername());
    }

    private void validatePassword(String password) {
        int minLength = ruleProperties.minPasswordLengthOrDefault();
        if (password == null || password.length() < minLength) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Lozinka mora imati najmanje " + minLength + " karaktera");
        }
    }

    private AppUser getUserOrThrow(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nalog sa ID " + id + " ne postoji"));
    }

    private static UserInfo toInfo(AppUser user) {
        return new UserInfo(user.getAppUserID(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
