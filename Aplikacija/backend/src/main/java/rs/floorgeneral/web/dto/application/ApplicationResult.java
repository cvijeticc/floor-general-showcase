package rs.floorgeneral.web.dto.application;

import java.time.LocalDateTime;
import rs.floorgeneral.domain.ApplicationStatus;
import rs.floorgeneral.domain.SkillLevel;

/** Stavka liste prijava u admin panelu; {@code playerId} je popunjen kad je prijava primljena. */
public record ApplicationResult(
        Long id,
        String fullName,
        String nickname,
        Integer heightCm,
        Integer weightKg,
        Integer birthYear,
        String phone,
        String email,
        SkillLevel skillLevel,
        String note,
        String about,
        ApplicationStatus status,
        Long playerId,
        LocalDateTime createdAt
) {
}
