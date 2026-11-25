package rs.floorgeneral.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Serija playoff-a („Tie", V17) — par u drvetu. Igraci stizu sami kad se zavrsi serija ispod
 * (engine/BracketMath); u prvom kolu ih postavlja kostur lige. Mecevi serije su redovi u
 * {@code matches} sa {@code series_id}; pobednik se cuva, jer serija moze biti resena i bez meca
 * (nedolazak — {@code walkover}).
 */
@Entity
@Table(name = "series",
        uniqueConstraints = @UniqueConstraint(name = "uk_series_round_position",
                columnNames = {"stage_round_id", "position", "third_place"}),
        indexes = @Index(name = "idx_series_round", columnList = "stage_round_id"))
@Getter
@Setter
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "series_id")
    private Long seriesID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_round_id", nullable = false)
    private StageRound round;

    /** Pozicija u kolu, odozgo nadole: M01 = 1. */
    @Column(nullable = false)
    private int position;

    /** Mec za 3. mesto — u kolu finala, igraju ga porazeni iz polufinala (BR-09). */
    @Column(name = "third_place", nullable = false)
    private boolean thirdPlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id")
    private Player player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id")
    private Player player2;

    /** Oznaka nosioca sa kostura, npr. „1. Gr. 1". */
    @Column(length = 40)
    private String seed1;

    @Column(length = 40)
    private String seed2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;

    /** Resena bez (svih) meceva — protivnik se nije pojavio ili je predao (BR-08, BR-10). */
    @Column(nullable = false)
    private boolean walkover;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id")
    private Court court;

    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;

    @Column(length = 500)
    private String note;

    public boolean hasBothPlayers() {
        return player1 != null && player2 != null;
    }
}
