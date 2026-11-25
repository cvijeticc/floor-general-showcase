import type { PublicPlayerProfile, PublicStandingRow } from "@/lib/site-api";
import { decimal, differenceColor, percent, signed, streakLabel } from "@/lib/stat-format";

/**
 * Statistika igrača kao pločice (stat tiles). Bilans je red iz tabele grupe — isti broj kao na
 * tabu „Tabele"; nizovi dolaze iz odigranih mečeva (backend, engine/PlayerForm).
 */
export function PlayerStats({ profile, row }: { profile: PublicPlayerProfile; row: PublicStandingRow | null }) {
  const played = row?.played ?? 0;

  return (
    <section aria-labelledby="statistika">
      <h2 id="statistika" className="sr-only">
        Statistika
      </h2>
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <Tile label="Odigrano" value={played} />
        <Tile label="Pobede" value={row?.wins ?? 0} />
        <Tile label="Porazi" value={row?.losses ?? 0} />
        <Tile label="Uspešnost" value={percent(row?.wins ?? 0, played)} />
        <Tile
          label="Koš-razlika (+/−)"
          value={signed(row?.pointDifference ?? 0)}
          className={differenceColor(row?.pointDifference ?? 0)}
        />
        <Tile label="Bodovi" value={row?.points ?? 0} />
      </dl>
      <dl className="mt-3 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Tile label="Poena po meču" value={played > 0 && row ? decimal(row.pointsFor / played) : "—"} />
        <Tile label="Primljeno po meču" value={played > 0 && row ? decimal(row.pointsAgainst / played) : "—"} />
        <Tile label="Trenutni niz" value={streakLabel(profile.currentStreak)} />
        <Tile label="Najduži niz pobeda" value={profile.longestWinStreak} />
      </dl>
    </section>
  );
}

function Tile({ label, value, className = "" }: { label: string; value: React.ReactNode; className?: string }) {
  return (
    <div className="rounded-2xl border border-black/10 px-4 py-3.5">
      <dt className="text-sm text-black/55">{label}</dt>
      <dd className={`mt-1.5 text-[26px] font-extrabold leading-none tracking-tight ${className}`}>{value}</dd>
    </div>
  );
}
