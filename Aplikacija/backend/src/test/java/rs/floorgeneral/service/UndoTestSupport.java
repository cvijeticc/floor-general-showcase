package rs.floorgeneral.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import rs.floorgeneral.domain.UndoAction;

/**
 * Ponistavanje (UC-ADM-23) je u servisima sporedna radnja — unit test proverava poslovno pravilo,
 * ne zapis u steku. Zato lazni {@link UndoService} ovde vraca pravi {@link UndoDraft}, koji nad
 * laznim servisom ne uradi nista: snimak reda je {@code null} pa se preskoci, a {@code save()}
 * ne upisuje nista. Da se ponistavanje zaista vraca stanje proverava {@code UndoControllerIT}.
 */
final class UndoTestSupport {

    static void stubUndo(UndoService undoService) {
        lenient().when(undoService.draft(any(UndoAction.class), any()))
                .thenAnswer(call -> new UndoDraft(undoService, call.getArgument(0), call.getArgument(1)));
    }

    private UndoTestSupport() {
    }
}
