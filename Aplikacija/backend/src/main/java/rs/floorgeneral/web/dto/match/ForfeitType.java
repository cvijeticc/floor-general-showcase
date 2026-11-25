package rs.floorgeneral.web.dto.match;

import rs.floorgeneral.domain.MatchStatus;

/**
 * BR-10.1 — nedolazak (WALKOVER) i predaja (FORFEIT) jednog igraca, i obostrani sluzbeni rezultat
 * (DOUBLE_FORFEIT, D-23): obojica su predala ili se nijedan nije pojavio. Samo ove statuse sme da postavi admin.
 */
public enum ForfeitType {

    WALKOVER(MatchStatus.WALKOVER),
    FORFEIT(MatchStatus.FORFEIT),
    DOUBLE_FORFEIT(MatchStatus.DOUBLE_FORFEIT);

    private final MatchStatus matchStatus;

    ForfeitType(MatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }

    public MatchStatus toMatchStatus() {
        return matchStatus;
    }

    /** Kod obostranog sluzbenog rezultata kazneni su obojica, pa se igrac ne bira. */
    public boolean punishesBoth() {
        return this == DOUBLE_FORFEIT;
    }
}
