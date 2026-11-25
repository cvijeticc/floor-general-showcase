package rs.floorgeneral.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Application;
import rs.floorgeneral.domain.ApplicationStatus;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.domain.UndoAction;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.ApplicationRepository;
import rs.floorgeneral.repository.PlayerRepository;
import rs.floorgeneral.service.mail.ApplicationSubmittedEvent;
import rs.floorgeneral.web.dto.application.ApplicationResult;
import rs.floorgeneral.web.dto.application.ChangeApplicationStatusCmd;
import rs.floorgeneral.web.dto.application.CreateApplicationCmd;
import rs.floorgeneral.web.mapper.ApplicationMapper;

/**
 * D-20 — prijave u ligu sa javnog sajta. Prijem je javan (bez prijave), sve ostalo radi admin;
 * svaka admin izmena ide na undo stek (D-17).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final PlayerRepository playerRepository;
    private final ApplicationMapper applicationMapper;
    private final UndoService undoService;
    private final ApplicationEventPublisher events;

    public List<ApplicationResult> search(ApplicationStatus status) {
        List<Application> applications = status == null
                ? applicationRepository.findAllByOrderByApplicationIDDesc()
                : applicationRepository.findByStatusOrderByApplicationIDDesc(status);
        return applications.stream().map(applicationMapper::toResult).toList();
    }

    /**
     * Prijem sa forme. Popunjena zamka ({@code website}) znaci bot: odgovor je isti kao za pravu
     * prijavu, da bot ne bi naucio razliku, ali se nista ne upisuje.
     */
    @Transactional
    public void submit(CreateApplicationCmd cmd) {
        if (cmd.website() != null && !cmd.website().isBlank()) {
            log.info("Prijava odbacena kao spam (popunjeno skriveno polje)");
            return;
        }
        Application application = applicationMapper.toEntity(cmd);
        application.setFullName(cmd.fullName().trim());
        application.setNickname(cmd.nickname().trim());
        application.setPhone(cmd.phone().trim());
        application.setEmail(cmd.email().trim());
        application.setNote(blankToNull(cmd.note()));
        application.setAbout(blankToNull(cmd.about()));
        application.setCreatedAt(LocalDateTime.now());
        applicationRepository.save(application);
        // Mejl ligi ide tek posle commit-a (ApplicationMailer) — neuspeo mejl ne poništava prijavu.
        events.publishEvent(new ApplicationSubmittedEvent(application.getApplicationID()));
    }

    @Transactional
    public ApplicationResult changeStatus(Long id, ChangeApplicationStatusCmd cmd) {
        Application application = getApplicationOrThrow(id);
        if (cmd.status() == ApplicationStatus.ACCEPTED && application.getPlayer() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Prijava se prima pravljenjem igraca (dugme „Napravi igraca\")");
        }

        undoService.draft(UndoAction.APPLICATION_STATUS,
                        "Status prijave " + application.getFullName() + " → " + cmd.status())
                .changed(UndoTables.APPLICATIONS, UndoTables.APPLICATION_ID, id)
                .save();

        application.setStatus(cmd.status());
        return applicationMapper.toResult(application);
    }

    /**
     * Pravi igraca iz prijave (ime, nadimak, godiste, telefon, visina, tezina, napomena) i prijavu
     * oznacava kao primljenu. Igrac se posle rasporedjuje u grupu kao i svaki drugi.
     * Jedno „Ponisti" brise igraca i vraca prijavu.
     */
    @Transactional
    public ApplicationResult accept(Long id) {
        Application application = getApplicationOrThrow(id);
        if (application.getPlayer() != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Iz ove prijave je vec napravljen igrac " + application.getPlayer().getDisplayName());
        }

        UndoDraft undo = undoService.draft(UndoAction.APPLICATION_ACCEPT,
                        "Igrac iz prijave " + application.getFullName())
                .changed(UndoTables.APPLICATIONS, UndoTables.APPLICATION_ID, id);

        String[] name = splitName(application.getFullName());
        Player player = new Player();
        player.setFirstName(name[0]);
        player.setLastName(name[1]);
        player.setNickname(application.getNickname());
        player.setBirthYear(application.getBirthYear());
        player.setPhone(application.getPhone());
        player.setHeightCm(application.getHeightCm());
        player.setWeightKg(application.getWeightKg());
        player.setNote(application.getNote());
        Player saved = playerRepository.save(player);

        undo.created(UndoTables.PLAYERS, UndoTables.PLAYER_ID, saved.getPlayerID()).save();

        application.setPlayer(saved);
        application.setStatus(ApplicationStatus.ACCEPTED);
        return applicationMapper.toResult(application);
    }

    @Transactional
    public void delete(Long id) {
        Application application = getApplicationOrThrow(id);
        undoService.draft(UndoAction.APPLICATION_DELETE, "Brisanje prijave " + application.getFullName())
                .changed(UndoTables.APPLICATIONS, UndoTables.APPLICATION_ID, id)
                .save();
        applicationRepository.delete(application);
    }

    /** „Petar Petrović" → [Petar, Petrović]; jedna rec ostaje ime, prezime se dopisuje rucno. */
    static String[] splitName(String fullName) {
        String trimmed = fullName.trim().replaceAll("\\s+", " ");
        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace < 0) {
            return new String[] {fit(trimmed), "-"};
        }
        return new String[] {fit(trimmed.substring(0, lastSpace)), fit(trimmed.substring(lastSpace + 1))};
    }

    /** Ime i prezime igraca su do 60 karaktera, a polje na formi do 120. */
    private static String fit(String value) {
        return value.length() <= 60 ? value : value.substring(0, 60);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Application getApplicationOrThrow(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prijava sa ID " + id + " ne postoji"));
    }
}
