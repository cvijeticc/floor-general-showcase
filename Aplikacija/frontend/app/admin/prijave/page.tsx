import type { Metadata } from "next";
import { ExportButton } from "./export-button";
import { ApplicationsTable } from "./applications-table";
import { MailStatusCard } from "./mail-status";

export const metadata: Metadata = {
  title: "Prijave",
};

/**
 * D-20 — prijave u ligu sa forme na javnom sajtu (/prijavi-se). Ovde se prate, a „Napravi igrača"
 * ih prebacuje u šifarnik igrača. „Preuzmi Excel" izvozi celu bazu (igrači, mečevi, tabele, prijave).
 */
export default function ApplicationsPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Prijave</h1>
          <p className="text-sm text-muted-foreground">
            Prijave u ligu sa sajta. Baza je glavni izvor — Excel je kopija koja se uvek pravi iznova.
          </p>
        </div>
        <ExportButton />
      </div>

      <MailStatusCard />

      <ApplicationsTable />
    </div>
  );
}
