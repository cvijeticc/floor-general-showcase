package rs.floorgeneral.web.dto.user;

import rs.floorgeneral.domain.UserRole;

/** Stavka liste naloga. Lozinka (ni hash) se NIKAD ne vraca kroz API. */
public record UserResult(
        Long id,
        String username,
        String email,
        UserRole role
) {
}
