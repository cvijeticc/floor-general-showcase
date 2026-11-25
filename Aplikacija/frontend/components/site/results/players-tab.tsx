import Link from "next/link";
import { playerThumb } from "@/lib/player-photos";
import { searchPlayers } from "@/lib/player-search";
import type { PublicGroup, PublicPlayerSummary } from "@/lib/site-api";
import { roundLabel } from "@/lib/site-api";
import { decimal, differenceColor, percent, plural, signed } from "@/lib/stat-format";
import { FormChips } from "./form-chips";
import { PlayerAvatar } from "./player-avatar";
import { PlayerLink } from "./player-link";

export const PLAYER_SORTS = [
  { key: "grupa", label: "Po grupama" },
  { key: "kr", label: "+/−" },
  { key: "pobede", label: "Pobede" },
  { key: "uspesnost", label: "% pobeda" },
  { key: "ime", label: "A–Ž" },
] as const;

export type PlayerSortKey = (typeof PLAYER_SORTS)[number]["key"];

/**
 * Tab „Igrači" (UC-PUB-10, lideri iz UC-PUB-08). Ceo tab se renderuje na serveru — radi bez JS-a i
 * Google vidi svaki profil; pretraga (?q=) i redosled (?sort=) su u URL-u, pa se link može poslati.
 */
export function PlayersTab({
  players,
  groups,
  group,
  query,
  sort,
  sortHref,
  clearHref,
}: {
  players: PublicPlayerSummary[];
  /** Grupe sezone, redom — za ime grupe i redosled „Po grupama". */
  groups: PublicGroup[];
  group?: PublicGroup;
  query: string;
  sort: PlayerSortKey;
  sortHref: (sort: PlayerSortKey) => string;
  clearHref: string;
}) {
  if (query) {
    // Pretraga ide kroz sve grupe — ko traži sebe, ne zna uvek ni u kojoj je grupi.
    const found = searchPlayers(query, players, (p) => p.player);
    return (
      <section>
        <div className="flex flex-wrap items-baseline justify-between gap-3">
          <h2 className="text-2xl font-bold">Pretraga: „{query}“</h2>
          <Link href={clearHref} className="font-semibold underline underline-offset-4">
            Svi igrači
          </Link>
        </div>
        {found.length === 0 ? (
          <p className="mt-6 text-lg text-black/60">
            Nema igrača za „{query}“. Probaj nadimak ili prezime — č, ć, š, ž i đ nisu obavezni.
          </p>
        ) : (
          <PlayerList players={found} />
        )}
      </section>
    );
  }

  const visible = group ? players.filter((p) => p.groupId === group.id) : players;
  if (visible.length === 0) {
    return <p className="py-16 text-center text-lg text-black/60">Igrači još nisu raspoređeni u grupe.</p>;
  }

  return (
    <>
      <Leaders players={visible} title={group ? `Lideri — ${group.name}` : "Lideri lige"} />

      <section className="mt-12">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <h2 className="text-2xl font-bold">
            {group ? group.name : "Svi igrači"} <span className="text-black/35">{visible.length}</span>
          </h2>
          <nav aria-label="Redosled" className="flex flex-wrap gap-2">
            {PLAYER_SORTS.map((s) => (
              <Link
                key={s.key}
                href={sortHref(s.key)}
                scroll={false}
                aria-current={s.key === sort ? "true" : undefined}
                className={`rounded-full px-4 py-1.5 text-sm transition-colors duration-300 ${
                  s.key === sort ? "bg-black font-bold text-white" : "bg-black/[0.06] text-black hover:bg-black/10"
                }`}
              >
                {s.label}
              </Link>
            ))}
          </nav>
        </div>
        <PlayerList players={sortPlayers(visible, sort, groups)} />
      </section>
    </>
  );
}

function sortPlayers(players: PublicPlayerSummary[], sort: PlayerSortKey, groups: PublicGroup[]) {
  const groupOrder = new Map(groups.map((g, i) => [g.id, i]));
  const byNickname = (a: PublicPlayerSummary, b: PublicPlayerSummary) =>
    a.player.nickname.localeCompare(b.player.nickname, "sr");
  const byForm = (a: PublicPlayerSummary, b: PublicPlayerSummary) =>
    b.pointDifference - a.pointDifference || b.wins - a.wins || byNickname(a, b);

  const sorted = [...players];
  switch (sort) {
    case "grupa":
      return sorted.sort(
        (a, b) =>
          (groupOrder.get(a.groupId) ?? 0) - (groupOrder.get(b.groupId) ?? 0) ||
          (a.position ?? Number.MAX_SAFE_INTEGER) - (b.position ?? Number.MAX_SAFE_INTEGER) ||
          byNickname(a, b),
      );
    case "kr":
      return sorted.sort(byForm);
    case "pobede":
      return sorted.sort((a, b) => b.wins - a.wins || byForm(a, b));
    case "uspesnost":
      return sorted.sort((a, b) => winRate(b) - winRate(a) || byForm(a, b));
    case "ime":
      return sorted.sort(byNickname);
  }
}

/** Igrač bez odigranog meča ide na dno, ne na 0 %. */
function winRate(p: PublicPlayerSummary) {
  return p.played > 0 ? p.wins / p.played : -1;
}

const COLUMNS = "md:grid-cols-[minmax(0,1fr)_8rem_3rem_4.5rem_4rem_8.5rem]";

function PlayerList({ players }: { players: PublicPlayerSummary[] }) {
  return (
    <div className="mt-5 overflow-hidden rounded-2xl border border-black/10">
      <div
        aria-hidden
        className={`hidden gap-4 bg-black px-4 py-3 text-xs font-semibold uppercase tracking-wide text-white md:grid ${COLUMNS}`}
      >
        <span>Igrač</span>
        <span>Grupa</span>
        <span className="text-center">O</span>
        <span className="text-center">P–I</span>
        <span className="text-center">+/−</span>
        <span>Forma</span>
      </div>
      <ul className="divide-y divide-black/5">
        {players.map((p) => (
          <li key={p.player.id}>
            <PlayerLink
              player={p.player}
              className={`group grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4 px-4 py-3 transition-colors hover:bg-black/[0.03] ${COLUMNS}`}
            >
              <div className="flex min-w-0 items-center gap-3">
                <PlayerAvatar name={p.player.name} src={playerThumb(p.player)} size={44} />
                <div className="min-w-0">
                  <div className="truncate text-lg font-bold underline-offset-4 group-hover:underline">
                    {p.player.nickname}
                  </div>
                  <div className="truncate text-sm text-black/55">
                    {p.player.name}
                    <span className="md:hidden"> · {placeLabel(p)}</span>
                  </div>
                  <FormChips items={p.form} className="mt-1.5 md:hidden" />
                </div>
              </div>
              <div className="hidden text-sm md:block">{placeLabel(p)}</div>
              <div className="hidden text-center tabular-nums md:block">{p.played}</div>
              <div className="text-right tabular-nums md:text-center">
                <div className="font-bold">
                  {p.wins}–{p.losses}
                </div>
                <div className={`text-sm md:hidden ${differenceColor(p.pointDifference)}`}>
                  {signed(p.pointDifference)}
                </div>
              </div>
              <div className={`hidden text-center font-bold tabular-nums md:block ${differenceColor(p.pointDifference)}`}>
                {signed(p.pointDifference)}
              </div>
              <div className="hidden md:block">
                <FormChips items={p.form} />
              </div>
            </PlayerLink>
          </li>
        ))}
      </ul>
    </div>
  );
}

function placeLabel(p: PublicPlayerSummary) {
  return p.position ? `${p.groupName} · ${p.position}.` : p.groupName;
}

/* ----------------------------------- Lideri ----------------------------------- */

type Leader = { player: PublicPlayerSummary; value: string; detail: string };

/**
 * „Lideri" (UC-PUB-08) — kao „Top players" na SofaScore-u. Proseci i % računaju se samo za igrače
 * sa bar pola mečeva od onog ko ih je najviše odigrao: prag dolazi iz podataka, ne iz koda.
 */
function Leaders({ players, title }: { players: PublicPlayerSummary[]; title: string }) {
  const mostPlayed = Math.max(0, ...players.map((p) => p.played));
  if (mostPlayed === 0) return null;
  const minPlayed = Math.max(1, Math.ceil(mostPlayed / 2));
  const regular = players.filter((p) => p.played >= minPlayed);
  const threshold = `bar ${minPlayed} ${plural(minPlayed, "meč", "meča", "mečeva")}`;

  const boards: { title: string; note?: string; leaders: Leader[] }[] = [
    {
      title: "Koš-razlika",
      leaders: top(
        players.filter((p) => p.played > 0),
        (p) => p.pointDifference,
        (p) => signed(p.pointDifference),
      ),
    },
    {
      title: "Pobede",
      leaders: top(players.filter((p) => p.wins > 0), (p) => p.wins, (p) => `${p.wins}–${p.losses}`),
    },
    {
      title: "% pobeda",
      note: threshold,
      leaders: top(regular, (p) => p.wins / p.played, (p) => percent(p.wins, p.played)),
    },
    {
      title: "Poena po meču",
      note: threshold,
      leaders: top(regular, (p) => p.pointsFor / p.played, (p) => decimal(p.pointsFor / p.played)),
    },
    {
      title: "Najduži niz pobeda",
      leaders: top(
        players.filter((p) => p.longestWinStreak > 0),
        (p) => p.longestWinStreak,
        (p) => String(p.longestWinStreak),
      ),
    },
    {
      title: "Najubedljivija pobeda",
      leaders: top(
        players.filter((p) => p.biggestWin),
        (p) => (p.biggestWin ? p.biggestWin.scored - p.biggestWin.conceded : 0),
        (p) => (p.biggestWin ? `${p.biggestWin.scored}:${p.biggestWin.conceded}` : ""),
        (p) => (p.biggestWin ? `protiv ${p.biggestWin.opponent.nickname} · ${roundLabel(p.biggestWin)}` : ""),
      ),
    },
  ];

  return (
    <section aria-labelledby="lideri">
      <h2 id="lideri" className="text-2xl font-bold">
        {title}
      </h2>
      <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {boards.map((board) => (
          <div key={board.title} className="rounded-2xl border border-black/10 p-4">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-black/55">
              {board.title}
              {board.note && <span className="font-normal normal-case tracking-normal text-black/40"> · {board.note}</span>}
            </h3>
            {board.leaders.length === 0 ? (
              <p className="mt-3 text-sm text-black/45">Još nema podataka.</p>
            ) : (
              <ol className="mt-3 space-y-2.5">
                {board.leaders.map((leader, i) => (
                  <li key={leader.player.player.id} className="flex items-center gap-3">
                    <span className="w-3 text-sm font-bold tabular-nums text-black/35">{i + 1}</span>
                    <PlayerLink player={leader.player.player} className="group flex min-w-0 flex-1 items-center gap-2.5">
                      <PlayerAvatar name={leader.player.player.name} src={playerThumb(leader.player.player)} size={32} />
                      <span className="min-w-0">
                        <span className="block truncate font-semibold underline-offset-2 group-hover:underline">
                          {leader.player.player.nickname}
                        </span>
                        <span className="block truncate text-xs text-black/50">{leader.detail}</span>
                      </span>
                    </PlayerLink>
                    <span className="shrink-0 text-lg font-extrabold tabular-nums">{leader.value}</span>
                  </li>
                ))}
              </ol>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}

const LEADERS_PER_BOARD = 3;

/** Prva tri; kod iste vrednosti bolja koš-razlika, pa nadimak — redosled ne sme da skače. */
function top(
  players: PublicPlayerSummary[],
  score: (p: PublicPlayerSummary) => number,
  value: (p: PublicPlayerSummary) => string,
  detail: (p: PublicPlayerSummary) => string = (p) => p.groupName,
): Leader[] {
  return [...players]
    .sort(
      (a, b) =>
        score(b) - score(a) ||
        b.pointDifference - a.pointDifference ||
        a.player.nickname.localeCompare(b.player.nickname, "sr"),
    )
    .slice(0, LEADERS_PER_BOARD)
    .map((p) => ({ player: p, value: value(p), detail: detail(p) }));
}
