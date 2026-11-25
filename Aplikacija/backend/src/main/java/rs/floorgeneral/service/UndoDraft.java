package rs.floorgeneral.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import rs.floorgeneral.domain.UndoAction;

/**
 * Skuplja sve sto je potrebno da se jedna admin radnja ponisti (UC-ADM-23), pa se na kraju
 * upisuje kao jedan zapis u stek.
 * <p>
 * <b>Redosled je bitan:</b> {@link #changed} se zove PRE izmene (pamti staro stanje),
 * a {@link #created} POSLE upisa (tek tada red ima kljuc). Masovni unos rezultata je jedan zapis
 * sa 60 redova — jedno „Ponisti" vraca celo kolo.
 *
 * <pre>
 * undoService.draft(UndoAction.PLAYER_DELETE, "Brisanje igraca " + player.getDisplayName())
 *         .changed(UndoTables.PLAYERS, UndoTables.PLAYER_ID, id)
 *         .save();
 * playerRepository.delete(player);
 * </pre>
 */
public final class UndoDraft {

    private final UndoService undoService;
    private final UndoAction action;
    private String description;
    private final List<RowSnapshot> rows = new ArrayList<>();
    private final Set<Long> recomputeGroupIds = new LinkedHashSet<>();

    UndoDraft(UndoService undoService, UndoAction action, String description) {
        this.undoService = undoService;
        this.action = action;
        this.description = description;
    }

    /** Pamti red onakav kakav je SADA — zove se pre izmene ili pre brisanja. */
    public UndoDraft changed(String table, String pkColumn, Object pkValue) {
        return add(undoService.capture(RowSnapshot.RowOp.CHANGED, table, pkColumn, pkValue));
    }

    /** Red koji je upravo napravljen — ponistavanje ga brise. Zove se posle upisa. */
    public UndoDraft created(String table, String pkColumn, Object pkValue) {
        return add(undoService.capture(RowSnapshot.RowOp.CREATED, table, pkColumn, pkValue));
    }

    /** Vec uzet snimak (masovni unos ga uzima red po red, pa zadrzava samo uspesne). */
    public UndoDraft changed(RowSnapshot snapshot) {
        return add(snapshot);
    }

    /** Opis koji se zna tek na kraju radnje — npr. koliko je rezultata zaista upisano. */
    public UndoDraft describe(String description) {
        this.description = description;
        return this;
    }

    /** Tabela grupe je izvedena, pa se posle ponistavanja preracunava (BR-12.3). */
    public UndoDraft recompute(Long groupId) {
        if (groupId != null) {
            recomputeGroupIds.add(groupId);
        }
        return this;
    }

    /** Upisuje zapis u istu transakciju u kojoj je i sama izmena — ili oboje prodje, ili nista. */
    public void save() {
        undoService.save(action, description, rows, List.copyOf(recomputeGroupIds));
    }

    private UndoDraft add(RowSnapshot snapshot) {
        if (snapshot != null) {
            rows.add(snapshot);
        }
        return this;
    }
}
