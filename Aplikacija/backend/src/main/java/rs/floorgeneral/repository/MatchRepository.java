package rs.floorgeneral.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;

public interface MatchRepository extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {

    /** Igrac sa mecom se ne brise (UC-ADM-04) — umesto brisanja mu se menja status. */
    boolean existsByPlayer1_PlayerIDOrPlayer2_PlayerID(Long player1ID, Long player2ID);

    boolean existsByStage_StageID(Long stageID);

    /** Mecevi igraca u zadatim statusima — odustanak otkazuje one koji jos nisu odigrani (BR-10). */
    @Query("""
            select m from Match m
            where (m.player1.playerID = :playerId or m.player2.playerID = :playerId)
              and m.status in :statuses
            """)
    List<Match> findForPlayerInStatuses(@Param("playerId") Long playerId,
                                        @Param("statuses") Collection<MatchStatus> statuses);

    /** Za „faza ima vec odigrane meceve" — prosledjuje se MatchStatus.hasResult() skup. */
    boolean existsByStage_StageIDAndStatusIn(Long stageID, Collection<MatchStatus> statuses);

    boolean existsByGroup_GroupID(Long groupID);

    List<Match> findByGroup_GroupID(Long groupID);

    /** Ima li igrac ijedan mec u toj fazi — uklanjanje iz grupe je tada zabranjeno (UC-ADM-07). */
    @Query("""
            select count(m) > 0 from Match m
            where m.stage.stageID = :stageId
              and (m.player1.playerID = :playerId or m.player2.playerID = :playerId)
            """)
    boolean existsInStageForPlayer(@Param("stageId") Long stageId, @Param("playerId") Long playerId);

    /** Koliko puta je igrac u toj fazi kaznjen nedolaskom — BR-10.4, prag za predlog iskljucenja. */
    @Query("""
            select count(m) from Match m
            where m.stage.stageID = :stageId
              and m.status = rs.floorgeneral.domain.MatchStatus.WALKOVER
              and (m.player1.playerID = :playerId or m.player2.playerID = :playerId)
              and m.winner.playerID <> :playerId
            """)
    long countWalkoversAgainstPlayer(@Param("stageId") Long stageId, @Param("playerId") Long playerId);

    /** Mecevi jednog igraca u istom terminu — prost preklop iz UC-ADM-08. */
    @Query("""
            select m from Match m
            where m.scheduledStart = :slot
              and (m.player1.playerID = :playerId or m.player2.playerID = :playerId)
              and m.status not in (rs.floorgeneral.domain.MatchStatus.CANCELLED,
                                   rs.floorgeneral.domain.MatchStatus.POSTPONED)
            """)
    List<Match> findAtSlotForPlayer(@Param("slot") LocalDateTime slot, @Param("playerId") Long playerId);

    /** Mecevi na istom terenu u istom terminu. */
    @Query("""
            select m from Match m
            where m.scheduledStart = :slot
              and m.court.courtID = :courtId
              and m.status not in (rs.floorgeneral.domain.MatchStatus.CANCELLED,
                                   rs.floorgeneral.domain.MatchStatus.POSTPONED)
            """)
    List<Match> findAtSlotOnCourt(@Param("slot") LocalDateTime slot, @Param("courtId") Long courtId);

    /** Mecevi serije playoff-a po redu (1, 2, 3). */
    List<Match> findBySeries_SeriesIDOrderByGameNumberAsc(Long seriesId);

    /** Mecevi svih serija faze — za drvo, u jednom upitu. */
    @Query("""
            select m from Match m
              join fetch m.series s
              left join fetch m.court
            where m.stage.stageID = :stageId
            order by s.seriesID, m.gameNumber
            """)
    List<Match> findSeriesGamesOfStage(@Param("stageId") Long stageId);

    /**
     * Javni mecevi sezone (UC-PUB-06, UC-PUB-10): isti uslovi kao javni raspored — faza nije u
     * pripremi, mec nije otkazan — ali sa igracima, grupom i terenom u istom upitu (bez N+1).
     * Redosled je hronologija: zapisnici nemaju datum, pa vreme nose faza, kolo i ID meca.
     */
    @Query("""
            select m from Match m
              join fetch m.player1
              join fetch m.player2
              left join fetch m.group
              left join fetch m.court
              left join fetch m.series ser
              left join fetch ser.round
            where m.stage.season.seasonID = :seasonId
              and m.stage.status <> rs.floorgeneral.domain.StageStatus.DRAFT
              and m.status <> rs.floorgeneral.domain.MatchStatus.CANCELLED
            order by m.stage.orderNo, m.roundNumber, m.scheduledStart, m.matchID
            """)
    List<Match> findPublicInSeason(@Param("seasonId") Long seasonId);

    /** Isto, za jednog igraca — javni profil. */
    @Query("""
            select m from Match m
              join fetch m.player1
              join fetch m.player2
              left join fetch m.group
              left join fetch m.court
              left join fetch m.series ser
              left join fetch ser.round
            where m.stage.season.seasonID = :seasonId
              and (m.player1.playerID = :playerId or m.player2.playerID = :playerId)
              and m.stage.status <> rs.floorgeneral.domain.StageStatus.DRAFT
              and m.status <> rs.floorgeneral.domain.MatchStatus.CANCELLED
            order by m.stage.orderNo, m.roundNumber, m.scheduledStart, m.matchID
            """)
    List<Match> findPublicInSeasonForPlayer(@Param("seasonId") Long seasonId, @Param("playerId") Long playerId);
}
