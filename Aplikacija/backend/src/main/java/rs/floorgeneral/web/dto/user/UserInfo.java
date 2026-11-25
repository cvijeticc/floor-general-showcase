package rs.floorgeneral.web.dto.user;

import rs.floorgeneral.domain.UserRole;

/** Detalj jednog naloga. Lozinka (ni hash) se NIKAD ne vraca kroz API. */
public record UserInfo(
        Long id,
        String username,
        String email,
        UserRole role
) {
}
