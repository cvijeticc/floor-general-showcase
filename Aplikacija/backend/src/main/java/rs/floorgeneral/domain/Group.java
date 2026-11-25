package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PMOV: Grupa — pripada Fazi (od V3, ranije Sezoni), sadrzi prijave igraca i meceve. */
@Entity
@Table(name = "groups",
        uniqueConstraints = @UniqueConstraint(name = "uk_groups_stage_name", columnNames = {"stage_id", "name"}))
@Getter
@Setter
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    /** Oznaka grupe: "Grupa A". */
    @Column(nullable = false, length = 40)
    private String name;
}
