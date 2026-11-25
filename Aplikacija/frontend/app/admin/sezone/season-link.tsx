"use client";

import Link from "next/link";
import { useSeasons } from "./use-seasons";

/**
 * „Sezona 2026/2027" iznad sadrzaja ekrana — koja sezona se trenutno vodi (najnovija, kao i na
 * ostalim ekranima). Klik vodi na ekran „Sezone".
 */
export function SeasonLink() {
  const seasons = useSeasons();
  const current = seasons.data?.at(-1);

  if (!current) {
    return null;
  }

  return (
    <Link
      href="/admin/sezone"
      className="inline-block text-sm text-muted-foreground underline-offset-4 transition-colors duration-150 ease-out hover:text-foreground hover:underline"
    >
      Sezona {current.name}
    </Link>
  );
}
