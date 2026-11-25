import type { Metadata } from "next";
import { StandingsTable } from "./standings-table";

export const metadata: Metadata = {
  title: "Tabele",
};

/**
 * UC-FE-08 — tabela grupe. Čita materijalizovanu tabelu (FR-6.5.1) koju preračun puni posle
 * svake izmene rezultata; dugme „Preračunaj" postoji za oporavak i posle uvoza podataka.
 */
export default function StandingsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Tabele grupa</h1>
        <p className="text-sm text-muted-foreground">
          Plasman se računa iz unetih rezultata i osvežava se sam posle svakog unosa.
        </p>
      </div>

      <StandingsTable />
    </div>
  );
}
