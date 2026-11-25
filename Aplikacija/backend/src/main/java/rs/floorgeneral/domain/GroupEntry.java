package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Prijava igraca u grupu. Zamenjuje nekadasnje polje Player.group — bez ove veze igrac bi mogao
 * biti u tacno jednoj grupi ikada, pa sezona sa dve grupne faze (odluka D-04) ne bi bila moguca.
 * Baza cuva jedinstvenost (group_id, player_id); da igrac nije u dve grupe ISTE faze proverava servis.
 */
@Entity
@Table(name = "group_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_entries_group_player", columnNames = {"group_id", "player_id"}),
        indexes = @Index(name = "idx_group_entries_player", columnList = "player_id"))
@Getter
@Setter
public class GroupEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_entry_id")
    private Long groupEntryID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
}
