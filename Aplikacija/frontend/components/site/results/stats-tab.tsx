import Link from "next/link";
import { playerThumb } from "@/lib/player-photos";
import type { PublicGroup, PublicStatRow, StatsPhase } from "@/lib/site-api";
import { decimal, differenceColor, signed } from "@/lib/stat-format";
import { PlayerAvatar } from "./player-avatar";
import { PlayerLink } from "./player-link";

export const STATS_PHASES: { key: StatsPhase; param: string; label: string }[] = [
  { key: "ALL", param: "sve", label: "Sve faze" },
  { key: "GROUP", param: "grupe", label: "Grupna faza" },
  { key: "KNOCKOUT", param: "playoff", label: "Playoff" },
];

export const STAT_SORTS = [
  { key: "dati", label: "Pros. dati" },
  { key: "primljeni", label: "Pros. primljeni" },
  { key: "kr", label: "+/−" },
  { key: "uspesnost", label: "% pobeda" },
  { key: "tesne", label: "Tesne pobede" },
  { key: "odigrano", label: "Odigrano" },
] as const;

export type StatSortKey = (typeof STAT_SORTS)[number]["key"];

const COMPARE: Record<StatSortKey, (a: PublicStatRow, b: PublicStatRow) => number> = {
  dati: (a, b) => b.averageFor - a.averageFor || b.pointDifference - a.pointDifference,
  // Najmanje primljenih je najbolja odbrana; igrači bez meča idu na kraj.
  primljeni: (a, b) => (a.played === 0 ? 1 : 0) - (b.played === 0 ? 1 : 0) || a.averageAgainst - b.averageAgainst,
  kr: (a, b) => b.pointDifference - a.pointDifference,
  uspesnost: (a, b) => b.winPercentage - a.winPercentage || b.wins - a.wins,
  tesne: (a, b) => b.closeWins - a.closeWins || a.closeLosses - b.closeLosses,
  odigrano: (a, b) => b.played - a.played || b.wins - a.wins,
};

/**
 * Tab „Statistika" — „Poeni po igračima" sa turniriukraju.com, uz proseke, procenat pobeda,
 * tesne pobede (razlika do 2 poena, jedan šut spolja) i pobede bez primljenog poena.
 * Faza (sve / grupna / playoff) i redosled su u URL-u, pa se tabela može podeliti linkom.
 * Službeni rezultati (11:0 bez igre) nisu u odigranim mečevima ni u poenima — broje se posebno.
 */
export function StatsTab({
  rows,
  group,
  phase,
  sort,
  phaseHref,
  sortHref,
}: {
  rows: PublicStatRow[];
  group?: PublicGroup;
  phase: StatsPhase;
  sort: StatSortKey;
  phaseHref: (phase: StatsPhase) => string;
  sortHref: (sort: StatSortKey) => string;
}) {
  const visible = (group ? rows.filter((r) => r.groupId === group.id) : rows)
    .filter((r) => r.played > 0 || phase !== "KNOCKOUT")
    .sort(COMPARE[sort]);

  return (
    <section>
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 className="text-2xl font-bold">Poeni po igračima</h2>
          <p className="mt-1 text-sm text-black/55">
            {group ? group.name : "Svi igrači"} · službeni rezultati se ne računaju u poene i proseke
          </p>
        </div>
        <nav aria-label="Faza" className="flex gap-2">
          {STATS_PHASES.map((p) => (
            <Link
              key={p.key}
              href={phaseHref(p.key)}
              aria-current={p.key === phase ? "page" : undefined}
              className={`rounded-full px-4 py-2 text-sm font-bold transition-colors ${
                p.key === phase
                  ? p.key === "KNOCKOUT"
                    ? "bg-playoff text-white"
                    : "bg-black text-white"
                  : "border border-black/15 hover:bg-black/5"
              }`}
            >
              {p.label}
            </Link>
          ))}
        </nav>
      </div>

      <nav aria-label="Redosled" className="-mx-5 mt-5 flex gap-2 overflow-x-auto px-5 pb-1 md:mx-0 md:flex-wrap md:px-0">
        {STAT_SORTS.map((s) => (
          <Link
            key={s.key}
            href={sortHref(s.key)}
            aria-current={s.key === sort ? "page" : undefined}
            className={`shrink-0 whitespace-nowrap rounded-full px-3 py-1.5 text-sm ${
              s.key === sort ? "bg-black font-bold text-white" : "bg-black/[0.05] hover:bg-black/10"
            }`}
          >
            {s.label}
          </Link>
        ))}
      </nav>

      {visible.length === 0 ? (
        <p className="py-16 text-center text-lg text-black/60">
          {phase === "KNOCKOUT" ? "Još nema odigranih mečeva playoff-a." : "Još nema odigranih mečeva."}
        </p>
      ) : (
        <div className="@container mt-6 overflow-x-auto rounded-2xl border border-black/10">
          <table className="w-full text-left text-[15px] tabular-nums">
            <thead className="bg-black text-xs uppercase tracking-wide text-white">
              <tr>
                <th className="py-3 pl-4 pr-2 font-semibold">#</th>
                <th className="px-2 py-3 font-semibold">Igrač</th>
                <Th title="Odigrano (bez službenih rezultata)">O</Th>
                <Th title="Pobede – porazi" className="hidden @md:table-cell">
                  P–I
                </Th>
                <Th title="Procenat pobeda" className="hidden @lg:table-cell">
                  %
                </Th>
                <Th title="Dati poeni" className="hidden @xl:table-cell">
                  Dati
                </Th>
                <Th title="Primljeni poeni" className="hidden @xl:table-cell">
                  Prim.
                </Th>
                <Th title="Koš-razlika">+/−</Th>
                <Th title="Prosek datih poena po meču">Pros. dati</Th>
                <Th title="Prosek primljenih poena po meču" className="hidden @md:table-cell">
                  Pros. prim.
                </Th>
                <Th title="Tesne pobede – tesni porazi (razlika do 2 poena)" className="hidden @3xl:table-cell">
                  Tesno
                </Th>
                <Th title="Pobede bez primljenog poena" className="hidden @3xl:table-cell">
                  Bez koša
                </Th>
                <Th title="Službeni rezultati: pobede – porazi (nedolazak ili predaja)" className="hidden pr-4 @4xl:table-cell">
                  Služb.
                </Th>
              </tr>
            </thead>
            <tbody>
              {visible.map((r, index) => (
                <tr key={r.player.id} className="border-t border-black/5 even:bg-black/[0.02]">
                  <td className="py-2.5 pl-4 pr-2 font-bold text-black/60">{index + 1}</td>
                  <td className="px-2 py-2">
                    <PlayerLink player={r.player} className="group flex items-center gap-2.5">
                      <PlayerAvatar name={r.player.name} src={playerThumb(r.player)} size={28} className="hidden @sm:inline-flex" />
                      <span className="min-w-0">
                        <span className="font-semibold underline-offset-2 group-hover:underline">{r.player.nickname}</span>
                        <span className="block text-[12px] text-black/45">{r.groupName}</span>
                      </span>
                    </PlayerLink>
                  </td>
                  <Td>{r.played}</Td>
                  <Td className="hidden @md:table-cell">
                    {r.wins}–{r.losses}
                  </Td>
                  <Td className="hidden @lg:table-cell">{r.played ? `${r.winPercentage}%` : "—"}</Td>
                  <Td className="hidden @xl:table-cell">{r.pointsFor}</Td>
                  <Td className="hidden @xl:table-cell">{r.pointsAgainst}</Td>
                  <Td className={`font-semibold ${differenceColor(r.pointDifference)}`}>{signed(r.pointDifference)}</Td>
                  <Td className={sort === "dati" ? "font-extrabold" : "font-semibold"}>{r.played ? decimal(r.averageFor) : "—"}</Td>
                  <Td className={`hidden @md:table-cell ${sort === "primljeni" ? "font-extrabold" : ""}`}>
                    {r.played ? decimal(r.averageAgainst) : "—"}
                  </Td>
                  <Td className="hidden @3xl:table-cell">
                    {r.closeWins}–{r.closeLosses}
                  </Td>
                  <Td className="hidden @3xl:table-cell">{r.shutouts}</Td>
                  <Td className="hidden pr-4 @4xl:table-cell">
                    {r.officialWins}–{r.officialLosses}
                  </Td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function Th({ children, title, className = "" }: { children: React.ReactNode; title: string; className?: string }) {
  return (
    <th className={`whitespace-nowrap px-2 py-3 text-center font-semibold ${className}`}>
      <abbr title={title} className="no-underline">
        {children}
      </abbr>
    </th>
  );
}

function Td({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return <td className={`px-2 py-2.5 text-center ${className}`}>{children}</td>;
}
