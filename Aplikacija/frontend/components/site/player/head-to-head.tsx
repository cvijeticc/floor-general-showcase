import { PlayerAvatar } from "@/components/site/results/player-avatar";
import { PlayerLink } from "@/components/site/results/player-link";
import { playerThumb } from "@/lib/player-photos";
import type { PublicHeadToHead } from "@/lib/site-api";
import { differenceColor, signed } from "@/lib/stat-format";

/** Međusobni dueli (FR-6.5.2): bilans protiv svakog protivnika u sezoni, redom prvog susreta. */
export function HeadToHead({ rows }: { rows: PublicHeadToHead[] }) {
  if (rows.length === 0) return null;

  return (
    <section aria-labelledby="medjusobni">
      <h2 id="medjusobni" className="text-2xl font-bold">
        Međusobni dueli
      </h2>
      <p className="mt-1 text-sm text-black/50">Bilans protiv svakog protivnika u ovoj sezoni.</p>
      <div className="mt-4 overflow-x-auto rounded-2xl border border-black/10">
        <table className="w-full text-left text-[15px] tabular-nums">
          <thead className="bg-black text-xs uppercase tracking-wide text-white">
            <tr>
              <th className="py-3 pl-4 pr-2 font-semibold">Protivnik</th>
              <th className="px-2 py-3 text-center font-semibold">
                <abbr title="Pobede–porazi" className="no-underline">
                  P–I
                </abbr>
              </th>
              <th className="hidden px-2 py-3 text-center font-semibold sm:table-cell">
                <abbr title="Dati i primljeni poeni" className="no-underline">
                  Poeni
                </abbr>
              </th>
              <th className="py-3 pl-2 pr-4 text-center font-semibold">+/−</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((h) => {
              const difference = h.pointsFor - h.pointsAgainst;
              return (
                <tr key={h.opponent.id} className="border-t border-black/5 even:bg-black/[0.02]">
                  <td className="py-2 pl-4 pr-2">
                    <PlayerLink player={h.opponent} className="group flex min-w-0 items-center gap-2.5">
                      <PlayerAvatar name={h.opponent.name} src={playerThumb(h.opponent)} size={28} />
                      <span className="min-w-0">
                        <span className="font-semibold underline-offset-2 group-hover:underline">{h.opponent.nickname}</span>{" "}
                        <span className="text-black/50">{h.opponent.name}</span>
                      </span>
                    </PlayerLink>
                  </td>
                  <td className="px-2 py-2 text-center font-bold">
                    {h.wins}–{h.losses}
                  </td>
                  <td className="hidden px-2 py-2 text-center sm:table-cell">
                    {h.pointsFor}:{h.pointsAgainst}
                  </td>
                  <td className={`py-2 pl-2 pr-4 text-center font-bold ${differenceColor(difference)}`}>{signed(difference)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
