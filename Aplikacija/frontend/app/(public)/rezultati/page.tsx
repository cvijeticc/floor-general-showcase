import type { Metadata } from "next";
import Link from "next/link";
import { CONTAINER } from "@/components/site/nav";
import { MatchRow } from "@/components/site/results/match-row";
import { PlayerSearch, type SearchOption } from "@/components/site/results/player-search";
import { PLAYER_SORTS, PlayersTab, type PlayerSortKey } from "@/components/site/results/players-tab";
import { Legend, StandingsTable } from "@/components/site/results/standings-table";
import { STAT_SORTS, STATS_PHASES, StatsTab, type StatSortKey } from "@/components/site/results/stats-tab";
import { mainSeries, seriesCode, shortWhen } from "@/lib/bracket";
import { playerThumb } from "@/lib/player-photos";
import { playerHref } from "@/lib/player-url";
import {
  formatDay,
  getMatches,
  getPlayers,
  getPlayoff,
  getSeason,
  getStandings,
  getStats,
  type PublicBracket,
  type PublicFormItem,
  type PublicGroup,
  type PublicMatch,
  type PublicPlayerSummary,
  type PublicSeason,
  type StatsPhase,
} from "@/lib/site-api";

export const metadata: Metadata = {
  title: "Rezultati, tabele, raspored, igrači i statistika",
  description:
    "Tabele grupa, rezultati, raspored, statistika i profili svih igrača Floor General 1 na 1 košarkaške lige — pobede, porazi, forma, proseci poena i koš-razlika, ažurirano posle svakog kola.",
};

const TABS = [
  { key: "tabele", label: "Tabele" },
  { key: "rezultati", label: "Rezultati" },
  { key: "raspored", label: "Raspored" },
  { key: "igraci", label: "Igrači" },
  { key: "statistika", label: "Statistika" },
] as const;

type TabKey = (typeof TABS)[number]["key"];

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

/** ?q=a&q=b daje niz — uzima se prvi, da strana ne padne na .trim() nad nizom. */
function one(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

/**
 * UC-PUB-02/04/05/10 — javni tab „Rezultati": tabele grupa, odigrani mečevi, raspored i igrači.
 * Svako ime vodi na profil igrača (UC-PUB-06), a „Pronađi igrača" je na svakom tabu.
 */
export default async function ResultsPage({ searchParams }: { searchParams: SearchParams }) {
  const params = await searchParams;
  const tabParam = one(params.tab);
  const tab: TabKey = TABS.some((t) => t.key === tabParam) ? (tabParam as TabKey) : "tabele";
  const sortParam = one(params.sort);
  const sort: PlayerSortKey = PLAYER_SORTS.some((s) => s.key === sortParam) ? (sortParam as PlayerSortKey) : "grupa";
  const query = (one(params.q) ?? "").trim().slice(0, 60);
  const phase: StatsPhase = STATS_PHASES.find((p) => p.param === one(params.faza))?.key ?? "ALL";
  const statSortParam = one(params.sort);
  const statSort: StatSortKey = STAT_SORTS.some((s) => s.key === statSortParam) ? (statSortParam as StatSortKey) : "dati";

  const [season, players, playoff] = await Promise.all([getSeason(), getPlayers(), getPlayoff()]);
  const bracket = playoff.status === "ok" ? playoff.data : null;
  const group = season?.groups.find((g) => String(g.id) === one(params.grupa));

  const searchOptions: SearchOption[] = (players ?? []).map((p) => ({
    id: p.player.id,
    nickname: p.player.nickname,
    name: p.player.name,
    groupName: p.groupName,
    href: playerHref(p.player),
    thumb: playerThumb(p.player),
  }));

  // Na tabu „Igrači" grupa i redosled ostaju kad se menja jedno od njih; na „Statistici" i faza.
  const extra = (s: PlayerSortKey): Record<string, string> => {
    if (tab === "statistika") return statParams(phase, statSort);
    return tab === "igraci" && s !== "grupa" ? { sort: s } : {};
  };

  return (
    <>
      <section className="bg-black pb-10 pt-12 font-site text-white xl:pb-14 xl:pt-[80px]">
        <div className={CONTAINER}>
          <h1 className="text-[42px] leading-[1.2] font-bold xl:text-[56px]">REZULTATI</h1>
          {season && (
            <p className="mt-2 text-lg text-white/70">
              {season.leagueName} · Sezona {season.name}
            </p>
          )}

          {/* Pretraga je u istom redu sa tabovima: na laptopu 14" crna traka ne sme da poraste. */}
          <div className="mt-8 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <nav aria-label="Prikaz" className="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
              {bracket && (
                <Link
                  href="/playoff"
                  className="col-span-2 inline-flex items-center justify-center gap-2 rounded-full bg-playoff px-5 py-2.5 text-base font-bold text-white transition-colors duration-300 hover:bg-playoff-dark sm:col-span-1"
                >
                  Playoff — drvo
                  <span aria-hidden>→</span>
                </Link>
              )}
              {TABS.map((t) => (
                <Link
                  key={t.key}
                  href={href(t.key, group?.id)}
                  aria-current={t.key === tab ? "page" : undefined}
                  className={`rounded-full px-5 py-2.5 text-center text-base font-bold transition-colors duration-300 last:col-span-2 sm:last:col-span-1 ${
                    t.key === tab ? "bg-white text-black" : "border border-white/40 text-white hover:bg-white/10"
                  }`}
                >
                  {t.label}
                </Link>
              ))}
            </nav>
            {searchOptions.length > 0 && (
              <PlayerSearch key={query} players={searchOptions} defaultValue={tab === "igraci" ? query : ""} />
            )}
          </div>

          {season && season.groups.length > 1 && (
            <nav aria-label="Grupa" className="-mx-5 mt-5 flex gap-2 overflow-x-auto px-5 pb-1 md:mx-0 md:flex-wrap md:px-0">
              <GroupChip label="Sve grupe" href={href(tab, undefined, extra(sort))} active={!group} />
              {season.groups.map((g) => (
                <GroupChip
                  key={g.id}
                  label={g.name}
                  href={href(tab, g.id, extra(sort))}
                  active={g.id === group?.id}
                />
              ))}
            </nav>
          )}
        </div>
      </section>

      <section className="min-h-[50vh] bg-white pb-24 pt-10 font-site text-black xl:pt-14">
        <div className={CONTAINER}>
          {!season ? (
            <Empty>Rezultati trenutno nisu dostupni. Pokušaj ponovo za par minuta.</Empty>
          ) : tab === "tabele" ? (
            <StandingsTab season={season} group={group} players={players} bracket={bracket} />
          ) : tab === "statistika" ? (
            <StatisticsTab
              group={group}
              phase={phase}
              sort={statSort}
              hrefFor={(p, so) => href("statistika", group?.id, statParams(p, so))}
            />
          ) : tab === "igraci" ? (
            players ? (
              <PlayersTab
                players={players}
                groups={season.groups}
                group={group}
                query={query}
                sort={sort}
                sortHref={(s) => href("igraci", group?.id, s === "grupa" ? {} : { sort: s })}
                clearHref={href("igraci", group?.id)}
              />
            ) : (
              <Empty>Igrači trenutno nisu dostupni. Pokušaj ponovo za par minuta.</Empty>
            )
          ) : (
            <MatchesTab season={season} group={group} played={tab === "rezultati"} bracket={bracket} />
          )}
        </div>
      </section>
    </>
  );
}

function href(tab: TabKey, groupId?: number, extra: Record<string, string> = {}) {
  const params = new URLSearchParams();
  if (tab !== "tabele") params.set("tab", tab);
  if (groupId) params.set("grupa", String(groupId));
  for (const [key, value] of Object.entries(extra)) params.set(key, value);
  const query = params.toString();
  return query ? `/rezultati?${query}` : "/rezultati";
}

/** Parametri taba „Statistika" u URL-u — podrazumevane vrednosti se ne pišu. */
function statParams(phase: StatsPhase, sort: StatSortKey): Record<string, string> {
  const params: Record<string, string> = {};
  const faza = STATS_PHASES.find((p) => p.key === phase)?.param;
  if (faza && phase !== "ALL") params.faza = faza;
  if (sort !== "dati") params.sort = sort;
  return params;
}

function GroupChip({ label, href, active }: { label: string; href: string; active: boolean }) {
  return (
    <Link
      href={href}
      aria-current={active ? "page" : undefined}
      className={`shrink-0 whitespace-nowrap rounded-full px-4 py-1.5 text-sm transition-colors duration-300 ${
        active ? "bg-white font-bold text-black" : "bg-white/10 text-white hover:bg-white/20"
      }`}
    >
      {label}
    </Link>
  );
}

function Empty({ children }: { children: React.ReactNode }) {
  return <p className="py-16 text-center text-lg text-black/60">{children}</p>;
}

/* ---------------------------------- Tabele ---------------------------------- */

async function StandingsTab({
  season,
  group,
  players,
  bracket,
}: {
  season: PublicSeason;
  group?: PublicGroup;
  players: PublicPlayerSummary[] | null;
  bracket: PublicBracket | null;
}) {
  const groups = group ? [group] : season.groups;
  const tables = await Promise.all(groups.map(async (g) => ({ group: g, rows: await getStandings(g.id) })));

  if (groups.length === 0) return <Empty>Grupe još nisu objavljene.</Empty>;

  // Kolona „Forma" dolazi sa spiska igrača, koji strana ionako učitava zbog pretrage.
  const forms = players ? new Map<number, PublicFormItem[]>(players.map((p) => [p.player.id, p.form])) : undefined;
  // Kad je drvo objavljeno, tabela boji igrače koji su u njemu (kostur je uzimao i 7., 8. i 9. mesto).
  const playoffIds = bracket ? playoffPlayers(bracket) : undefined;

  return (
    <>
      <Legend group={groups[0]} playoff={!!playoffIds} />
      <div className={`mt-8 grid gap-10 ${group ? "" : "xl:grid-cols-2 xl:gap-x-12"}`}>
        {tables.map(({ group: g, rows }) => (
          <StandingsTable key={g.id} group={g} rows={rows} forms={forms} playoffIds={playoffIds} />
        ))}
      </div>
    </>
  );
}

/** Igrači prvog kola drveta — oni su „u playoff-u". */
function playoffPlayers(bracket: PublicBracket): Set<number> {
  const ids = new Set<number>();
  for (const s of mainSeries(bracket.rounds[0])) {
    if (s.player1) ids.add(s.player1.id);
    if (s.player2) ids.add(s.player2.id);
  }
  return ids;
}

/* ---------------------------------- Statistika ---------------------------------- */

async function StatisticsTab({
  group,
  phase,
  sort,
  hrefFor,
}: {
  group?: PublicGroup;
  phase: StatsPhase;
  sort: StatSortKey;
  hrefFor: (phase: StatsPhase, sort: StatSortKey) => string;
}) {
  const rows = await getStats(phase);
  if (!rows) return <Empty>Statistika trenutno nije dostupna. Pokušaj ponovo za par minuta.</Empty>;
  return (
    <StatsTab
      rows={rows}
      group={group}
      phase={phase}
      sort={sort}
      phaseHref={(p) => hrefFor(p, sort)}
      sortHref={(so) => hrefFor(phase, so)}
    />
  );
}

/* ------------------------------ Rezultati / Raspored ------------------------------ */

async function MatchesTab({
  season,
  group,
  played,
  bracket,
}: {
  season: PublicSeason;
  group?: PublicGroup;
  played: boolean;
  bracket: PublicBracket | null;
}) {
  const matches = await getMatches(season.id, { groupId: group?.id, played });
  if (!matches) return <Empty>Rezultati trenutno nisu dostupni. Pokušaj ponovo za par minuta.</Empty>;

  // Raspored playoff-a su serije (mečevi serije nastaju tek kad se odigraju).
  const upcomingSeries = !played && bracket && !group ? upcoming(bracket) : [];

  if (matches.length === 0 && upcomingSeries.length === 0)
    return <Empty>{played ? "Još nema odigranih mečeva." : "Trenutno nema zakazanih mečeva."}</Empty>;

  // Rezultati: playoff (najnovije kolo prvo), pa grupna faza po kolima. Raspored: po danu i satu.
  // Za „Sve grupe" je grupna faza samo poslednja dva dana igranja — svih 900 mečeva je po grupama
  // (strana sa svim mečevima bi imala nekoliko MB, a čita se sa telefona).
  const latestGroupDays = played && !group ? lastPlayedDays(matches, 2) : null;
  const shown = latestGroupDays
    ? matches.filter((m) => m.stageType === "KNOCKOUT" || latestGroupDays.has((m.scheduledStart ?? "").slice(0, 10)))
    : matches;
  const sections = played
    ? latestGroupDays
      ? [...byRound(shown.filter((m) => m.stageType === "KNOCKOUT")), ...byPlayedDay(shown.filter((m) => m.stageType !== "KNOCKOUT"))]
      : byRound(shown)
    : byDay(shown);

  return (
    <div className="space-y-12">
      {upcomingSeries.length > 0 && (
        <section>
          <h2 className="flex items-center gap-3 border-b-2 border-playoff pb-2 text-2xl font-bold">
            Playoff — serije na redu
            <Link href="/playoff" className="text-sm font-bold text-playoff underline underline-offset-4">
              Drvo
            </Link>
          </h2>
          <ul className="mt-2 divide-y divide-black/10">
            {upcomingSeries.map(({ series, roundName }) => (
              <li key={series.id} className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 py-4 md:gap-6">
                <span className="truncate text-right text-base font-semibold md:text-lg">{series.player1?.nickname}</span>
                <span className="flex min-w-[120px] flex-col items-center">
                  <span className="rounded-full bg-playoff px-3 py-1 text-sm font-bold text-white">
                    {series.status === "IN_PROGRESS" ? `${series.wins1}:${series.wins2}` : "vs"}
                  </span>
                  <span className="mt-1 text-center text-xs text-black/50">
                    {[roundName, seriesCode(series), shortWhen(series.scheduledStart, series.courtName)].filter(Boolean).join(" · ")}
                  </span>
                </span>
                <span className="truncate text-left text-base font-semibold md:text-lg">{series.player2?.nickname}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
      {latestGroupDays && (
        <p className="rounded-2xl bg-black/[0.04] p-4 text-sm text-black/70">
          Grupna faza je završena — ispod su mečevi poslednjeg vikenda. Svih 90 mečeva svake grupe, po kolima, vidi se
          kad izabereš grupu gore.
        </p>
      )}
      {sections.map((s) => (
        <section key={s.title} className="[contain-intrinsic-size:auto_900px] [content-visibility:auto]">
          <h2 className={`border-b-2 pb-2 text-2xl font-bold ${s.playoff ? "border-playoff" : "border-black"}`}>{s.title}</h2>
          <ul className="mt-2 divide-y divide-black/10">
            {s.matches.map((m) => (
              <MatchRow key={m.id} match={m} showGroup={!group} played={played} />
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

/** Serije koje čekaju ili se igraju (oba igrača poznata), po kolu pa po terminu. */
function upcoming(bracket: PublicBracket) {
  return bracket.rounds
    .flatMap((round) =>
      round.series
        .filter((s) => s.player1 && s.player2 && (s.status === "READY" || s.status === "IN_PROGRESS"))
        .map((series) => ({ series, roundName: series.thirdPlace ? "Za 3. mesto" : round.name, roundNo: round.roundNo })),
    )
    .sort(
      (a, b) =>
        (a.series.scheduledStart ?? "~").localeCompare(b.series.scheduledStart ?? "~") ||
        a.roundNo - b.roundNo ||
        a.series.number - b.series.number,
    );
}

function byRound(matches: PublicMatch[]) {
  const playoff = new Map<number, PublicMatch[]>();
  const rounds = new Map<number, PublicMatch[]>();
  for (const m of matches) {
    const target = m.stageType === "KNOCKOUT" ? playoff : rounds;
    target.set(m.roundNumber, [...(target.get(m.roundNumber) ?? []), m]);
  }
  return [
    ...[...playoff.entries()]
      .sort(([a], [b]) => b - a)
      .map(([, list]) => ({
        title: `Playoff · ${list.find((m) => m.roundName && m.roundName !== "Za 3. mesto")?.roundName ?? list[0].roundName}`,
        matches: list,
        playoff: true,
      })),
    ...[...rounds.entries()]
      .sort(([a], [b]) => b - a)
      .map(([round, list]) => ({ title: `${round}. kolo`, matches: list, playoff: false })),
  ];
}

/** Poslednjih {@code count} dana na kojima je igran grupni meč (datum sa turniriukraju.com). */
function lastPlayedDays(matches: PublicMatch[], count: number): Set<string> {
  const days = [...new Set(matches.filter((m) => m.stageType !== "KNOCKOUT" && m.scheduledStart).map((m) => m.scheduledStart!.slice(0, 10)))];
  return new Set(days.sort().reverse().slice(0, count));
}

/** Grupna faza po danu, najnoviji dan prvi. */
function byPlayedDay(matches: PublicMatch[]) {
  const days = new Map<string, PublicMatch[]>();
  for (const m of [...matches].sort((a, b) => (b.scheduledStart ?? "").localeCompare(a.scheduledStart ?? ""))) {
    const key = formatDay(m.scheduledStart);
    days.set(key, [...(days.get(key) ?? []), m]);
  }
  return [...days.entries()].map(([title, list]) => ({ title: `Grupna faza · ${title}`, matches: list, playoff: false }));
}

function byDay(matches: PublicMatch[]) {
  const sorted = [...matches].sort((a, b) => (a.scheduledStart ?? "~").localeCompare(b.scheduledStart ?? "~"));
  const days = new Map<string, PublicMatch[]>();
  for (const m of sorted) {
    const key = formatDay(m.scheduledStart);
    days.set(key, [...(days.get(key) ?? []), m]);
  }
  return [...days.entries()].map(([title, list]) => ({ title, matches: list, playoff: false }));
}
