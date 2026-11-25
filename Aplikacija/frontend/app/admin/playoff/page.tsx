import type { Metadata } from "next";
import { PlayoffBoard } from "./playoff-board";

export const metadata: Metadata = {
  title: "Playoff",
};

/**
 * Rok 3 — unos playoff-a (BR-08, BR-09, UC-PLA). Kolo po kolo, serija po serija: rezultat meča,
 * službeni rezultat, termin i par. Pobednik ide sam u sledeće kolo; sve vraća „Poništi" (D-17).
 */
export default function PlayoffPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Playoff</h1>
        <p className="text-sm text-muted-foreground">
          Do četvrtfinala serija na dve pobede, Final Four jedna utakmica. Javni sajt pokazuje drvo na{" "}
          <a href="/playoff" target="_blank" rel="noreferrer" className="underline underline-offset-4">
            /playoff
          </a>{" "}
          (osvežava se za najviše minut).
        </p>
      </div>
      <PlayoffBoard />
    </div>
  );
}
