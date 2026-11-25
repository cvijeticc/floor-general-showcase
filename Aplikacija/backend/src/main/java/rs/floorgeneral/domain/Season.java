package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * PMOV: Sezona. Pored atributa sa dijagrama nosi i parametre bodovanja —
 * nijedan broj se ne hardkoduje u kodu (BR-01, tehnicka specifikacija poglavlje 4).
 */
@Entity
@Table(name = "seasons")
@Getter
@Setter
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "season_id")
    private Long seasonID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(nullable = false, length = 100)
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    /** Dijagram: brojIgraca. */
    @Column(nullable = false)
    private int playerCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeasonStatus status = SeasonStatus.DRAFT;

    /** BR-01 — bodovanje. */
    @Column(nullable = false)
    private int pointsForWin = 2;

    @Column(nullable = false)
    private int pointsForLoss = 1;

    @Column(nullable = false)
    private int pointsForForfeit = 0;

    /** BR-10.2 — sluzbeni rezultat 0:11. */
    @Column(nullable = false)
    private int forfeitWinnerScore = 11;

    @Column(nullable = false)
    private int forfeitLoserScore = 0;

    /**
     * BR-11 — granica posle koje mec moze da se zavrsi pre isteka vremena.
     * <b>Nije obavezan rezultat</b> (odluka D-18): mec se igra {@link #durationMinutes} minuta
     * i pobedjuje onaj ko na kraju ima vise poena, pa je i 2:1 ispravan rezultat.
     */
    @Column(nullable = false)
    private int targetScore = 11;

    /**
     * Najveci moguci rezultat. Veci je od {@link #targetScore} jer sut spolja vredi dva poena:
     * ko vodi 10:7 i pogodi spolja zavrsava 12:7 (odluka D-18).
     */
    @Column(nullable = false)
    private int maxScore = 12;

    /** Trajanje meca u minutima; posle isteka pobedjuje onaj ko ima vise poena (odluka D-18). */
    @Column(nullable = false)
    private int durationMinutes = 8;
}
