package rs.floorgeneral.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Prijava u ligu sa javnog sajta (D-20, migracija V16). Nije igrac dok je admin ne primi. */
@Entity
@Table(name = "applications")
@Getter
@Setter
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationID;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 60)
    private String nickname;

    private Integer heightCm;

    private Integer weightKg;

    private Integer birthYear;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkillLevel skillLevel;

    /** „Napomene" — uglavnom kada igrac ne moze da igra; prelazi u Player.note. */
    @Column(length = 500)
    private String note;

    /** „Nešto više o sebi" (opciono). */
    @Column(length = 500)
    private String about;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
