import type { components } from "@/lib/api/schema";

export type MatchRow = components["schemas"]["MatchResult"];
export type MatchInfo = components["schemas"]["MatchInfo"];
export type CreateMatchCmd = components["schemas"]["CreateMatchCmd"];
export type UpdateMatchCmd = components["schemas"]["UpdateMatchCmd"];
export type BulkResultsInfo = components["schemas"]["BulkResultsInfo"];
export type MatchStatus = NonNullable<MatchRow["status"]>;

/** BR-10.1 — svih osam statusa. Vrednost ostaje engleska, tekst je na srpskom (CLAUDE.md §5). */
export const STATUS_LABELS: Record<MatchStatus, string> = {
  SCHEDULED: "Zakazan",
  ANNOUNCED: "Najavljen",
  IN_PROGRESS: "U toku",
  FINISHED: "Odigran",
  FORFEIT: "Predaja",
  WALKOVER: "Nedolazak",
  DOUBLE_FORFEIT: "Obojica kažnjena",
  POSTPONED: "Odložen",
  CANCELLED: "Otkazan",
};

export const STATUS_ORDER: MatchStatus[] = [
  "SCHEDULED",
  "ANNOUNCED",
  "IN_PROGRESS",
  "FINISHED",
  "FORFEIT",
  "WALKOVER",
  "DOUBLE_FORFEIT",
  "POSTPONED",
  "CANCELLED",
];

/** Isto pravilo kao MatchStatus.hasResult() na backendu. */
export function hasResult(status?: MatchStatus): boolean {
  return status === "FINISHED" || status === "FORFEIT" || status === "WALKOVER" || status === "DOUBLE_FORFEIT";
}

/** „2026-09-19T10:00:00" → „19.09.2026. 10:00" (CLAUDE.md §4 — format datuma). */
export function formatSlot(iso?: string | null): string {
  if (!iso) {
    return "—";
  }
  const [date, time] = iso.split("T");
  const [year, month, day] = date.split("-");
  return `${day}.${month}.${year}. ${(time ?? "").slice(0, 5)}`;
}

/** Datum i vreme iz dva polja forme u ono sto backend ocekuje (LocalDateTime). */
/**
 * Danasnji datum po satu uredjaja, u obliku koji trazi <input type="date"> (yyyy-MM-dd).
 * Namerno ne toISOString() -- on vraca UTC, pa bi posle ponoci datum bio jucerasnji.
 */
export function todayLocalDate(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export function toIsoSlot(date: string, time: string): string | undefined {
  if (!date) {
    return undefined;
  }
  return `${date}T${time || "00:00"}:00`;
}

/** Suprotan smer — termin meca natrag u dva polja forme. */
export function splitSlot(iso?: string | null): { date: string; time: string } {
  if (!iso) {
    return { date: "", time: "" };
  }
  const [date, time] = iso.split("T");
  return { date, time: (time ?? "").slice(0, 5) };
}
