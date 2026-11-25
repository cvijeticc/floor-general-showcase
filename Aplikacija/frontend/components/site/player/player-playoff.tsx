import Link from "next/link";
import { PlayerAvatar } from "@/components/site/results/player-avatar";
import { PlayerLink } from "@/components/site/results/player-link";
import { seriesCode, shortWhen } from "@/lib/bracket";
import { playerThumb } from "@/lib/player-photos";
import type { PublicBracket, PublicPlayer, PublicSeries } from "@/lib/site-api";

export type PlayoffEntry = { series: PublicSeries; roundName: string; roundNo: number; winsRequired: number };

/** Serije igrača u drvetu, od prvog kola ka finalu. */
export function playoffEntries(bracket: PublicBracket | null, playerId: number): PlayoffEntry[] {
  if (!bracket) return [];
  return bracket.rounds.flatMap((round) =>
    round.series
      .filter((s) => s.player1?.id === playerId || s.player2?.id === playerId)
      .map((series) => ({
        series,
        roundName: series.thirdPlace ? "Za 3. mesto" : round.name,
        roundNo: round.roundNo,
        winsRequired: round.winsRequired,
      })),
  );
}

/** Oznaka u vrhu profila: „Playoff · Top 32", „Ispao · Top 16", „Šampion". */
export function playoffBadge(entries: PlayoffEntry[], playerId: number, lastRoundNo: number) {
  const last = entries.filter((e) => !e.series.thirdPlace).at(-1);
  if (!last) return null;
  const { series } = last;
  if (series.winnerId === playerId && last.roundNo === lastRoundNo) return { label: "Šampion", tone: "gold" as const };
  if (series.winnerId != null && series.winnerId !== playerId) {
    return { label: `Ispao · ${last.roundName}`, tone: "muted" as const };
  }
  return { label: `Playoff · ${last.roundName}`, tone: "playoff" as const };
}

/** Put igrača kroz playoff na profilu: kolo, protivnik, rezultat serije i mečevi. */
export function PlayerPlayoff({ player, entries }: { player: PublicPlayer; entries: PlayoffEntry[] }) {
  if (entries.length === 0) return null;
  return (
    <section aria-labelledby="playoff-igraca">
      <div className="flex items-baseline justify-between gap-3">
        <h2 id="playoff-igraca" className="text-2xl font-bold">
          Playoff
        </h2>
        <Link href="/playoff" className="text-sm font-bold text-playoff underline underline-offset-4">
          Celo drvo
        </Link>
      </div>
      <ol className="mt-4 space-y-3">
        {entries.map(({ series, roundName, winsRequired }) => {
          const first = series.player1?.id === player.id;
          const opponent = first ? series.player2 : series.player1;
          const mine = first ? series.wins1 : series.wins2;
          const theirs = first ? series.wins2 : series.wins1;
          const won = series.winnerId === player.id;
          const lost = series.winnerId != null && !won;
          const status = won
            ? series.walkover
              ? "Prošao službeno"
              : "Prošao dalje"
            : lost
              ? series.walkover
                ? "Ispao službeno"
                : "Ispao"
              : series.status === "IN_PROGRESS"
                ? "U toku"
                : opponent
                  ? shortWhen(series.scheduledStart, series.courtName) || "Na redu"
                  : "Čeka protivnika";
          return (
            <li
              key={series.id}
              className={`rounded-2xl border p-4 ${won ? "border-emerald-500/40 bg-emerald-50" : lost ? "border-black/10 bg-black/[0.02]" : "border-playoff/50 bg-orange-50/60"}`}
            >
              <div className="flex items-center justify-between gap-3 text-xs font-bold uppercase tracking-wide text-black/50">
                <span>
                  {roundName} · {seriesCode(series)}
                </span>
                <span className={won ? "text-emerald-700" : lost ? "" : "text-playoff"}>{status}</span>
              </div>
              <div className="mt-2 flex items-center gap-3">
                {opponent ? (
                  <PlayerLink player={opponent} className="group flex min-w-0 flex-1 items-center gap-3">
                    <PlayerAvatar name={opponent.name} src={playerThumb(opponent)} size={36} />
                    <span className="min-w-0">
                      <span className="block truncate font-semibold underline-offset-4 group-hover:underline">
                        <span className="text-black/45">protiv </span>
                        {opponent.nickname}
                      </span>
                      <span className="block truncate text-sm text-black/50">{opponent.name}</span>
                    </span>
                  </PlayerLink>
                ) : (
                  <span className="flex-1 text-sm text-black/50">Protivnik se čeka iz prethodnog kola</span>
                )}
                {series.games.length > 0 && winsRequired > 1 && (
                  <span className="text-2xl font-extrabold tabular-nums">
                    {mine}:{theirs}
                  </span>
                )}
              </div>
              {series.games.length > 0 && (
                <p className="mt-2 text-sm tabular-nums text-black/60">
                  {series.games
                    .map((g) => {
                      const a = first ? g.score1 : g.score2;
                      const b = first ? g.score2 : g.score1;
                      return `${a}:${b}`;
                    })
                    .join(" · ")}
                </p>
              )}
            </li>
          );
        })}
      </ol>
    </section>
  );
}
