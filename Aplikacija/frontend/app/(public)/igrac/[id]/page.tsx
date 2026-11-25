import type { Metadata } from "next";
import { notFound, permanentRedirect } from "next/navigation";
import { CONTAINER } from "@/components/site/nav";
import { FormChart } from "@/components/site/player/form-chart";
import { HeadToHead } from "@/components/site/player/head-to-head";
import { isUpcoming } from "@/components/site/player/match-view";
import { PlayerHero } from "@/components/site/player/player-hero";
import { NextMatch, PlayerMatches } from "@/components/site/player/player-matches";
import { PlayerRecords } from "@/components/site/player/player-records";
import { PlayerStats } from "@/components/site/player/player-stats";
import { StandingsTable } from "@/components/site/results/standings-table";
import { playerPhoto } from "@/lib/player-photos";
import { parsePlayerParam, playerHref } from "@/lib/player-url";
import { PlayerPlayoff, playoffBadge, playoffEntries } from "@/components/site/player/player-playoff";
import { getPlayerProfile, getPlayoff, type PublicPlayerProfile } from "@/lib/site-api";
import { SITE_URL } from "@/lib/site-url";
import { plural, signed } from "@/lib/stat-format";

type Props = { params: Promise<{ id: string }> };

/**
 * UC-PUB-06 — profil igrača, /igrac/94-milos-ristic. „Najvažniji SEO ulaz" (UC-PUB-09): SSR,
 * generateMetadata i schema.org. Samo statistika — bez visine, težine i godišta (odluka D-21).
 *
 * notFound() i permanentRedirect() su namerno van try/catch-a i bez loading.tsx: tako 404 ostaje
 * 404, a preusmerenje 308 — ne 200 strana koju bi nginx keširao.
 */
export default async function PlayerPage({ params }: Props) {
  const { id } = await params;
  const playerId = parsePlayerParam(id);
  if (!playerId) notFound();

  const [result, playoff] = await Promise.all([getPlayerProfile(playerId), getPlayoff()]);
  if (result.status === "not-found") notFound();
  // Backend ne odgovara: greška (500) umesto prazne strane — nginx tada daje poslednju dobru kopiju.
  if (result.status === "error") throw new Error("Profil igrača trenutno nije dostupan");

  const profile = result.data;
  const canonical = playerHref(profile.player);
  if (`/igrac/${id}` !== canonical) permanentRedirect(canonical);

  const { player, group } = profile;
  const row = profile.groupStandings.find((r) => r.player.id === player.id) ?? null;
  const upcoming = profile.matches.filter(isUpcoming);
  const nextMatch = upcoming.find((m) => m.status !== "POSTPONED") ?? upcoming[0];
  const photo = playerPhoto(player);
  const bracket = playoff.status === "ok" ? playoff.data : null;
  const entries = playoffEntries(bracket, player.id);
  const badge = bracket ? playoffBadge(entries, player.id, bracket.rounds.at(-1)?.roundNo ?? 0) : undefined;

  return (
    <>
      <script
        type="application/ld+json"
        // Escapovanje „<": ime iz baze ne sme da zatvori <script>.
        dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData(profile, photo)).replace(/</g, "\\u003c") }}
      />

      <PlayerHero profile={profile} row={row} photo={photo} playoff={badge} />

      <section className="bg-white pb-24 pt-8 font-site text-black xl:pt-12">
        <div className={CONTAINER}>
          <PlayerStats profile={profile} row={row} />
          {nextMatch && <NextMatch player={player} match={nextMatch} />}

          <div className="mt-12 grid gap-12 lg:grid-cols-[minmax(0,1fr)_360px] lg:gap-10">
            <div className="min-w-0 space-y-12">
              <PlayerPlayoff player={player} entries={entries} />
              <FormChart items={profile.form} />
              <PlayerMatches player={player} matches={profile.matches} />
              <HeadToHead rows={profile.headToHead} />
            </div>
            <aside className="min-w-0 space-y-10">
              {group && (
                <StandingsTable
                  group={group}
                  rows={profile.groupStandings}
                  highlightPlayerId={player.id}
                  heading={<h2 className="text-2xl font-bold">Tabela — {group.name}</h2>}
                />
              )}
              <PlayerRecords profile={profile} />
            </aside>
          </div>
        </div>
      </section>
    </>
  );
}

/**
 * Naslov i opis za Google i za pregled linka (Instagram, WhatsApp). Ovde se nikad ne zove
 * notFound(): Next 15.5 metapodatke šalje u toku, pa bi 404 postao 200 — strana sama vraća 404.
 */
export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const playerId = parsePlayerParam(id);
  const result = playerId ? await getPlayerProfile(playerId) : null;
  if (!result || result.status !== "ok") return { title: "Igrač", robots: { index: false } };

  const { player, group, groupStandings } = result.data;
  const row = groupStandings.find((r) => r.player.id === player.id);
  const title = `${player.nickname} — ${player.name}`;
  const description =
    row && group
      ? `${player.name} „${player.nickname}“ — ${group.name}, ${row.position}. mesto: ${row.wins} ${plural(row.wins, "pobeda", "pobede", "pobeda")}, ${row.losses} ${plural(row.losses, "poraz", "poraza", "poraza")}, koš-razlika ${signed(row.pointDifference)}. Forma, svi mečevi i međusobni dueli u Floor General 1 na 1 ligi.`
      : `${player.name} „${player.nickname}“ — profil igrača Floor General 1 na 1 košarkaške lige.`;
  const photo = playerPhoto(player);

  return {
    title,
    description,
    // openGraph strane zamenjuje onaj iz root layout-a (ne spaja se), pa se sve navodi ponovo.
    openGraph: {
      type: "profile",
      username: player.nickname,
      title,
      description,
      locale: "sr_RS",
      siteName: "Floor General 1 na 1 Liga",
      images: [photo ?? "/site/og.png"],
    },
  };
}

/** schema.org (FR-6.1.13): ProfilePage + Person; SportsEvent samo za mečeve sa datumom (Google traži startDate). */
function structuredData(profile: PublicPlayerProfile, photo: string | null) {
  const person = (p: PublicPlayerProfile["player"]) => ({
    "@type": "Person",
    name: p.name,
    alternateName: p.nickname,
    url: `${SITE_URL}${playerHref(p)}`,
  });

  const events = profile.matches
    .filter((m) => m.scheduledStart)
    .map((m) => ({
      "@type": "SportsEvent",
      name: `${m.player1.nickname} – ${m.player2.nickname}`,
      sport: "Basketball",
      startDate: m.scheduledStart,
      competitor: [person(m.player1), person(m.player2)],
      ...(m.courtName && { location: { "@type": "Place", name: m.courtName } }),
    }));

  return {
    "@context": "https://schema.org",
    "@type": "ProfilePage",
    mainEntity: {
      ...person(profile.player),
      ...(photo && { image: `${SITE_URL}${photo}` }),
      memberOf: { "@type": "SportsOrganization", name: "Floor General 1 na 1 Liga", sport: "Basketball" },
      ...(events.length > 0 && { performerIn: events }),
    },
  };
}
