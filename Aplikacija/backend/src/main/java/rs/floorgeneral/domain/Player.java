package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PMOV: Igrac — sifarnik lige; pripadnost grupi ide kroz GroupEntry (od V3). */
@Entity
@Table(name = "players")
@Getter
@Setter
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long playerID;

    @Column(nullable = false, length = 60)
    private String firstName;

    @Column(nullable = false, length = 60)
    private String lastName;

    /**
     * Nadimak je obavezan (V5, D-16): liga igrace vodi i objavljuje po nadimku — CSV sa
     * spiskom grupa ima jednu kolonu „Ime i nadimak", a na javnom sajtu je nadimak ono
     * sto posetilac prepoznaje.
     */
    @Column(nullable = false, length = 60)
    private String nickname;

    /** Prava baza lige ima samo godiste, ne i pun datum rodjenja (V5, D-16). */
    private Integer birthYear;

    @Column(length = 30)
    private String phone;

    private Integer heightCm;

    private Integer weightKg;

    /**
     * Prepisana kolona UPLATA iz postojece evidencije lige („6500din", „Free", „1500/3000din").
     * Obican tekst — modul placanja je Faza 3 i van MVP-a (CLAUDE.md §3.4).
     */
    @Column(length = 40)
    private String paymentNote;

    /** Kolona NAPOMENA — uglavnom raspolozivost igraca, koristi se pri zakazivanju termina. */
    @Column(length = 500)
    private String note;

    /** BR-10 — odustanak i diskvalifikacija. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayerStatus status = PlayerStatus.ACTIVE;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Onako kako liga igraca zove i kako stoji na spisku grupa: „Lazar Ristić Laki". */
    public String getDisplayName() {
        return nickname == null || nickname.isBlank()
                ? getFullName()
                : getFullName() + " " + nickname;
    }
}
