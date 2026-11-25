import { BACKEND_URL } from "@/lib/api/config";

/**
 * Javni podaci za tab „Rezultati" (backend /api/v1/public/**, bez tokena). Čita se na serveru
 * (SSR, zbog Google-a — D-03) i kešira, da svaki posetilac ne udara u bazu. Izmenu iz admin
 * panela posetilac vidi odmah: proxy (app/api/backend) posle svake uspešne izmene briše oznaku
 * PUBLIC_CACHE_TAG. Rok od 60 s važi samo za izmene mimo panela (migracije).
 */
const REVALIDATE_SECONDS = 60;
export const PUBLIC_CACHE_TAG = "public";

export type MatchStatus =
  | "SCHEDULED"
  | "ANNOUNCED"
  | "IN_PROGRESS"
  | "FINISHED"
  | "FORFEIT"
  | "WALKOVER"
  | "DOUBLE_FORFEIT"
  | "POSTPONED"
  | "CANCELLED";

export type PublicPlayer = { id: number; name: string; nickname: string };

export type PublicGroup = {
  id: number;
  name: string;
  stageId: number;
  stageName: string;
  directAdvance: number | null;
  additionalAdvance: number | null;
};

export type PublicSeason = {
  id: number;
  name: string;
  leagueName: string;
  startDate: string | null;
  endDate: string | null;
  groups: PublicGroup[];
};

/** Jedan meč forme iz ugla igrača (UC-PUB-06): pobeda/poraz, rezultat, protivnik. */
export type PublicFormItem = {
  matchId: number;
  roundNumber: number;
  won: boolean;
  scored: number;
  conceded: number;
  opponent: PublicPlayer;
  status: MatchStatus;
  /** Naziv kola playoff-a („Top 64"); null za grupni meč. */
  roundName: string | null;
};

/** „3. kolo" za grupni meč, „Top 64" za playoff — svuda gde se meč opisuje jednom rečju. */
export function roundLabel(item: { roundNumber: number; roundName: string | null }): string {
  return item.roundName ?? `${item.roundNumber}. kolo`;
}

export type PublicStandingRow = {
  position: number;
  player: PublicPlayer;
  played: number;
  wins: number;
  losses: number;
  pointsFor: number;
  pointsAgainst: number;
  pointDifference: number;
  points: number;
  /** BR-03 kriterijum kojim je igrač odvojen od izjednačenih (FR-5.1); null bez izjednačenja. */
  tieBreak: TieBreakCriterion | null;
};

export type TieBreakCriterion =
  | "HEAD_TO_HEAD_WINS"
  | "HEAD_TO_HEAD_DIFFERENCE"
  | "MINI_TABLE_POINTS"
  | "MINI_TABLE_DIFFERENCE"
  | "MINI_TABLE_POINTS_FOR"
  | "GROUP_DIFFERENCE"
  | "GROUP_POINTS_FOR"
  | "UNRESOLVED";

/** Objašnjenje za tooltip u tabeli (FR-5.1) — igrači razdvojeni tie-break-om, kojim kriterijumom. */
export const TIE_BREAK_LABEL: Record<TieBreakCriterion, string> = {
  HEAD_TO_HEAD_WINS: "Izjednačeni po bodovima — odlučile su međusobne pobede",
  HEAD_TO_HEAD_DIFFERENCE: "Izjednačeni po bodovima i međusobnim pobedama — odlučila je koš-razlika međusobnih mečeva",
  MINI_TABLE_POINTS: "Više igrača sa istim bodovima — odlučili su bodovi iz njihovih međusobnih mečeva",
  MINI_TABLE_DIFFERENCE: "Više igrača sa istim bodovima — odlučila je koš-razlika iz njihovih međusobnih mečeva",
  MINI_TABLE_POINTS_FOR: "Više igrača sa istim bodovima — odlučili su dati poeni u njihovim međusobnim mečevima",
  GROUP_DIFFERENCE: "Izjednačeni i u međusobnim mečevima — odlučila je koš-razlika u celoj grupi",
  GROUP_POINTS_FOR: "Izjednačeni i u koš-razlici — odlučili su dati poeni u celoj grupi",
  UNRESOLVED: "Potpuno izjednačeni — redosled je privremen",
};

/** Igrač na javnom spisku (tab „Igrači", UC-PUB-10; lideri, UC-PUB-08). Samo statistika — odluka D-21. */
export type PublicPlayerSummary = {
  player: PublicPlayer;
  groupId: number;
  groupName: string;
  /** null dok tabela grupe nije preračunata. */
  position: number | null;
  played: number;
  wins: number;
  losses: number;
  pointsFor: number;
  pointsAgainst: number;
  pointDifference: number;
  points: number;
  /** Poslednjih 5 mečeva, od starijeg ka novijem. */
  form: PublicFormItem[];
  /** +3 = tri pobede zaredom, −2 = dva poraza zaredom. */
  currentStreak: number;
  longestWinStreak: number;
  /** Najubedljivija odigrana pobeda — službeni rezultat se ne računa. */
  biggestWin: PublicFormItem | null;
};

export type PublicHeadToHead = {
  opponent: PublicPlayer;
  played: number;
  wins: number;
  losses: number;
  pointsFor: number;
  pointsAgainst: number;
};

/** Profil igrača (UC-PUB-06). Bilans je red igrača u tabeli grupe; forma je poslednjih 10. */
export type PublicPlayerProfile = {
  player: PublicPlayer;
  group: PublicGroup | null;
  /** Cela tabela grupe — prazna dok nije preračunata. */
  groupStandings: PublicStandingRow[];
  form: PublicFormItem[];
  currentStreak: number;
  longestWinStreak: number;
  biggestWinMatchId: number | null;
  heaviestLossMatchId: number | null;
  /** Svi mečevi sezone, hronološki — odigrani i predstojeći. */
  matches: PublicMatch[];
  headToHead: PublicHeadToHead[];
};

export type PublicMatch = {
  id: number;
  groupId: number | null;
  groupName: string | null;
  roundNumber: number;
  player1: PublicPlayer;
  player2: PublicPlayer;
  scorePlayer1: number | null;
  scorePlayer2: number | null;
  winnerId: number | null;
  status: MatchStatus;
  courtName: string | null;
  scheduledStart: string | null;
  /** GROUP ili KNOCKOUT — playoff meč nema grupu nego kolo drveta i seriju. */
  stageType: "GROUP" | "KNOCKOUT";
  /** „Top 64", „Finale", „Za 3. mesto"; null za grupni meč. */
  roundName: string | null;
  seriesId: number | null;
  /** Redni broj meča u seriji (1, 2, 3). */
  gameNumber: number | null;
};

/* ---------------------------------- Playoff (UC-PLA) ---------------------------------- */

export type SeriesStatus = "PENDING" | "READY" | "IN_PROGRESS" | "FINISHED";

/** Meč serije; score1 je uvek rezultat prvog igrača serije. */
export type PublicGame = {
  matchId: number;
  gameNumber: number;
  score1: number;
  score2: number;
  winnerId: number | null;
  scheduledStart: string | null;
  courtName: string | null;
};

export type PublicSeries = {
  id: number;
  /** Redni broj na kosturu lige: M01…M63, meč za 3. mesto je poslednji. */
  number: number;
  position: number;
  thirdPlace: boolean;
  player1: PublicPlayer | null;
  player2: PublicPlayer | null;
  /** „1. Gr. 1" — mesto i grupa iz koje je igrač stigao (samo prvo kolo). */
  seed1: string | null;
  seed2: string | null;
  wins1: number;
  wins2: number;
  winnerId: number | null;
  /** Rešena službeno (nedolazak ili predaja). */
  walkover: boolean;
  status: SeriesStatus;
  scheduledStart: string | null;
  courtName: string | null;
  games: PublicGame[];
};

export type PublicRound = {
  roundNo: number;
  name: string;
  /** 2 = serija na dve pobede, 1 = jedna utakmica. */
  winsRequired: number;
  targetScore: number;
  maxScore: number;
  durationMinutes: number;
  series: PublicSeries[];
};

export type PublicBracket = {
  stageId: number;
  stageName: string;
  bracketSize: number;
  rounds: PublicRound[];
};

/* ---------------------------------- Statistika ---------------------------------- */

export type StatsPhase = "ALL" | "GROUP" | "KNOCKOUT";

/** Red taba „Statistika" — službeni rezultati nisu u odigranim mečevima ni u poenima. */
export type PublicStatRow = {
  player: PublicPlayer;
  groupId: number | null;
  groupName: string | null;
  played: number;
  wins: number;
  losses: number;
  winPercentage: number;
  pointsFor: number;
  pointsAgainst: number;
  pointDifference: number;
  averageFor: number;
  averageAgainst: number;
  closeWins: number;
  closeLosses: number;
  shutouts: number;
  officialWins: number;
  officialLosses: number;
};

/** „Nema ga" i „backend ne radi" nisu isto: prvo je strana 404, drugo poruka da se pokuša kasnije. */
export type ApiResult<T> = { status: "ok"; data: T } | { status: "not-found" } | { status: "error" };

async function request<T>(path: string): Promise<ApiResult<T>> {
  try {
    const response = await fetch(`${BACKEND_URL}/api/v1/public${path}`, {
      next: { revalidate: REVALIDATE_SECONDS, tags: [PUBLIC_CACHE_TAG] },
      headers: { Accept: "application/json" },
    });
    if (response.status === 404) return { status: "not-found" };
    if (!response.ok) return { status: "error" };
    return { status: "ok", data: (await response.json()) as T };
  } catch {
    // Backend nedostupan (npr. tokom deploy-a) — strana prikazuje poruku umesto greške.
    return { status: "error" };
  }
}

async function get<T>(path: string): Promise<T | null> {
  const result = await request<T>(path);
  return result.status === "ok" ? result.data : null;
}

export const getSeason = () => get<PublicSeason>("/season");

/** Igrači aktuelne sezone (backend bira istu sezonu kao /season), pa ide paralelno sa getSeason(). */
export const getPlayers = () => get<PublicPlayerSummary[]>("/players");

/** Profil u aktuelnoj sezoni; 404 = igrač ne igra u njoj (ili ne postoji). */
export const getPlayerProfile = (playerId: number) => request<PublicPlayerProfile>(`/players/${playerId}`);

export const getStandings = (groupId: number) => get<PublicStandingRow[]>(`/groups/${groupId}/standings`);

/** Drvo playoff-a aktuelne sezone; not-found dok playoff nije objavljen. */
export const getPlayoff = () => request<PublicBracket>("/playoff");

export const getStats = (phase: StatsPhase) => get<PublicStatRow[]>(`/stats?phase=${phase}`);

export function getMatches(seasonId: number, opts: { groupId?: number; played?: boolean } = {}) {
  const params = new URLSearchParams({ seasonId: String(seasonId) });
  if (opts.groupId) params.set("groupId", String(opts.groupId));
  if (opts.played !== undefined) params.set("played", String(opts.played));
  return get<PublicMatch[]>(`/matches?${params}`);
}

/** „2026-09-19T19:00:00" → „19.09.2026." — bez Date objekta, da vremenska zona servera ne pomeri dan. */
export function formatDate(iso: string | null): string {
  if (!iso) return "";
  const [y, m, d] = iso.slice(0, 10).split("-");
  return `${d}.${m}.${y}.`;
}

export function formatTime(iso: string | null): string {
  return iso && iso.length >= 16 ? iso.slice(11, 16) : "";
}

const DAYS = ["Nedelja", "Ponedeljak", "Utorak", "Sreda", "Četvrtak", "Petak", "Subota"];

export function formatDay(iso: string | null): string {
  if (!iso) return "Termin nije određen";
  const [y, m, d] = iso.slice(0, 10).split("-").map(Number);
  const day = DAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()];
  return `${day}, ${formatDate(iso)}`;
}
