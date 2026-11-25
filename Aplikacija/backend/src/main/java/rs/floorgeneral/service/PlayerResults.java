package rs.floorgeneral.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.engine.PlayerForm;
import rs.floorgeneral.engine.PlayerGame;
import rs.floorgeneral.web.dto.site.PublicFormItem;

/**
 * Odigrani mecevi iz ugla igraca — most izmedju JPA meceva i ciste {@link PlayerForm} (UC-PUB-06).
 * Zajednicko za spisak igraca (i kolonu „Forma" u tabelama na sajtu) i profil, da forma svuda
 * izgleda isto.
 * <p>
 * Ulaz je uvek hronoloski ({@link #chronological}): faza, pa u grupnoj fazi datum (od uvoza sa
 * turniriukraju.com, V18, vecina meceva ga ima), pa kolo i ID meca. Zvanicno kolo grupne faze
 * (raspored lige) nije redosled igranja — mecevi su se igrali i van rasporeda.
 */
final class PlayerResults {

    /** Forma u tabeli i na spisku igraca — poslednjih 5, kao na SofaScore-u. Prikaz, ne format takmicenja. */
    static final int SHORT_FORM = 5;

    /** Forma na profilu — poslednjih 10 (zahtev korisnika 08.10.2026.). */
    static final int PROFILE_FORM = 10;

    private PlayerResults() {
    }

    /**
     * Hronologija meceva: faza po redu; u grupnoj fazi prvo datum (mec bez datuma — sluzbeni
     * rezultat igraca koji je odustao — ide kao stariji), pa kolo, termin i ID. U playoff-u kolo
     * drveta vec jeste hronologija, pa datum ne sme da ga preskoci.
     */
    static List<Match> chronological(List<Match> matches) {
        Comparator<Match> order = Comparator
                .comparingInt((Match m) -> m.getStage().getOrderNo())
                .thenComparing(m -> m.getStage().getType() == StageType.GROUP ? m.getScheduledStart() : null,
                        Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()))
                .thenComparingInt(Match::getRoundNumber)
                .thenComparing(Match::getScheduledStart, Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()))
                .thenComparing(Match::getMatchID);
        List<Match> sorted = new ArrayList<>(matches);
        sorted.sort(order);
        return sorted;
    }

    /** Mecevi sa rezultatom, po igracu, u istom (hronoloskom) redosledu kao ulaz. */
    static Map<Long, List<Match>> byPlayer(List<Match> chronological) {
        Map<Long, List<Match>> byPlayer = new HashMap<>();
        for (Match match : chronological) {
            if (hasResult(match)) {
                byPlayer.computeIfAbsent(match.getPlayer1().getPlayerID(), id -> new ArrayList<>()).add(match);
                byPlayer.computeIfAbsent(match.getPlayer2().getPlayerID(), id -> new ArrayList<>()).add(match);
            }
        }
        return byPlayer;
    }

    /** Isto pravilo kao tabela grupe (StandingsService): status sa rezultatom i upisan rezultat. */
    static boolean hasResult(Match match) {
        return match.getStatus().hasResult() && match.getScorePlayer1() != null && match.getScorePlayer2() != null;
    }

    static List<PlayerGame> games(List<Match> results, long playerId) {
        return results.stream().map(match -> game(match, playerId)).toList();
    }

    /** Poslednjih {@code size} meceva, od starijeg ka novijem. */
    static List<PublicFormItem> form(List<Match> results, long playerId, int size) {
        return PlayerForm.last(results, size).stream().map(match -> formItem(match, playerId)).toList();
    }

    private static PlayerGame game(Match match, long playerId) {
        boolean first = match.getPlayer1().getPlayerID() == playerId;
        return new PlayerGame(
                match.getMatchID(),
                opponent(match, playerId).getPlayerID(),
                first ? match.getScorePlayer1() : match.getScorePlayer2(),
                first ? match.getScorePlayer2() : match.getScorePlayer1(),
                // Isto pravilo kao tabela (StandingsService): predaja, nedolazak i obostrani
                // sluzbeni rezultat (D-23, 0:0 — poraz za oba) su sluzbeni rezultat.
                match.getStatus().isOfficial());
    }

    /** Opis jednog meca forme za DTO — za najubedljiviju pobedu na spisku igraca. */
    static PublicFormItem formItem(List<Match> results, long playerId, long matchId) {
        return results.stream()
                .filter(match -> match.getMatchID() == matchId)
                .findFirst()
                .map(match -> formItem(match, playerId))
                .orElseThrow();
    }

    private static PublicFormItem formItem(Match match, long playerId) {
        PlayerGame game = game(match, playerId);
        return new PublicFormItem(match.getMatchID(), match.getRoundNumber(), game.won(), game.scored(),
                game.conceded(), PublicSiteService.toPlayer(opponent(match, playerId)), match.getStatus(),
                PublicSiteService.roundNameOf(match));
    }

    static Player opponent(Match match, long playerId) {
        return match.getPlayer1().getPlayerID() == playerId ? match.getPlayer2() : match.getPlayer1();
    }
}
