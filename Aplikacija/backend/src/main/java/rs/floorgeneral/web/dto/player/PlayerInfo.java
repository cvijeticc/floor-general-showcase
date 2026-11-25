package rs.floorgeneral.web.dto.player;

import rs.floorgeneral.domain.PlayerStatus;

/** Detaljan pregled jednog igraca (GET po ID-ju, kreiranje, izmena). */
public record PlayerInfo(
        Long id,
        String firstName,
        String lastName,
        String nickname,
        String fullName,
        String displayName,
        Integer birthYear,
        String phone,
        Integer heightCm,
        Integer weightKg,
        String paymentNote,
        String note,
        PlayerStatus status
) {
}
