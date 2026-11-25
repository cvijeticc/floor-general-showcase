package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Faza sezone. Nosi parametre formata (tehnicka specifikacija poglavlje 4.4) — broj grupa,
 * velicinu grupe, broj krugova, direktan i dodatni prolaz, velicinu drveta.
 * Zbog ove tabele se 10 / 10 / 2 / 6 / 4 / 64 nikad ne pisu u kodu (odluka D-05).
 */
@Entity
@Table(name = "stages",
        uniqueConstraints = @UniqueConstraint(name = "uk_stages_season_order", columnNames = {"season_id", "order_no"}),
        indexes = @Index(name = "idx_stages_season", columnList = "season_id"))
@Getter
@Setter
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stage_id")
    private Long stageID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    /** "Grupna faza", "Playoff". */
    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageType type;

    /** Redosled faze u sezoni: 1, 2, 3. */
    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageStatus status = StageStatus.DRAFT;

    /** Broj grupa — samo za GROUP fazu. */
    private Integer groupCount;

    /** Velicina grupe — samo za GROUP fazu. */
    private Integer groupSize;

    /** 1 = jednokruzno, 2 = dvokruzno. */
    private Integer roundCount;

    /** BR-05 — prvih N iz svake grupe prolazi dalje. */
    private Integer directAdvance;

    /** BR-05 — najboljih M (N+1)-plasiranih prolazi dalje. */
    private Integer additionalAdvance;

    /** Velicina drveta — samo za KNOCKOUT fazu. */
    private Integer bracketSize;
}
