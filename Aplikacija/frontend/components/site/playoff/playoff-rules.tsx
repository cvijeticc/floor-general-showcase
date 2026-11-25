import { roundFormat } from "@/lib/bracket";
import type { PublicRound } from "@/lib/site-api";

/**
 * Pravila playoff-a iz obaveštenja lige (grupa, 08. i 09.11.2026.). Format (broj pobeda, do koliko
 * poena, koliko minuta) dolazi iz kola (stage_rounds) — ovde je samo ono što je opis, ne pravilo
 * koje sistem primenjuje: tereni, sudije, čelendž, prenos.
 */
const ROUND_NOTES: Record<string, string[]> = {
  "Top 64": [
    "Igra se na obe strane terena, kao i do sada",
    "1 čelendž — ako je uspešan, vraća ti se",
    "1 timeout",
  ],
  "Top 32": [
    "Igra se samo na terenu A",
    "Dve sudije sude meč u isto vreme",
    "Nova aut linija na polovini terena",
    "Komentator i prenos uživo svakog meča",
    "1 čelendž i 1 timeout",
  ],
  "Top 16": [
    "Sve kao u Top 32",
    "Od ove faze igrači ne plaćaju ništa — troškove do kraja preuzima liga",
    "Svaki meč se dodatno snima za društvene mreže",
  ],
  Četvrtfinale: ["Sve kao u Top 16 (Top 8)"],
  Polufinale: [
    "Final Four: jedna utakmica",
    "2 timeouta od po 45 sekundi, čelendž",
    "U bonus se ulazi od 4. faula, a ne od 3.",
  ],
  Finale: ["Pobednik osvaja 500.000 dinara, drugo mesto 100.000, treće 50.000"],
};

const GENERAL = [
  "Ceo krug se igra istog dana — subota ili nedelja — dva ili tri meča, dok neko ne dođe do dve pobede.",
  "Ko se ne pojavi, protivnik automatski prolazi dalje.",
  "Odbrambeni igrač pri uručivanju lopte stoji korak od napadača i baca loptu o pod; tek kad je napadač uhvati, sme da priđe.",
  "Suđenje je pooštreno: nepotrebno komentarisanje, prigovori i agresivnost kažnjavaju se tehničkom greškom (protivnik ima bacanje).",
  "Više od 3 tehničke greške u jednoj rundi (2 ili 3 meča sa istim protivnikom) — automatski izgubljen meč.",
  "Preterana agresivnost prema sudiji, protivniku ili organizatorima — diskvalifikacija.",
  "Nema više weekly challenge-a, ali i dalje se biraju top 3 poteza runde.",
];

export function PlayoffRules({ rounds }: { rounds: PublicRound[] }) {
  return (
    <section aria-labelledby="pravila-playoffa" className="mt-16">
      <h2 id="pravila-playoffa" className="text-[28px] font-bold leading-tight md:text-[36px]">
        Pravila playoff-a
      </h2>
      <p className="mt-2 max-w-2xl text-base text-black/60">
        Kako se igra svaka faza, od Top 64 do finala — iz obaveštenja lige igračima.
      </p>

      <ol className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {rounds.map((round) => (
          <li key={round.roundNo} className="rounded-2xl border border-black/10 bg-white p-5">
            <p className="text-xs font-bold uppercase tracking-wide text-playoff">{round.roundNo}. kolo</p>
            <h3 className="mt-1 text-xl font-extrabold">{round.name}</h3>
            <p className="mt-1 text-sm font-semibold">{roundFormat(round)}</p>
            {ROUND_NOTES[round.name] && (
              <ul className="mt-3 space-y-1.5 text-sm text-black/70">
                {ROUND_NOTES[round.name].map((note) => (
                  <li key={note} className="flex gap-2">
                    <span aria-hidden className="mt-[7px] size-1.5 shrink-0 rounded-full bg-black/30" />
                    {note}
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ol>

      <div className="mt-6 rounded-2xl bg-black p-6 text-white md:p-8">
        <h3 className="text-xl font-extrabold">Važi za ceo playoff</h3>
        <ul className="mt-4 grid gap-3 text-sm text-white/80 md:grid-cols-2">
          {GENERAL.map((rule) => (
            <li key={rule} className="flex gap-2">
              <span aria-hidden className="mt-[7px] size-1.5 shrink-0 rounded-full bg-playoff" />
              {rule}
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
