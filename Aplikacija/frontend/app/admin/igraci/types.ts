import type { components } from "@/lib/api/schema";

export type PlayerResult = components["schemas"]["PlayerResult"];
export type PlayerInfo = components["schemas"]["PlayerInfo"];
export type CreatePlayerCmd = components["schemas"]["CreatePlayerCmd"];
export type UpdatePlayerCmd = components["schemas"]["UpdatePlayerCmd"];
export type GroupResult = components["schemas"]["GroupResult"];
export type PlayerStatus = NonNullable<PlayerResult["status"]>;

/** BR-10 — statusi igraca. Tekst je na srpskom, vrednost ostaje engleska (CLAUDE.md §5). */
export const STATUS_LABELS: Record<PlayerStatus, string> = {
  ACTIVE: "Aktivan",
  WITHDRAWN: "Odustao",
  DISQUALIFIED: "Diskvalifikovan",
};

export const STATUS_ORDER: PlayerStatus[] = ["ACTIVE", "WITHDRAWN", "DISQUALIFIED"];
