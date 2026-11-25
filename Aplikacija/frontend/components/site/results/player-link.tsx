import Link from "next/link";
import { playerHref } from "@/lib/player-url";
import type { PublicPlayer } from "@/lib/site-api";

/**
 * Svako ime igrača na sajtu vodi na njegov profil (UC-PUB-06). Bez prefetch-a: „Sve grupe" i
 * rezultati imaju po stotinak imena, a prefetch bi za svako vidljivo ime povukao ceo profil.
 */
export function PlayerLink({
  player,
  className,
  children,
}: {
  player: Pick<PublicPlayer, "id" | "name">;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <Link href={playerHref(player)} prefetch={false} className={className}>
      {children}
    </Link>
  );
}
