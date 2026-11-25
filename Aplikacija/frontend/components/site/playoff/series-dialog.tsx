"use client";

import { XIcon } from "lucide-react";
import { Dialog } from "radix-ui";
import { PlayerAvatar } from "@/components/site/results/player-avatar";
import { PlayerLink } from "@/components/site/results/player-link";
import { feederLabel, roundFormat, seriesCode, shortWhen, winnerSide } from "@/lib/bracket";
import { playerThumb } from "@/lib/player-photos";
import { formatDay, formatTime, type PublicPlayer, type PublicRound, type PublicSeries } from "@/lib/site-api";

/**
 * Detalji serije (dodir na karticu u drvetu): oba igrača sa linkom na profil, rezultat serije,
 * svaki meč sa terminom i terenom, i format kola. Na telefonu klizi odozdo, na računaru je u sredini.
 */
export function SeriesDialog({
  open,
  rounds,
  onClose,
}: {
  open: { series: PublicSeries; round: PublicRound; roundIndex: number } | null;
  rounds: PublicRound[];
  onClose: () => void;
}) {
  return (
    <Dialog.Root open={open != null} onOpenChange={(next) => !next && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-[60] bg-black/50 backdrop-blur-[2px] data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=closed]:animate-out data-[state=closed]:fade-out-0" />
        <Dialog.Content className="fixed inset-x-0 bottom-0 z-[61] max-h-[90dvh] overflow-y-auto rounded-t-3xl bg-white p-5 pb-8 font-site text-black shadow-2xl outline-none data-[state=open]:animate-in data-[state=open]:slide-in-from-bottom-8 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 md:inset-x-auto md:bottom-auto md:left-1/2 md:top-1/2 md:w-[480px] md:-translate-x-1/2 md:-translate-y-1/2 md:rounded-3xl md:p-7">
          {open && <Body {...open} rounds={rounds} />}
          <Dialog.Close
            aria-label="Zatvori"
            className="absolute right-4 top-4 flex size-9 items-center justify-center rounded-full bg-black/[0.06] text-black transition-colors hover:bg-black/10"
          >
            <XIcon className="size-4" />
          </Dialog.Close>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function Body({
  series,
  round,
  roundIndex,
  rounds,
}: {
  series: PublicSeries;
  round: PublicRound;
  roundIndex: number;
  rounds: PublicRound[];
}) {
  const side = winnerSide(series);
  const title = series.thirdPlace ? "Meč za 3. mesto" : round.name;
  const status = series.walkover
    ? "Rešeno službeno — protivnik se nije pojavio ili je predao"
    : series.status === "FINISHED"
      ? "Serija je završena"
      : series.status === "IN_PROGRESS"
        ? "Serija je u toku"
        : series.status === "READY"
          ? series.scheduledStart
            ? `${formatDay(series.scheduledStart)}${formatTime(series.scheduledStart) && formatTime(series.scheduledStart) !== "00:00" ? ` u ${formatTime(series.scheduledStart)}` : ""}${series.courtName ? ` · ${series.courtName}` : ""}`
            : "Termin još nije objavljen"
          : "Čeka se pobednik iz prethodnog kola";

  return (
    <div>
      <p className="text-xs font-bold uppercase tracking-wide text-black/45">
        {seriesCode(series)} · {roundFormat(round)}
      </p>
      <Dialog.Title className="mt-1 pr-10 text-2xl font-extrabold">{title}</Dialog.Title>
      <Dialog.Description className="mt-1 text-sm text-black/60">{status}</Dialog.Description>

      <div className="mt-6 grid grid-cols-[1fr_auto_1fr] items-center gap-3">
        <Side player={series.player1} seed={series.seed1} placeholder={feederLabel(rounds, roundIndex, series, 1)} won={side === 1} />
        {series.walkover && series.games.length === 0 ? (
          // Službeno rešeno bez ijednog meča: „0:0 pobeda" bi zbunjivalo, pa stoji samo oznaka.
          <div className="text-center">
            <p className="text-3xl font-extrabold">w.o.</p>
            <p className="mt-1 text-[11px] font-semibold uppercase tracking-wide text-black/40">službeno</p>
          </div>
        ) : (
          <div className="text-center">
            <p className="text-4xl font-extrabold tabular-nums">
              <span className={side === 2 ? "text-black/35" : ""}>{round.winsRequired === 1 ? (series.games[0]?.score1 ?? "–") : series.wins1}</span>
              <span className="mx-1.5 text-black/25">:</span>
              <span className={side === 1 ? "text-black/35" : ""}>{round.winsRequired === 1 ? (series.games[0]?.score2 ?? "–") : series.wins2}</span>
            </p>
            <p className="mt-1 text-[11px] font-semibold uppercase tracking-wide text-black/40">
              {round.winsRequired === 1 ? "rezultat" : "pobede"}
            </p>
          </div>
        )}
        <Side player={series.player2} seed={series.seed2} placeholder={feederLabel(rounds, roundIndex, series, 2)} won={side === 2} align="right" />
      </div>

      {series.games.length > 0 && (
        <ol className="mt-6 divide-y divide-black/10 rounded-2xl border border-black/10">
          {series.games.map((game) => {
            const first = game.score1 > game.score2;
            return (
              <li key={game.matchId} className="flex items-center gap-3 px-4 py-3">
                <span className="w-14 shrink-0 text-xs font-bold uppercase text-black/45">{game.gameNumber}. meč</span>
                <span className="flex-1 text-sm text-black/55">{shortWhen(game.scheduledStart, game.courtName)}</span>
                <span className="text-lg font-extrabold tabular-nums">
                  <span className={first ? "" : "text-black/35"}>{game.score1}</span>
                  <span className="mx-1 text-black/25">:</span>
                  <span className={first ? "text-black/35" : ""}>{game.score2}</span>
                </span>
              </li>
            );
          })}
        </ol>
      )}
    </div>
  );
}

function Side({
  player,
  seed,
  placeholder,
  won,
  align = "left",
}: {
  player: PublicPlayer | null;
  seed: string | null;
  placeholder: string;
  won: boolean;
  align?: "left" | "right";
}) {
  const box = `flex min-w-0 flex-col gap-2 ${align === "right" ? "items-end text-right" : "items-start text-left"}`;
  if (!player) {
    return (
      <div className={box}>
        <span className="size-14 rounded-full border-2 border-dashed border-black/15" />
        <span className="text-sm text-black/40">{placeholder}</span>
      </div>
    );
  }
  return (
    <PlayerLink player={player} className={`${box} group`}>
      <PlayerAvatar name={player.name} src={playerThumb(player)} size={56} />
      <span className="min-w-0">
        <span className={`block truncate text-lg underline-offset-4 group-hover:underline ${won ? "font-extrabold" : "font-semibold"}`}>
          {player.nickname}
        </span>
        <span className="block truncate text-xs text-black/50">{player.name}</span>
        {seed && <span className="mt-0.5 block text-[11px] font-semibold text-black/40">{seed}</span>}
      </span>
    </PlayerLink>
  );
}
