package rs.floorgeneral.service;

import java.util.Map;

/**
 * Stanje jednog reda tabele, onako kako se pamti za ponistavanje (UC-ADM-23).
 * <p>
 * Vrednosti se cuvaju kao tekst — bas onako kako ih baza ispisuje — a uz njih i naziv tipa kolone,
 * pa se pri vracanju upisuju kroz {@code cast(? as tip)}. Zahvaljujuci tome ovde nema nijednog
 * imena entiteta: isti kod vraca i igraca, i mec, i prijavu u grupu.
 *
 * @param op       {@code CREATED} — red je napravljen, ponistava se brisanjem;
 *                 {@code CHANGED} — {@code values} nosi stanje PRE izmene ili brisanja
 * @param values   sve kolone reda; {@code null} vrednost kolone ostaje {@code null}
 * @param types    naziv tipa svake kolone iz baze ({@code int8}, {@code varchar}, {@code timestamp}…)
 */
public record RowSnapshot(
        RowOp op,
        String table,
        String pkColumn,
        String pkValue,
        Map<String, String> values,
        Map<String, String> types
) {

    public enum RowOp {
        CREATED,
        CHANGED
    }
}
