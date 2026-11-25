import type { Metadata } from "next";
import Link from "next/link";
import { CONTAINER } from "@/components/site/nav";
import { BracketViewer } from "@/components/site/playoff/bracket-viewer";
import { PlayoffRules } from "@/components/site/playoff/playoff-rules";
import { currentRoundIndex, mainSeries } from "@/lib/bracket";
import { getPlayoff } from "@/lib/site-api";

// Na zahtev, ne pri build-u: u Docker build-u backend ne postoji, pa bi statična strana ostala sa
// porukom o grešci dok je ISR ne osveži. Podaci se i dalje keširaju 60 s (lib/site-api.ts).
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Playoff — drvo Top 64",
  description:
    "Playoff Floor General 1 na 1 lige: drvo od 64 igrača, serije na dve pobede do četvrtfinala, Final Four i meč za 3. mesto. Rezultati svake serije, uživo posle svakog meča.",
  alternates: { canonical: "/playoff" },
};

/**
 * UC-PLA-01 — javno drvo playoff-a (SSR, keš 60 s kao ostatak sajta). Drvo se otvara na kolu koje
 * se trenutno igra; ispod su pravila svake faze.
 */
export default async function PlayoffPage() {
  const result = await getPlayoff();
  const bracket = result.status === "ok" ? result.data : null;
  const first = bracket ? mainSeries(bracket.rounds[0]) : [];
  const decided = bracket ? bracket.rounds.flatMap((r) => r.series).filter((s) => s.status === "FINISHED").length : 0;

  return (
    <>
      <section className="relative overflow-hidden bg-black pb-10 pt-12 font-site text-white xl:pb-14 xl:pt-[80px]">
        <div
          aria-hidden
          className="pointer-events-none absolute -right-40 -top-40 size-[520px] rounded-full bg-playoff/25 blur-3xl"
        />
        <div className={`${CONTAINER} relative`}>
          <p className="inline-flex items-center gap-2 rounded-full bg-playoff px-3 py-1 text-xs font-bold uppercase tracking-wide">
            <span aria-hidden className="relative flex size-2">
              <span className="absolute inline-flex size-full animate-ping rounded-full bg-white/80 motion-reduce:animate-none" />
              <span className="relative inline-flex size-2 rounded-full bg-white" />
            </span>
            Playoff je počeo
          </p>
          <h1 className="mt-4 text-[42px] font-bold leading-[1.1] xl:text-[64px]">
            PLAYOFF <span className="text-playoff">TOP {bracket?.bracketSize ?? 64}</span>
          </h1>
          <p className="mt-3 max-w-2xl text-lg text-white/70">
            Put do titule: {bracket?.bracketSize ?? 64} igrača, {first.length || 32} serija u prvom kolu, do četvrtfinala na dve
            pobede, a Final Four na jednu utakmicu. Pobednik osvaja 500.000 dinara.
          </p>
          <dl className="mt-8 grid max-w-xl grid-cols-3 gap-4">
            <Fact label="igrača" value={String(bracket?.bracketSize ?? 64)} />
            <Fact label="serija odigrano" value={String(decided)} />
            <Fact label="nagradni fond" value="650.000" />
          </dl>
        </div>
      </section>

      <section className="min-h-[50vh] bg-[#f4f4f5] pb-24 pt-8 font-site text-black xl:pt-12">
        <div className={CONTAINER}>
          {bracket ? (
            <BracketViewer bracket={bracket} initialRound={currentRoundIndex(bracket)} />
          ) : (
            <p className="py-16 text-center text-lg text-black/60">
              {result.status === "not-found"
                ? "Drvo playoff-a još nije objavljeno."
                : "Drvo trenutno nije dostupno. Pokušaj ponovo za par minuta."}
            </p>
          )}

          {bracket && <PlayoffRules rounds={bracket.rounds} />}

          <p className="mt-12 text-center text-sm text-black/55">
            Tabele grupne faze, svi rezultati i statistika igrača su na strani{" "}
            <Link href="/rezultati" className="font-bold text-black underline underline-offset-4">
              Rezultati
            </Link>
            .
          </p>
        </div>
      </section>
    </>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="sr-only">{label}</dt>
      <dd className="text-2xl font-extrabold sm:text-3xl xl:text-4xl">{value}</dd>
      <dd className="text-xs uppercase tracking-wide text-white/55">{label}</dd>
    </div>
  );
}
