import type { Metadata } from "next";
import { SeasonLink } from "../sezone/season-link";
import { PlayersTable } from "./players-table";

export const metadata: Metadata = {
  title: "Igrači",
};

/**
 * UC-FE-05 — sifarnik igraca lige: pretraga po imenu, prezimenu i nadimku, filter po grupi
 * i statusu, dodavanje, izmena, promena statusa (BR-10) i brisanje.
 * Admin deo namerno nema SSR (CLAUDE.md §5) — bitna je brzina rada, ne SEO.
 */
export default function PlayersPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Igrači</h1>
        <p className="text-sm text-muted-foreground">
          Šifarnik igrača lige. Raspoređivanje u grupe je na ekranu „Grupe“.
        </p>
      </div>

      <SeasonLink />

      <PlayersTable />
    </div>
  );
}
