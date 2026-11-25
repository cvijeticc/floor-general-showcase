import type { PublicPlayer } from "@/lib/site-api";

const SERBIAN_LATIN: Record<string, string> = { č: "c", ć: "c", š: "s", ž: "z", đ: "dj" };

/**
 * „Miloš Ristić" → „milos-ristic". Isti slug je i deo URL-a profila i ime fajla slike igrača
 * (public/igraci/{slug}.webp, odluka D-21) — scripts/player-photos.mjs ima istu funkciju.
 */
export function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/[čćšžđ]/g, (ch) => SERBIAN_LATIN[ch])
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

/** Profil igrača (UC-PUB-06): /igrac/94-milos-ristic — ID nosi podatak, ime je tu zbog ljudi i Google-a. */
export function playerHref(player: Pick<PublicPlayer, "id" | "name">): string {
  const slug = slugify(player.name);
  return slug ? `/igrac/${player.id}-${slug}` : `/igrac/${player.id}`;
}

/** „94-milos-ristic" (ili samo „94") → 94; sve ostalo → null. */
export function parsePlayerParam(param: string): number | null {
  const match = /^(\d{1,15})(?:-|$)/.exec(param);
  if (!match) return null;
  const id = Number(match[1]);
  return id > 0 ? id : null;
}
