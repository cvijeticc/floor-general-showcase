package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.floorgeneral.domain.AppUser;
import rs.floorgeneral.domain.UserRole;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.AppUserRepository;
import rs.floorgeneral.web.dto.user.ChangeOwnPasswordCmd;
import rs.floorgeneral.web.dto.user.CreateUserCmd;
import rs.floorgeneral.web.dto.user.ResetPasswordCmd;
import rs.floorgeneral.web.dto.user.UserInfo;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RuleProperties ruleProperties;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void create_storesBcryptHashAndNeverPlainPassword() {
        CreateUserCmd cmd = new CreateUserCmd("organizator", "tajnalozinka", "org@example.com", null);
        when(appUserRepository.findByUsername("organizator")).thenReturn(Optional.empty());
        when(ruleProperties.minPasswordLengthOrDefault()).thenReturn(8);
        when(passwordEncoder.encode("tajnalozinka")).thenReturn("$2a$hash");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserInfo info = appUserService.create(cmd);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(info.username()).isEqualTo("organizator");
    }

    @Test
    void create_duplicateUsername_throwsConflict() {
        CreateUserCmd cmd = new CreateUserCmd("admin", "tajnalozinka", null, null);
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user(1L, "admin")));

        assertThatThrownBy(() -> appUserService.create(cmd))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void create_tooShortPassword_throwsBadRequest() {
        CreateUserCmd cmd = new CreateUserCmd("organizator", "kratka", null, null);
        when(appUserRepository.findByUsername("organizator")).thenReturn(Optional.empty());
        when(ruleProperties.minPasswordLengthOrDefault()).thenReturn(8);

        assertThatThrownBy(() -> appUserService.create(cmd))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("8")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changeOwnPassword_withCorrectOldPassword_storesNewHash() {
        AppUser user = user(1L, "admin");
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("staralozinka", "$2a$old")).thenReturn(true);
        when(ruleProperties.minPasswordLengthOrDefault()).thenReturn(8);
        when(passwordEncoder.encode("novalozinka")).thenReturn("$2a$new");

        appUserService.changeOwnPassword("admin", new ChangeOwnPasswordCmd("staralozinka", "novalozinka"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$new");
    }

    @Test
    void changeOwnPassword_withWrongOldPassword_throwsBadRequest() {
        AppUser user = user(1L, "admin");
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pogresna", "$2a$old")).thenReturn(false);

        assertThatThrownBy(() -> appUserService.changeOwnPassword("admin",
                new ChangeOwnPasswordCmd("pogresna", "novalozinka")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Stara lozinka")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(user.getPasswordHash()).isEqualTo("$2a$old");
    }

    @Test
    void resetPassword_doesNotAskForOldPassword() {
        AppUser user = user(2L, "organizator");
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(ruleProperties.minPasswordLengthOrDefault()).thenReturn(8);
        when(passwordEncoder.encode("novalozinka")).thenReturn("$2a$reset");

        appUserService.resetPassword(2L, new ResetPasswordCmd("novalozinka"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$reset");
    }

    @Test
    void resetPassword_unknownUser_throwsResourceNotFoundException() {
        when(appUserRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.resetPassword(404L, new ResetPasswordCmd("novalozinka")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_lastAdmin_throwsConflict() {
        AppUser user = user(1L, "admin");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> appUserService.delete(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Poslednji")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(appUserRepository, never()).delete(any(AppUser.class));
    }

    @Test
    void delete_whenAnotherAdminExists_removesUser() {
        AppUser user = user(2L, "organizator");
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(appUserRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);

        appUserService.delete(2L);

        verify(appUserRepository).delete(user);
    }

    private static AppUser user(Long id, String username) {
        AppUser user = new AppUser();
        user.setAppUserID(id);
        user.setUsername(username);
        user.setPasswordHash("$2a$old");
        user.setRole(UserRole.ADMIN);
        return user;
    }
}
