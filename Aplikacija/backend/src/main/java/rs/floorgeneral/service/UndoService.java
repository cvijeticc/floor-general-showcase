package rs.floorgeneral.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.UndoAction;
import rs.floorgeneral.domain.UndoEntry;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.repository.GroupRepository;
import rs.floorgeneral.repository.UndoEntryRepository;
import rs.floorgeneral.web.dto.undo.UndoEntryResult;
import rs.floorgeneral.web.dto.undo.UndoInfo;

/**
 * UC-ADM-23 — ponistavanje admin izmena („Ponisti", Ctrl+Z).
 * <p>
 * <b>Kako radi.</b> Servis koji nesto menja otvori {@link UndoDraft}, zapamti stanje redova koje
 * dira i sacuva ga u <i>istoj transakciji</i> kao i samu izmenu. Ponistavanje uzima poslednji
 * nepotroseni zapis i vraca redove u zapamceno stanje kroz {@link TableRows}.
 * <p>
 * <b>Zasto samo vrh steka.</b> Vracanje ide obrnutim redosledom od unosa, pa se nikad ne desi da
 * se vrati red koji zavisi od necega sto je u medjuvremenu obrisano (mec bez igraca, prijava bez
 * grupe). Starija izmena zato ceka da se ponisti sve novije — kao Ctrl+Z u uredjivacu teksta.
 * <p>
 * <b>Sta se ne pamti.</b> Tabela {@code standings} je izvedena (FR-6.5.1): posle vracanja redova
 * se preracunava iz meceva, pa joj snimak nije potreban.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UndoService {

    private final UndoEntryRepository undoEntryRepository;
    private final TableRows tableRows;
    private final StandingsService standingsService;
    private final GroupRepository groupRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    /** Pocetak zapisa o jednoj admin radnji; opis je tekst koji admin vidi na dugmetu. */
    public UndoDraft draft(UndoAction action, String description) {
        return new UndoDraft(this, action, description);
    }

    /** Vrh steka — sta ce dugme uraditi sledece; prazno kad nema sta da se ponisti. */
    public Optional<UndoEntryResult> lastUndoable() {
        return undoEntryRepository.findFirstByUndoneFalseOrderByUndoEntryIDDesc().map(UndoService::toResult);
    }

    /** Poslednjih {@code limit} izmena koje se mogu ponistiti, od najnovije. */
    public List<UndoEntryResult> recent(int limit) {
        return undoEntryRepository.findByUndoneFalseOrderByUndoEntryIDDesc(PageRequest.of(0, limit)).stream()
                .map(UndoService::toResult)
                .toList();
    }

    /**
     * Ponistava poslednju izmenu; ponovljen poziv ide korak dublje kroz stek.
     * Kad nema sta da se ponisti vraca 409, a ne tih prolaz — da UI ne bi javio da je nesto vraceno.
     */
    @Transactional
    public UndoInfo undoLast() {
        // Sve sto ceka u Hibernate sesiji mora u bazu pre nego sto krene vracanje kroz JDBC.
        entityManager.flush();

        UndoEntry entry = undoEntryRepository.findFirstByUndoneFalseOrderByUndoEntryIDDesc()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Nema izmene koja se moze ponistiti"));

        Changes changes = readChanges(entry);

        List<RowSnapshot> rows = changes.rows();
        for (int index = rows.size() - 1; index >= 0; index--) {
            tableRows.restore(rows.get(index));
        }

        // Redovi su promenjeni mimo Hibernate-a, pa sve sto je sesija ucitala vise ne vazi.
        entityManager.clear();

        for (Long groupId : changes.recomputeGroupIds()) {
            if (groupRepository.existsById(groupId)) {
                standingsService.recompute(groupId);
            }
        }

        Long undoneId = entry.getUndoEntryID();
        markUndone(undoneId);

        log.info("Ponisteno: {} (zapis {})", entry.getDescription(), undoneId);

        return new UndoInfo(undoneId, entry.getDescription(), lastUndoable().orElse(null));
    }

    /** Zove ga {@link UndoDraft#save()} — u transakciji same izmene, pa su nerazdvojivi. */
    void save(UndoAction action, String description, List<RowSnapshot> rows, List<Long> recomputeGroupIds) {
        if (rows.isEmpty()) {
            return;
        }

        UndoEntry entry = new UndoEntry();
        entry.setAction(action);
        entry.setDescription(trimmed(description));
        entry.setPerformedBy(currentUsername());
        entry.setPerformedAt(LocalDateTime.now());
        entry.setChanges(writeChanges(new Changes(rows, recomputeGroupIds)));

        undoEntryRepository.save(entry);
    }

    /** Snimak reda; {@code null} kad reda nema, pa se takav zapis preskace. */
    RowSnapshot capture(RowSnapshot.RowOp op, String table, String pkColumn, Object pkValue) {
        entityManager.flush();
        return tableRows.capture(op, table, pkColumn, pkValue);
    }

    /** Zapis je posle {@code clear()} odvojen od sesije, pa se dovlaci ponovo da bi se sacuvao. */
    private void markUndone(Long id) {
        undoEntryRepository.findById(id).ifPresent(reloaded -> {
            reloaded.setUndone(true);
            reloaded.setUndoneAt(LocalDateTime.now());
        });
    }

    private String writeChanges(Changes changes) {
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (Exception ex) {
            throw new IllegalStateException("Undo zapis se ne moze serijalizovati", ex);
        }
    }

    private Changes readChanges(UndoEntry entry) {
        try {
            return objectMapper.readValue(entry.getChanges(), new TypeReference<Changes>() {
            });
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Zapis o izmeni je ostecen i ne moze se ponistiti (zapis " + entry.getUndoEntryID() + ")");
        }
    }

    private static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private static String trimmed(String description) {
        return description.length() <= 300 ? description : description.substring(0, 297) + "...";
    }

    private static UndoEntryResult toResult(UndoEntry entry) {
        return new UndoEntryResult(entry.getUndoEntryID(), entry.getAction(), entry.getDescription(),
                entry.getPerformedBy(), entry.getPerformedAt());
    }

    /** Sadrzaj kolone {@code changes} — oblik je opisan u migraciji V8. */
    record Changes(List<RowSnapshot> rows, List<Long> recomputeGroupIds) {
    }
}
