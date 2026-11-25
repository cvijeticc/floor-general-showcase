import manifest from "@/lib/player-photos.json";
import { slugify } from "@/lib/player-url";

/**
 * Slike igrača su statični fajlovi (odluka D-21), ne kolona u bazi: portret
 * public/igraci/{slug}.jpg (640×800) i mala slika public/igraci/mini/{slug}.webp (160×160),
 * gde je slug ime i prezime bez dijakritika — „milos-ristic". Fajlove i ovaj spisak
 * (player-photos.json) pravi scripts/player-photos.mjs. Igrač bez slike dobija inicijale.
 * Spisak je običan JSON, pa radi i na serveru i u pregledaču (padajuća lista pretrage).
 */
type PhotoEntry = { photo: string; thumb: string };

const PHOTOS: Record<string, PhotoEntry | undefined> = manifest;

/** Velika slika za profil i za pregled linka (og:image), ili null. */
export function playerPhoto(player: { name: string }): string | null {
  return PHOTOS[slugify(player.name)]?.photo ?? null;
}

/** Mala slika za tabele, mečeve i spiskove, ili null. */
export function playerThumb(player: { name: string }): string | null {
  return PHOTOS[slugify(player.name)]?.thumb ?? null;
}
