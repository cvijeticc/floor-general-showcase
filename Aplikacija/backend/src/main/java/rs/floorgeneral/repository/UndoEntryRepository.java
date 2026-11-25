package rs.floorgeneral.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.floorgeneral.domain.UndoEntry;

public interface UndoEntryRepository extends JpaRepository<UndoEntry, Long> {

    /** Vrh steka — poslednja izmena koja jos nije ponistena. */
    Optional<UndoEntry> findFirstByUndoneFalseOrderByUndoEntryIDDesc();

    List<UndoEntry> findByUndoneFalseOrderByUndoEntryIDDesc(Pageable pageable);
}
