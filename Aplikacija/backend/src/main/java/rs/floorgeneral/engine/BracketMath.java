package rs.floorgeneral.engine;

/**
 * Raspored drveta playoff-a (BR-05, BR-09) — koja serija vodi u koju. Ciste funkcije.
 * <p>
 * Kolo 1 ima {@code bracketSize / 2} serija, svako sledece upola manje, do finala. Pobednik serije
 * na poziciji {@code p} ide u sledece kolo na poziciju {@code (p + 1) / 2}: kao prvi igrac ako je
 * {@code p} neparno, kao drugi ako je parno. Tako je M01 + M02 → M33 na kosturu lige (64 igraca:
 * M01–M32, M33–M48, M49–M56, M57–M60, M61–M62, M63). Porazeni iz polufinala idu u mec za 3. mesto.
 * <p>
 * Velicina drveta je parametar faze ({@code Stage.bracketSize}), nikad broj u kodu (CLAUDE.md §4).
 */
public final class BracketMath {

    private BracketMath() {
    }

    /** Da li je velicina drveta stepen dvojke i bar 2. */
    public static boolean isValidSize(int bracketSize) {
        return bracketSize >= 2 && Integer.bitCount(bracketSize) == 1;
    }

    /** Broj kola: 64 → 6 (Top 64, Top 32, Top 16, cetvrtfinale, polufinale, finale). */
    public static int roundCount(int bracketSize) {
        requireValid(bracketSize);
        return Integer.numberOfTrailingZeros(bracketSize);
    }

    /** Broj serija u kolu: kolo 1 od 64 → 32, finale → 1. */
    public static int seriesInRound(int bracketSize, int roundNo) {
        requireValid(bracketSize);
        if (roundNo < 1 || roundNo > roundCount(bracketSize)) {
            throw new IllegalArgumentException("Kolo " + roundNo + " ne postoji u drvetu od " + bracketSize);
        }
        return bracketSize >> roundNo;
    }

    /** Pozicija u sledecem kolu na koju ide pobednik pozicije {@code position}. */
    public static int nextPosition(int position) {
        return (position + 1) / 2;
    }

    /** {@code true} = pobednik ide kao prvi igrac (gornji red) serije u sledecem kolu. */
    public static boolean goesToFirstSlot(int position) {
        return position % 2 == 1;
    }

    /** Polufinale je kolo pre finala — odatle porazeni idu u mec za 3. mesto (BR-09). */
    public static boolean isSemiFinal(int bracketSize, int roundNo) {
        return roundNo == roundCount(bracketSize) - 1;
    }

    /**
     * Redni broj serije u celom drvetu, kao na kosturu lige: kolo 1 je 1…N/2, kolo 2 nastavlja dalje.
     * Mec za 3. mesto dobija poslednji broj ({@code bracketSize}).
     */
    public static int number(int bracketSize, int roundNo, int position, boolean thirdPlace) {
        if (thirdPlace) {
            return bracketSize;
        }
        int offset = 0;
        for (int round = 1; round < roundNo; round++) {
            offset += seriesInRound(bracketSize, round);
        }
        return offset + position;
    }

    /**
     * Podrazumevani naziv kola: „Top 64", „Top 32", „Top 16", pa „Četvrtfinale", „Polufinale", „Finale".
     * Admin ga moze promeniti na kolu (stage_rounds.name).
     */
    public static String defaultRoundName(int bracketSize, int roundNo) {
        int players = 2 * seriesInRound(bracketSize, roundNo);
        return switch (players) {
            case 2 -> "Finale";
            case 4 -> "Polufinale";
            case 8 -> "Četvrtfinale";
            default -> "Top " + players;
        };
    }

    private static void requireValid(int bracketSize) {
        if (!isValidSize(bracketSize)) {
            throw new IllegalArgumentException("Velicina drveta mora biti stepen dvojke, a ne " + bracketSize);
        }
    }
}
