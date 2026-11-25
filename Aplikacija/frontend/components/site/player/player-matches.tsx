import { STATUS_NOTE } from "@/components/site/results/match-row";
import { PlayerAvatar } from "@/components/site/results/player-avatar";
import { PlayerLink } from "@/components/site/results/player-link";
import { playerThumb } from "@/lib/player-photos";
import { formatDate, formatDay, formatTime, type PublicMatch, type PublicPlayer, roundLabel } from "@/lib/site-api";
import { signed } from "@/lib/stat-format";
import { isPlayed, isUpcoming, viewFor, type MatchView } from "./match-view";

/** „Sledeći meč" — kao na SofaScore-u, odmah ispod statistike: ko, koje kolo, kad i gde. */
export function NextMatch({ player, match }: { player: PublicPlayer; match: PublicMatch }) {
  const { opponent } = viewFor(match, player.id);
  const when = match.scheduledStart
    ? `${formatDay(match.scheduledStart)} · ${formatTime(match.scheduledStart)}`
    : "termin još nije određen";
  const details = [roundLabel(match), when, match.courtName, STATUS_NOTE[match.status]].filter(Boolean);

  return (
    <section aria-labelledby="sledeci-mec" className="mt-8 rounded-2xl bg-black px-5 py-5 text-white sm:px-8">
      <h2 id="sledeci-mec" className="text-sm font-semibold uppercase tracking-[0.18em] text-white/55">
        Sledeći meč
      </h2>
      <div className="mt-4 grid grid-cols-[1fr_auto_1fr] items-center gap-3 sm:gap-6">
        <div className="flex min-w-0 items-center justify-end gap-3 text-right">
          <span className="min-w-0 truncate text-lg font-bold sm:text-xl">{player.nickname}</span>
          <PlayerAvatar name={player.name} src={playerThumb(player)} size={44} tone="dark" className="hidden sm:inline-flex" />
        </div>
        <span className="rounded-full bg-white px-3 py-1 text-sm font-extrabold text-black">VS</span>
        <PlayerLink player={opponent} className="group flex min-w-0 items-center gap-3">
          <PlayerAvatar name={opponent.name} src={playerThumb(opponent)} size={44} tone="dark" className="hidden sm:inline-flex" />
          <span className="min-w-0 truncate text-lg font-bold underline-offset-4 group-hover:underline sm:text-xl">
            {opponent.nickname}
          </span>
        </PlayerLink>
      </div>
      <p className="mt-3 text-center text-sm text-white/60">{details.join(" · ")}</p>
    </section>
  );
}

/**
 * Svi mečevi igrača iz njegovog ugla (UC-PUB-06): predstojeći, pa odigrani od najnovijeg.
 * Ovo je i „tabela" za grafikon forme — svaka vrednost iz oblačića je i ovde.
 */
export function PlayerMatches({ player, matches }: { player: PublicPlayer; matches: PublicMatch[] }) {
  const upcoming = matches.filter(isUpcoming).map((m) => viewFor(m, player.id));
  const played = matches
    .filter(isPlayed)
    .map((m) => viewFor(m, player.id))
    .reverse();

  return (
    <section aria-labelledby="mecevi">
      <h2 id="mecevi" className="text-2xl font-bold">
        Mečevi
      </h2>

      {upcoming.length > 0 && (
        <>
          <h3 className="mt-5 text-sm font-semibold uppercase tracking-wide text-black/50">Predstojeći</h3>
          <ul className="mt-2 divide-y divide-black/5 rounded-2xl border border-black/10">
            {upcoming.map((view) => (
              <MatchLine key={view.match.id} view={view} />
            ))}
          </ul>
        </>
      )}

      <h3 className="mt-5 text-sm font-semibold uppercase tracking-wide text-black/50">Odigrani</h3>
      {played.length === 0 ? (
        <p className="mt-2 text-black/60">Još nema odigranih mečeva.</p>
      ) : (
        <ul className="mt-2 divide-y divide-black/5 rounded-2xl border border-black/10">
          {played.map((view) => (
            <MatchLine key={view.match.id} view={view} />
          ))}
        </ul>
      )}
    </section>
  );
}

function MatchLine({ view }: { view: MatchView }) {
  const { match, opponent, scored, conceded, won } = view;
  const played = scored !== null && conceded !== null && isPlayed(match);
  const meta = [match.scheduledStart ? formatDate(match.scheduledStart) : null, match.courtName, STATUS_NOTE[match.status]]
    .filter(Boolean)
    .join(" · ");

  return (
    <li className="grid grid-cols-[3.5rem_minmax(0,1fr)_auto] items-center gap-3 px-4 py-3 sm:grid-cols-[4.5rem_minmax(0,1fr)_auto_auto] sm:gap-4">
      <span className="text-sm tabular-nums text-black/50">
        {match.stageType === "KNOCKOUT" ? (
          <span className="font-semibold text-playoff">{match.roundName}</span>
        ) : (
          `${match.roundNumber}. kolo`
        )}
      </span>
      <PlayerLink player={opponent} className="group flex min-w-0 items-center gap-3">
        <PlayerAvatar name={opponent.name} src={playerThumb(opponent)} size={36} />
        <span className="min-w-0">
          <span className="block truncate font-semibold underline-offset-4 group-hover:underline">
            <span className="sr-only">protiv </span>
            {opponent.nickname}
          </span>
          <span className="block truncate text-sm text-black/50">
            {opponent.name}
            {meta && ` · ${meta}`}
          </span>
        </span>
      </PlayerLink>
      {played ? (
        <>
          <span className="hidden text-xl font-extrabold tabular-nums sm:block">
            {scored}:{conceded}
          </span>
          <span
            className={`inline-flex min-w-[4.5rem] items-center justify-center gap-1.5 rounded-lg px-2 py-1 text-sm font-bold text-white ${
              won ? "bg-emerald-700" : "bg-red-700"
            }`}
          >
            <span aria-hidden>{won ? "P" : "I"}</span>
            <span className="sr-only">{won ? "pobeda" : "poraz"}</span>
            <span className="sm:hidden">
              {scored}:{conceded}
            </span>
            <span className="hidden font-semibold opacity-90 sm:inline">{signed(scored - conceded)}</span>
          </span>
        </>
      ) : (
        <span className="col-start-3 justify-self-end rounded-full bg-black px-3 py-1 text-sm font-bold text-white sm:col-start-4">
          {formatTime(match.scheduledStart) || "vs"}
        </span>
      )}
    </li>
  );
}
