package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * PMOV: Tabela — jedan red tabele grupe za jednog igraca.
 * Materijalizovana; preracunava se posle svake izmene rezultata, idempotentno (BR-12.3).
 */
@Entity
@Table(name = "standings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "player_id"}),
        indexes = @Index(name = "idx_standings_group_position", columnList = "group_id, position_in_group"))
@Getter
@Setter
public class Standing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "standing_id")
    private Long standingID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /** Odigrano meceva. */
    @Column(nullable = false)
    private int played;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    /** Dati poeni (DP). */
    @Column(nullable = false)
    private int pointsFor;

    /** Primljeni poeni (PP). */
    @Column(nullable = false)
    private int pointsAgainst;

    /** Kos-razlika (KR = DP - PP). */
    @Column(nullable = false)
    private int pointDifference;

    /** Bodovi. */
    @Column(nullable = false)
    private int points;

    /** Mesto u tabeli. */
    @Column(name = "position_in_group", nullable = false)
    private int position;

    /** FR-5.1 — kriterijum BR-03 kojim je igrac odvojen od izjednacenih; null kad izjednacenja nije bilo. */
    @Column(name = "tie_break", length = 40)
    private String tieBreak;
}
