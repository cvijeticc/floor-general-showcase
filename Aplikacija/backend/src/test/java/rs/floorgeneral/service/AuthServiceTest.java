package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import rs.floorgeneral.domain.AppUser;
import rs.floorgeneral.domain.UserRole;
import rs.floorgeneral.repository.AppUserRepository;
import rs.floorgeneral.security.JwtProperties;
import rs.floorgeneral.security.TokenService;
import rs.floorgeneral.web.dto.auth.LoginRequest;
import rs.floorgeneral.web.dto.auth.LoginResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_validCredentials_returnsTokenAndUserInfo() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        when(tokenService.generateToken(authentication)).thenReturn("jwt-token-123");
        when(jwtProperties.expiration()).thenReturn(Duration.ofHours(1));

        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setRole(UserRole.ADMIN);
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        LoginResponse response = authService.login(new LoginRequest("admin", "admin123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token-123");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    void login_badCredentials_propagatesAuthenticationException() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Pogresna lozinka"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "pogresno")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
