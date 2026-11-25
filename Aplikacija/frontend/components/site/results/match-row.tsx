import { playerThumb } from "@/lib/player-photos";
import { formatDate, formatTime, type PublicMatch } from "@/lib/site-api";
import { PlayerAvatar } from "./player-avatar";
import { PlayerLink } from "./player-link";

export const STATUS_NOTE: Partial<Record<PublicMatch["status"], string>> = {
  WALKOVER: "službeni rezultat",
  FORFEIT: "predaja",
  DOUBLE_FORFEIT: "obojica kažnjena",
  POSTPONED: "odložen",
  IN_PROGRESS: "u toku",
};

/** Meč na tabovima „Rezultati" i „Raspored" (UC-PUB-04/05): oba imena vode na profil igrača. */
export function MatchRow({ match: m, showGroup, played }: { match: PublicMatch; showGroup: boolean; played: boolean }) {
  const won1 = m.winnerId === m.player1.id;
  const won2 = m.winnerId === m.player2.id;
  const playoff = m.stageType === "KNOCKOUT";
  const meta = [
    playoff ? [m.roundName, m.gameNumber ? `${m.gameNumber}. meč` : null].filter(Boolean).join(" · ") : showGroup ? m.groupName : null,
    played ? formatDate(m.scheduledStart) : formatTime(m.scheduledStart),
    m.courtName,
    played || playoff ? null : `${m.roundNumber}. kolo`,
    STATUS_NOTE[m.status],
  ].filter(Boolean);

  return (
    <li className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 py-4 md:gap-6">
      <PlayerName player={m.player1} winner={won1} align="right" />
      <div className="flex min-w-[96px] flex-col items-center">
        {played ? (
          <span className="text-2xl font-extrabold tabular-nums md:text-[28px]">
            <span className={won1 ? "" : "text-black/40"}>{m.scorePlayer1}</span>
            <span className="mx-1.5 text-black/30">:</span>
            <span className={won2 ? "" : "text-black/40"}>{m.scorePlayer2}</span>
          </span>
        ) : (
          <span className="rounded-full bg-black px-3 py-1 text-sm font-bold text-white">
            {formatTime(m.scheduledStart) || "vs"}
          </span>
        )}
        <span className="mt-1 text-center text-xs text-black/50">{meta.join(" · ")}</span>
      </div>
      <PlayerName player={m.player2} winner={won2} align="left" />
    </li>
  );
}

/** Slika je uz rezultat (unutrašnja strana), ime prema ivici — kao na SofaScore-u. */
function PlayerName({
  player,
  winner,
  align,
}: {
  player: PublicMatch["player1"];
  winner: boolean;
  align: "left" | "right";
}) {
  return (
    <PlayerLink
      player={player}
      className={`group flex min-w-0 items-center gap-3 ${align === "right" ? "flex-row-reverse text-right" : "text-left"}`}
    >
      <PlayerAvatar name={player.name} src={playerThumb(player)} size={40} className="hidden sm:inline-flex" />
      <span className="min-w-0">
        <span
          className={`block text-base underline-offset-4 group-hover:underline md:text-lg ${winner ? "font-extrabold" : "font-semibold"}`}
        >
          {player.nickname}
        </span>
        <span className="block text-xs text-black/50 md:text-sm">{player.name}</span>
      </span>
    </PlayerLink>
  );
}
