package rs.floorgeneral.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Application;
import rs.floorgeneral.domain.ApplicationStatus;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.domain.SkillLevel;
import rs.floorgeneral.domain.Standing;
import rs.floorgeneral.repository.ApplicationRepository;
import rs.floorgeneral.repository.GroupEntryRepository;
import rs.floorgeneral.repository.MatchRepository;
import rs.floorgeneral.repository.PlayerRepository;
import rs.floorgeneral.repository.StandingRepository;

/**
 * D-20 — cela baza lige u jednom .xlsx fajlu: igraci, mecevi, tabele, prijave.
 * Baza je izvor istine; Excel je kopija koja se uvek pravi iznova, pa brisanje fajla nista ne kosta.
 * Pravi backup baze je zaseban (pg_dump u S3 + EBS snapshot, Infrastruktura/HANDOVER.md).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    private static final String DATE = "dd.MM.yyyy";
    private static final String DATE_TIME = "dd.MM.yyyy HH:mm";

    private final PlayerRepository playerRepository;
    private final GroupEntryRepository groupEntryRepository;
    private final MatchRepository matchRepository;
    private final StandingRepository standingRepository;
    private final ApplicationRepository applicationRepository;

    public byte[] exportWorkbook() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Workbook workbook = new Workbook(out, "Floor General", "1.0");
            writePlayers(workbook.newWorksheet("Igrači"));
            writeMatches(workbook.newWorksheet("Mečevi"));
            writeStandings(workbook.newWorksheet("Tabele"));
            writeApplications(workbook.newWorksheet("Prijave"));
            workbook.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("Excel izvoz nije uspeo", e);
        }
        return out.toByteArray();
    }

    private void writePlayers(Worksheet ws) {
        Map<Long, String> groupsByPlayer = groupEntryRepository.findAll().stream()
                .collect(Collectors.groupingBy(ge -> ge.getPlayer().getPlayerID(),
                        Collectors.mapping(ge -> ge.getGroup().getName(), Collectors.joining(", "))));

        header(ws, "ID", "Ime", "Prezime", "Nadimak", "Grupa", "Godište", "Telefon", "Visina (cm)",
                "Težina (kg)", "Uplata", "Napomena", "Status");
        int r = 1;
        for (Player p : playerRepository.findAll(Sort.by("lastName", "firstName"))) {
            int c = 0;
            cell(ws, r, c++, p.getPlayerID());
            cell(ws, r, c++, p.getFirstName());
            cell(ws, r, c++, p.getLastName());
            cell(ws, r, c++, p.getNickname());
            cell(ws, r, c++, groupsByPlayer.get(p.getPlayerID()));
            cell(ws, r, c++, p.getBirthYear());
            cell(ws, r, c++, p.getPhone());
            cell(ws, r, c++, p.getHeightCm());
            cell(ws, r, c++, p.getWeightKg());
            cell(ws, r, c++, p.getPaymentNote());
            cell(ws, r, c++, p.getNote());
            cell(ws, r, c, playerStatus(p.getStatus()));
            r++;
        }
        widths(ws, 6, 14, 16, 16, 10, 9, 15, 11, 11, 12, 40, 12);
    }

    private void writeMatches(Worksheet ws) {
        header(ws, "ID", "Faza", "Grupa", "Kolo", "Termin", "Teren", "Igrač 1", "Poeni 1", "Poeni 2",
                "Igrač 2", "Pobednik", "Status", "Napomena");
        // Sortira se ovde, ne u upitu: sort po group.name bi napravio inner join i izbacio
        // playoff meceve, koji nemaju grupu.
        List<Match> matches = matchRepository.findAll().stream()
                .sorted(Comparator.comparingInt((Match m) -> m.getStage().getOrderNo())
                        .thenComparing(m -> m.getGroup() == null ? "" : m.getGroup().getName(),
                                PublicSiteService::compareNatural)
                        .thenComparingInt(Match::getRoundNumber)
                        .thenComparing(Match::getScheduledStart, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Match::getMatchID))
                .toList();
        int r = 1;
        for (Match m : matches) {
            int c = 0;
            cell(ws, r, c++, m.getMatchID());
            cell(ws, r, c++, m.getStage().getName());
            cell(ws, r, c++, m.getGroup() == null ? null : m.getGroup().getName());
            cell(ws, r, c++, m.getRoundNumber());
            if (m.getScheduledStart() != null) {
                ws.value(r, c, m.getScheduledStart());
                ws.style(r, c).format(DATE_TIME).set();
            }
            c++;
            cell(ws, r, c++, m.getCourt() == null ? null : m.getCourt().getName());
            cell(ws, r, c++, m.getPlayer1().getDisplayName());
            cell(ws, r, c++, m.getScorePlayer1());
            cell(ws, r, c++, m.getScorePlayer2());
            cell(ws, r, c++, m.getPlayer2().getDisplayName());
            cell(ws, r, c++, m.getWinner() == null ? null : m.getWinner().getDisplayName());
            cell(ws, r, c++, matchStatus(m.getStatus()));
            cell(ws, r, c, m.getNote());
            r++;
        }
        widths(ws, 6, 14, 10, 6, 17, 10, 28, 8, 8, 28, 28, 14, 40);
    }

    private void writeStandings(Worksheet ws) {
        header(ws, "Grupa", "Poz.", "Igrač", "Odigrano", "Pobede", "Porazi", "Dati poeni", "Primljeni poeni",
                "Koš-razlika", "Bodovi");
        List<Standing> rows = standingRepository.findAll().stream()
                .sorted(Comparator.comparing((Standing s) -> s.getGroup().getStage().getOrderNo())
                        .thenComparing(s -> s.getGroup().getName(), PublicSiteService::compareNatural)
                        .thenComparingInt(Standing::getPosition))
                .toList();
        int r = 1;
        for (Standing s : rows) {
            int c = 0;
            cell(ws, r, c++, s.getGroup().getName());
            cell(ws, r, c++, s.getPosition());
            cell(ws, r, c++, s.getPlayer().getDisplayName());
            cell(ws, r, c++, s.getPlayed());
            cell(ws, r, c++, s.getWins());
            cell(ws, r, c++, s.getLosses());
            cell(ws, r, c++, s.getPointsFor());
            cell(ws, r, c++, s.getPointsAgainst());
            cell(ws, r, c++, s.getPointDifference());
            cell(ws, r, c, s.getPoints());
            r++;
        }
        widths(ws, 10, 6, 30, 9, 8, 8, 10, 14, 11, 8);
    }

    private void writeApplications(Worksheet ws) {
        header(ws, "Datum prijave", "Ime i prezime", "Nadimak", "Telefon", "Email", "Godište", "Visina (cm)",
                "Težina (kg)", "Košarkaško znanje", "Napomena", "O sebi", "Status");
        int r = 1;
        for (Application a : applicationRepository.findAllByOrderByApplicationIDDesc()) {
            int c = 0;
            ws.value(r, c, a.getCreatedAt());
            ws.style(r, c++).format(DATE_TIME).set();
            cell(ws, r, c++, a.getFullName());
            cell(ws, r, c++, a.getNickname());
            cell(ws, r, c++, a.getPhone());
            cell(ws, r, c++, a.getEmail());
            cell(ws, r, c++, a.getBirthYear());
            cell(ws, r, c++, a.getHeightCm());
            cell(ws, r, c++, a.getWeightKg());
            cell(ws, r, c++, skill(a.getSkillLevel()));
            cell(ws, r, c++, a.getNote());
            cell(ws, r, c++, a.getAbout());
            cell(ws, r, c, applicationStatus(a.getStatus()));
            r++;
        }
        widths(ws, 17, 24, 16, 15, 26, 9, 11, 11, 18, 40, 40, 12);
    }

    private static void header(Worksheet ws, String... titles) {
        for (int c = 0; c < titles.length; c++) {
            ws.value(0, c, titles[c]);
        }
        ws.range(0, 0, 0, titles.length - 1).style().bold().fillColor("E8E8E8").set();
        ws.freezePane(0, 1);
    }

    private static void widths(Worksheet ws, int... widths) {
        for (int c = 0; c < widths.length; c++) {
            ws.width(c, widths[c]);
        }
    }

    private static void cell(Worksheet ws, int r, int c, Object value) {
        if (value instanceof String s) {
            ws.value(r, c, s);
        } else if (value instanceof Number n) {
            ws.value(r, c, n);
        }
    }

    private static String playerStatus(PlayerStatus status) {
        return switch (status) {
            case ACTIVE -> "Aktivan";
            case WITHDRAWN -> "Odustao";
            case DISQUALIFIED -> "Isključen";
        };
    }

    private static String matchStatus(MatchStatus status) {
        return switch (status) {
            case SCHEDULED -> "Zakazan";
            case ANNOUNCED -> "Najavljen";
            case IN_PROGRESS -> "U toku";
            case FINISHED -> "Završen";
            case FORFEIT -> "Predaja";
            case WALKOVER -> "Službeni rezultat";
            case DOUBLE_FORFEIT -> "Obostrani službeni rezultat";
            case POSTPONED -> "Odložen";
            case CANCELLED -> "Otkazan";
        };
    }

    private static String skill(SkillLevel level) {
        return switch (level) {
            case BEGINNER -> "Početnik";
            case TRAINED_BEFORE -> "Trenirao sam pre";
            case PRO -> "Profi";
        };
    }

    private static String applicationStatus(ApplicationStatus status) {
        return switch (status) {
            case NEW -> "Nova";
            case CONTACTED -> "Kontaktiran";
            case ACCEPTED -> "Primljen";
            case REJECTED -> "Odbijen";
        };
    }
}
