package rs.floorgeneral.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import rs.floorgeneral.domain.AppUser;
import rs.floorgeneral.repository.AppUserRepository;
import rs.floorgeneral.security.JwtProperties;
import rs.floorgeneral.security.TokenService;
import rs.floorgeneral.web.dto.auth.LoginRequest;
import rs.floorgeneral.web.dto.auth.LoginResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AppUserRepository appUserRepository;
    private final JwtProperties jwtProperties;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String token = tokenService.generateToken(authentication);
        AppUser appUser = appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Autentifikovan korisnik nije pronadjen u bazi"));

        return new LoginResponse(
                token,
                "Bearer",
                jwtProperties.expiration().toSeconds(),
                appUser.getUsername(),
                appUser.getRole().name()
        );
    }
}
