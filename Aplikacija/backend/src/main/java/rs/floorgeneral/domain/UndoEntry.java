package rs.floorgeneral.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Jedna admin izmena koja se moze ponistiti (UC-ADM-23) — stek po principu „poslednja prva".
 * <p>
 * Nije deo PMOV modela: ovo je infrastruktura, ne domen. {@code changes} nosi JSON sa prethodnim
 * stanjem svakog dirnutog reda; oblik je opisan u migraciji {@code V8__undo_entries.sql}.
 */
@Entity
@Table(name = "undo_entries", indexes = {
        @Index(name = "idx_undo_entries_open", columnList = "undone, undo_entry_id")
})
@Getter
@Setter
public class UndoEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "undo_entry_id")
    private Long undoEntryID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UndoAction action;

    /** Tekst koji admin vidi na dugmetu: „Brisanje igraca Lazar Ristic". */
    @Column(nullable = false, length = 300)
    private String description;

    @Column(name = "performed_by", length = 60)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(nullable = false, columnDefinition = "text")
    private String changes;

    @Column(nullable = false)
    private boolean undone;

    @Column(name = "undone_at")
    private LocalDateTime undoneAt;
}
