package rs.floorgeneral.web.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String username,
        String role
) {
}
