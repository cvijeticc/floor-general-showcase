import Image from "next/image";
import Link from "next/link";
import { CONTAINER } from "@/components/site/nav";
import { FormChips } from "@/components/site/results/form-chips";
import { initials } from "@/components/site/results/player-avatar";
import { advanceZone } from "@/components/site/results/standings-table";
import type { PublicPlayerProfile, PublicStandingRow } from "@/lib/site-api";
import { signed } from "@/lib/stat-format";

const ZONE_LABEL = { direct: "Zona prolaza", extra: "Borba za dodatno mesto" } as const;

/**
 * Vrh profila — „sportska kartica, ne tabela iz Excela" (spec §12): slika, NADIMAK, ime, mesto u
 * grupi i forma. Samo statistika — bez visine, težine i godišta (odluka D-21).
 */
export function PlayerHero({
  profile,
  row,
  photo,
  playoff,
}: {
  profile: PublicPlayerProfile;
  row: PublicStandingRow | null;
  photo: string | null;
  /**
   * Oznaka playoff-a; undefined = drvo nije objavljeno (tada važi zona po mestu u grupi),
   * null = drvo postoji, a igrač nije u njemu.
   */
  playoff?: { label: string; tone: "playoff" | "muted" | "gold" } | null;
}) {
  const { player, group } = profile;
  // Posle objave drveta mesto u grupi više ne kaže ko prolazi — kostur je uzimao i 7., 8. i 9. mesto.
  const zone = playoff === undefined && group && row ? advanceZone(row.position, group) : null;

  return (
    <section className="bg-black pb-10 pt-8 font-site text-white xl:pb-14 xl:pt-12">
      <div className={CONTAINER}>
        <nav aria-label="Putanja" className="text-sm text-white/60">
          <ol className="flex flex-wrap items-center gap-1.5">
            <li>
              <Link href="/rezultati" className="transition-colors hover:text-white">
                Rezultati
              </Link>
            </li>
            <li aria-hidden>/</li>
            <li>
              <Link href="/rezultati?tab=igraci" className="transition-colors hover:text-white">
                Igrači
              </Link>
            </li>
            <li aria-hidden>/</li>
            <li aria-current="page" className="text-white/90">
              {player.nickname}
            </li>
          </ol>
        </nav>

        <div className="mt-6 flex flex-col gap-6 sm:flex-row sm:items-center sm:gap-8 xl:gap-12">
          {/* Portret: na laptopu 14" kartica mora da stane u ekran zajedno sa menijem. */}
          <div className="relative aspect-[4/5] w-[140px] shrink-0 overflow-hidden rounded-[20px] bg-[linear-gradient(160deg,rgb(64,64,64)_0%,rgb(20,20,20)_100%)] sm:w-[170px] xl:w-[200px]">
            {photo ? (
              <Image src={photo} alt={player.name} fill priority unoptimized sizes="200px" className="object-cover" />
            ) : (
              <span
                aria-hidden
                className="absolute inset-0 flex items-center justify-center text-[56px] font-extrabold tracking-tight text-white/85 xl:text-[72px]"
              >
                {initials(player.name)}
              </span>
            )}
          </div>

          <div className="min-w-0">
            {group && (
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-white/55">
                {group.stageName} · {group.name}
              </p>
            )}
            <h1 className="mt-1 break-words text-[44px] font-extrabold uppercase leading-[0.95] tracking-[-0.03em] xl:text-[68px]">
              {player.nickname}
            </h1>
            <p className="mt-2 text-xl text-white/75 xl:text-2xl">{player.name}</p>

            {row && group && (
              <ul className="mt-5 flex flex-wrap items-center gap-2 text-sm font-bold">
                <li className="rounded-full bg-white px-3.5 py-1.5 text-black">
                  {row.position}. mesto · {group.name}
                </li>
                {playoff && (
                  <li>
                    <Link
                      href="/playoff"
                      className={`block rounded-full px-3.5 py-1.5 ${
                        playoff.tone === "playoff"
                          ? "bg-playoff text-white"
                          : playoff.tone === "gold"
                            ? "bg-prize-gold text-black"
                            : "bg-white/15 text-white"
                      }`}
                    >
                      {playoff.label}
                    </Link>
                  </li>
                )}
                {zone && (
                  <li
                    className={`rounded-full px-3.5 py-1.5 ${zone === "direct" ? "bg-emerald-500 text-black" : "bg-prize-gold text-black"}`}
                  >
                    {ZONE_LABEL[zone]}
                  </li>
                )}
                <li className="rounded-full border border-white/30 px-3.5 py-1.5">
                  {row.wins}–{row.losses} · {signed(row.pointDifference)}
                </li>
              </ul>
            )}

            {profile.form.length > 0 && (
              <div className="mt-5 flex items-center gap-3">
                <span className="text-sm text-white/60">Forma</span>
                <FormChips items={profile.form.slice(-5)} size="md" />
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
