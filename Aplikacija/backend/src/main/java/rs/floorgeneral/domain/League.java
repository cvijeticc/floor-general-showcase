package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PMOV: Liga. */
@Entity
@Table(name = "leagues")
@Getter
@Setter
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "league_id")
    private Long leagueID;

    @Column(nullable = false, length = 100)
    private String name;
}
