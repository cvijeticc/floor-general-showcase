package rs.floorgeneral.engine;

/**
 * Odigran mec iz ugla jednog igraca — ulaz u {@link PlayerForm}. Bez JPA i bez datuma:
 * hronologiju nosi redosled u listi (zapisnici sa terena nemaju datum, pa servis redja po kolu).
 *
 * @param forfeit mec je zavrsen sluzbenim rezultatom (predaja ili nedolazak, BR-10.2) — racuna se
 *                u formu i nizove, ali ne i u rekorde: 11:0 bez igre nije „najubedljivija pobeda"
 */
public record PlayerGame(
        long matchId,
        long opponentId,
        int scored,
        int conceded,
        boolean forfeit
) {

    /** Nereseno ne postoji (BR-11, zlatni kos), pa je vise poena uvek pobeda. */
    public boolean won() {
        return scored > conceded;
    }

    /** Razlika iz ugla igraca: +5 za 11:6, −3 za 8:11. */
    public int margin() {
        return scored - conceded;
    }
}
