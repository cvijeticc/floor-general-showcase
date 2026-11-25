import type { MatchStatus, PublicMatch, PublicPlayer } from "@/lib/site-api";

/** Statusi sa rezultatom (BR-10.1) — isto kao MatchStatus.hasResult() na backendu. */
const WITH_RESULT: ReadonlySet<MatchStatus> = new Set([
  "FINISHED",
  "FORFEIT",
  "WALKOVER",
  "DOUBLE_FORFEIT",
]);

/** Predstojeći: zakazan, najavljen, u toku ili odložen (otkazane backend ni ne šalje). */
const UPCOMING: ReadonlySet<MatchStatus> = new Set(["SCHEDULED", "ANNOUNCED", "IN_PROGRESS", "POSTPONED"]);

export type MatchView = {
  match: PublicMatch;
  opponent: PublicPlayer;
  /** Rezultat iz ugla igrača čiji je profil — null dok meč nije odigran. */
  scored: number | null;
  conceded: number | null;
  won: boolean;
};

/** Meč iz ugla jednog igrača: on je uvek levo, protivnik desno. */
export function viewFor(match: PublicMatch, playerId: number): MatchView {
  const first = match.player1.id === playerId;
  return {
    match,
    opponent: first ? match.player2 : match.player1,
    scored: first ? match.scorePlayer1 : match.scorePlayer2,
    conceded: first ? match.scorePlayer2 : match.scorePlayer1,
    won: match.winnerId === playerId,
  };
}

export function isPlayed(match: PublicMatch): boolean {
  return WITH_RESULT.has(match.status) && match.scorePlayer1 !== null && match.scorePlayer2 !== null;
}

export function isUpcoming(match: PublicMatch): boolean {
  return UPCOMING.has(match.status);
}
