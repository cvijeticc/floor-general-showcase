package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** PMOV: Mec — dva igraca, odigran na terenu. Pripada fazi; grupi samo ako je grupni (spec 7). */
@Entity
@Table(name = "matches", indexes = {
        @Index(name = "idx_matches_group_round", columnList = "group_id, round_number"),
        @Index(name = "idx_matches_stage_round", columnList = "stage_id, round_number"),
        @Index(name = "idx_matches_player1", columnList = "player1_id"),
        @Index(name = "idx_matches_player2", columnList = "player2_id"),
        @Index(name = "idx_matches_scheduled_start", columnList = "scheduled_start"),
        @Index(name = "idx_matches_court_start", columnList = "court_id, scheduled_start")
})
@Getter
@Setter
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long matchID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    /** null za playoff mec — tamo mec pripada seriji, ne grupi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    /** Kolo unutar grupe, odnosno unutar faze. */
    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player1_id", nullable = false)
    private Player player1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player2_id", nullable = false)
    private Player player2;

    private Integer scorePlayer1;

    private Integer scorePlayer2;

    /** Izvodi se iz rezultata, ne unosi se rucno. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SCHEDULED;

    /** Termin: teren + datum i vreme pocetka. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id")
    private Court court;

    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;

    /** Razlog sluzbenog rezultata, odlaganja ili korekcije. */
    @Column(length = 500)
    private String note;

    /** Serija playoff-a kojoj mec pripada (V17); null za grupni mec. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private Series series;

    /** Redni broj meca u seriji: 1, 2, 3. */
    @Column(name = "game_number")
    private Integer gameNumber;
}
