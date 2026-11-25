import type { Metadata } from "next";
import { MatchesTable } from "./matches-table";

export const metadata: Metadata = {
  title: "Mečevi",
};

/**
 * UC-FE-07 (+ UC-FE-06) — mečevi i rezultati na jednom ekranu.
 * <p>
 * Unos meča je: ko je igrao s kim, kog datuma, u koliko sati, na kom terenu i sa kojim rezultatom.
 * Rezultati celog kola se upisuju odjednom, sa ovog istog spiska — vikendom se odigra ~60 mečeva
 * koje uveče jedna osoba unosi zaredom (CLAUDE.md §3.1), pa svako otvaranje meča posebno košta.
 */
export default function MatchesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Mečevi</h1>
        <p className="text-sm text-muted-foreground">
          Zakazivanje i unos odigranih mečeva, rezultati celog kola odjednom, službeni rezultat i
          ispravka. Pogrešan unos se vraća dugmetom „Poništi“ gore desno (Ctrl+Z).
        </p>
      </div>

      <MatchesTable />
    </div>
  );
}
