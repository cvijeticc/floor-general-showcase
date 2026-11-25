import { PlayerLink } from "@/components/site/results/player-link";
import type { PublicPlayerProfile } from "@/lib/site-api";
import { roundLabel } from "@/lib/site-api";
import { viewFor } from "./match-view";

/**
 * Rekordi sezone: najubedljivija pobeda i najteži poraz. Službeni rezultat (11:0 bez igre)
 * se ne računa — to odlučuje backend (engine/PlayerForm), ovde se samo prikazuje.
 */
export function PlayerRecords({ profile }: { profile: PublicPlayerProfile }) {
  const records = [
    { label: "Najubedljivija pobeda", matchId: profile.biggestWinMatchId },
    { label: "Najteži poraz", matchId: profile.heaviestLossMatchId },
  ];

  return (
    <section aria-labelledby="rekordi" className="rounded-2xl border border-black/10 p-5">
      <h2 id="rekordi" className="text-xl font-bold">
        Rekordi sezone
      </h2>
      <dl className="mt-4 space-y-4">
        {records.map((record) => {
          const match = profile.matches.find((m) => m.id === record.matchId);
          const view = match ? viewFor(match, profile.player.id) : null;
          return (
            <div key={record.label}>
              <dt className="text-sm text-black/55">{record.label}</dt>
              <dd className="mt-1">
                {view ? (
                  <>
                    <span className="text-2xl font-extrabold">
                      {view.scored}:{view.conceded}
                    </span>{" "}
                    <span className="text-black/60">
                      protiv{" "}
                      <PlayerLink player={view.opponent} className="font-semibold text-black underline-offset-4 hover:underline">
                        {view.opponent.nickname}
                      </PlayerLink>{" "}
                      · {roundLabel(view.match)}
                    </span>
                  </>
                ) : (
                  <span className="text-black/45">—</span>
                )}
              </dd>
            </div>
          );
        })}
      </dl>
    </section>
  );
}
