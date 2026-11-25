package rs.floorgeneral.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Kolo playoff faze (V17): naziv, broj potrebnih pobeda u seriji i format meca (BR-08).
 * Prazan format znaci format sezone ({@code Season.targetScore}, {@code maxScore}, {@code durationMinutes});
 * Final Four ga ima svoj — do 16 poena ili 15 minuta.
 */
@Entity
@Table(name = "stage_rounds",
        uniqueConstraints = @UniqueConstraint(name = "uk_stage_rounds_stage_round", columnNames = {"stage_id", "round_no"}))
@Getter
@Setter
public class StageRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stage_round_id")
    private Long stageRoundID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    /** 1 = prvo kolo drveta. */
    @Column(name = "round_no", nullable = false)
    private int roundNo;

    /** „Top 64", „Četvrtfinale", „Finale". */
    @Column(nullable = false, length = 60)
    private String name;

    /** Pobeda potrebnih za prolaz: 2 = serija na dve pobede, 1 = jedna utakmica. */
    @Column(name = "wins_required", nullable = false)
    private int winsRequired = 1;

    private Integer targetScore;

    private Integer maxScore;

    private Integer durationMinutes;

    /** Najveci moguci rezultat u ovom kolu — sopstveni, inace iz sezone (D-18). */
    public int effectiveMaxScore() {
        return maxScore != null ? maxScore : stage.getSeason().getMaxScore();
    }

    public int effectiveTargetScore() {
        return targetScore != null ? targetScore : stage.getSeason().getTargetScore();
    }

    public int effectiveDurationMinutes() {
        return durationMinutes != null ? durationMinutes : stage.getSeason().getDurationMinutes();
    }
}
