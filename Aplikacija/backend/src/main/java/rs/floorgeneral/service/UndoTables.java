package rs.floorgeneral.service;

/** Tabele i primarni kljucevi koje undo dira — da se nazivi ne prepisuju po servisima. */
public final class UndoTables {

    public static final String PLAYERS = "players";
    public static final String PLAYER_ID = "player_id";

    public static final String GROUPS = "groups";
    public static final String GROUP_ID = "group_id";

    public static final String GROUP_ENTRIES = "group_entries";
    public static final String GROUP_ENTRY_ID = "group_entry_id";

    public static final String MATCHES = "matches";
    public static final String MATCH_ID = "match_id";

    public static final String STAGE_ROUNDS = "stage_rounds";
    public static final String STAGE_ROUND_ID = "stage_round_id";

    public static final String SERIES = "series";
    public static final String SERIES_ID = "series_id";

    public static final String APPLICATIONS = "applications";
    public static final String APPLICATION_ID = "application_id";

    private UndoTables() {
    }
}
