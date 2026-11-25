import type { MetadataRoute } from "next";
import { playerHref } from "@/lib/player-url";
import { getPlayers } from "@/lib/site-api";
import { SITE_URL } from "@/lib/site-url";

// Pravi se na zahtev, ne pri build-u: u Docker build-u backend ne postoji, pa bi sitemap
// zauvek ostao bez igrača. Sam spisak igrača se i dalje kešira 60 s (lib/site-api.ts).
export const dynamic = "force-dynamic";

/** UC-PUB-09 — javne strane i profil svakog igrača („najvažniji SEO ulaz"). */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const players = (await getPlayers()) ?? [];

  return [
    { url: `${SITE_URL}/`, changeFrequency: "weekly", priority: 1 },
    { url: `${SITE_URL}/playoff`, changeFrequency: "daily", priority: 0.95 },
    { url: `${SITE_URL}/rezultati`, changeFrequency: "daily", priority: 0.9 },
    { url: `${SITE_URL}/rezultati?tab=statistika`, changeFrequency: "daily", priority: 0.8 },
    { url: `${SITE_URL}/rezultati?tab=igraci`, changeFrequency: "daily", priority: 0.8 },
    { url: `${SITE_URL}/prijavi-se`, changeFrequency: "monthly", priority: 0.6 },
    { url: `${SITE_URL}/kontakt`, changeFrequency: "monthly", priority: 0.5 },
    ...players.map((p) => ({
      url: `${SITE_URL}${playerHref(p.player)}`,
      changeFrequency: "weekly" as const,
      priority: 0.7,
    })),
  ];
}
