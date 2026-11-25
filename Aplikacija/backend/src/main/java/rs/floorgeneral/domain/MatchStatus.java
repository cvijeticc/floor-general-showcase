package rs.floorgeneral.domain;

/**
 * BR-10.1 (8 statusa) + obostrani sluzbeni rezultat (D-23):
 * FORFEIT = predaja igraca, WALKOVER = sluzbeni rezultat zbog nedolaska,
 * DOUBLE_FORFEIT = obojica predala ili se nijedan nije pojavio — oba igraca dobijaju poraz
 * sa {@code pointsForForfeit} bodova, rezultat je {@code forfeitLoserScore}:{@code forfeitLoserScore}, bez pobednika.
 */
public enum MatchStatus {
    SCHEDULED, ANNOUNCED, IN_PROGRESS, FINISHED, FORFEIT, WALKOVER, DOUBLE_FORFEIT, POSTPONED, CANCELLED;

    public boolean hasResult() {
        return this == FINISHED || isOfficial();
    }

    /** Sluzbeni rezultat (BR-10.2): racuna se u tabelu i formu, ali ne i u poene i rekorde. */
    public boolean isOfficial() {
        return this == FORFEIT || this == WALKOVER || this == DOUBLE_FORFEIT;
    }
}
