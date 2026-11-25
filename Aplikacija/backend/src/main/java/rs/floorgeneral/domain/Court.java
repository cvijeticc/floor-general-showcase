package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PMOV: Teren. */
@Entity
@Table(name = "courts")
@Getter
@Setter
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "court_id")
    private Long courtID;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 160)
    private String address;
}
