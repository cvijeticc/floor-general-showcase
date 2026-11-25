import Image from "next/image";
import { cn } from "@/lib/utils";

type Props = {
  /** Ime i prezime — za inicijale i za alt kad je slika sama (bez imena pored). */
  name: string;
  src: string | null;
  /** Prečnik u pikselima. */
  size: number;
  /** Slika stoji pored ispisanog imena → alt je prazan, da čitač ekrana ne čita ime dvaput. */
  decorative?: boolean;
  /** „dark" za crnu traku na vrhu strane. */
  tone?: "light" | "dark";
  className?: string;
};

/**
 * Slika igrača (odluka D-21) ili inicijali dok slika ne stigne — kao SofaScore. Slike su unapred
 * smanjene (scripts/player-photos.mjs), pa `unoptimized`: produkcijski /_next/image ih ionako ne menja.
 */
export function PlayerAvatar({ name, src, size, decorative = true, tone = "light", className = "" }: Props) {
  // cn spaja klase kao tailwind-merge: „hidden @md:inline-flex" od pozivaoca mora da pregazi
  // podrazumevani „inline-flex", inače se avatar vidi i tamo gde je trebalo da bude sakriven.
  const box = (extra: string) =>
    cn("relative inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full", extra, className);

  if (src) {
    return (
      <span className={box("bg-black/5")} style={{ width: size, height: size }}>
        <Image
          src={src}
          alt={decorative ? "" : name}
          width={size}
          height={size}
          unoptimized
          className="size-full object-cover"
        />
      </span>
    );
  }

  return (
    <span
      aria-hidden={decorative || undefined}
      role={decorative ? undefined : "img"}
      aria-label={decorative ? undefined : name}
      className={box(
        `font-bold tracking-tight ${tone === "dark" ? "bg-white/15 text-white" : "bg-black/[0.07] text-black/55"}`,
      )}
      style={{ width: size, height: size, fontSize: Math.max(10, Math.round(size * 0.36)) }}
    >
      {initials(name)}
    </span>
  );
}

/** „Miloš Ristić" → „MR". */
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toUpperCase();
}
