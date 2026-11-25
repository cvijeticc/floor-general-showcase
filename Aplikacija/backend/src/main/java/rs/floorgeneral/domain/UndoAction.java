package rs.floorgeneral.domain;

/**
 * Vrsta admin izmene koja se moze ponistiti (UC-ADM-23). Sluzi za ikonicu i grupisanje u UI-ju;
 * sam postupak ponistavanja je isti za sve — vraca se prethodno stanje dirnutih redova.
 */
public enum UndoAction {

    PLAYER_CREATE,
    PLAYER_UPDATE,
    PLAYER_STATUS,
    PLAYER_DELETE,

    GROUP_CREATE,
    GROUP_UPDATE,
    GROUP_DELETE,
    GROUP_ASSIGN,
    GROUP_REMOVE,

    MATCH_CREATE,
    MATCH_UPDATE,
    MATCH_POSTPONE,
    MATCH_DELETE,

    RESULT_ENTER,
    RESULT_BULK,
    RESULT_CORRECT,
    RESULT_FORFEIT,

    PLAYOFF_BRACKET,
    PLAYOFF_ROUND,
    PLAYOFF_GAME,
    PLAYOFF_WALKOVER,
    PLAYOFF_PLAYERS,
    PLAYOFF_SCHEDULE,

    APPLICATION_STATUS,
    APPLICATION_DELETE,
    APPLICATION_ACCEPT
}
