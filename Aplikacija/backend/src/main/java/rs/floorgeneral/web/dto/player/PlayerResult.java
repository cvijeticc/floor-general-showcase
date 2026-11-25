package rs.floorgeneral.web.dto.player;

import rs.floorgeneral.domain.PlayerStatus;

/** Stavka liste/pretrage igraca — sve sto tabela na ekranu „Igraci" (UC-FE-05) prikazuje. */
public record PlayerResult(
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
        PlayerStatus status
) {
}
