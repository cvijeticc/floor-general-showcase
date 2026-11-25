import type { Metadata } from "next";
import { SeasonsList } from "./seasons-list";

export const metadata: Metadata = {
  title: "Sezone",
};

/** Spisak sezona lige. Za sada je jedna (2026/2027); svaka sledeca se samo pojavi u listi. */
export default function SeasonsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Sezone</h1>
        <p className="text-sm text-muted-foreground">
          Izaberi sezonu da vidiš njene faze, parametre bodovanja i grupe.
        </p>
      </div>

      <SeasonsList />
    </div>
  );
}
