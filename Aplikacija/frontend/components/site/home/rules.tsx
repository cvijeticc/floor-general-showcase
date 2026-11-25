import { InView } from "../motion";
import { ANCHOR_TARGET, CONTAINER } from "../nav";

/**
 * Pravilnik. Tačke 1 i 2 su ispravljene po odluci D-18 (CLAUDE.md §4): ko prvi stigne do 11
 * pobeđuje, ali trojka posle 10 daje 12; ako vreme od 8 minuta istekne, pobeđuje onaj ko vodi.
 * Tačke 7 i 9 (11.11.2026., zahtev lige): posle faula je uvek JEDNO bacanje, nikad dva —
 * pogođeno bacanje znači da lopta ostaje fauliranom igraču.
 */
const RULES = [
  "Igra se do 11 poena — ko prvi stigne do 11, pobedio je. Šut ispod trojke vredi 1 poen, trojka vredi 2 poena, pa ko ima 10 i pogodi trojku završava sa 12",
  "Trajanje meča - 8 minuta. Ako niko ne stigne do 11, pobeđuje onaj ko ima više poena kada istekne vreme",
  "Trajanje napada - 10 sekundi",
  "Poslednjih 1:30 minuta meča, vreme se zaustavlja na svaki prekid",
  "Prvi posed dobija igrač koji je pobedio u igri papir kamen makaze",
  "Svaki igrač ima samo jedan pokušaj šuta na koš. Ako je postigao koš, lopta ostaje u njegovom posedu. Ako je promašio, lopta pripada protivniku",
  "Nakon faula pri šutu, faulirani igrač ima jedno slobodno bacanje — i kad je fauliran pri šutu za 2 poena, bacanje je samo jedno, ne dva. Ako pogodi, lopta ostaje u njegovom posedu. Ako promaši, posed pripada protivniku",
  "Nakon 3. faula igrača na meču, igrač će ući u bonus i svaki naredni faul će biti bacanje za protivnika",
  "U slučaju faul-koša, faulirani igrač ima jedno slobodno bacanje. Ako ga pogodi, lopta ostaje u njegovom posedu",
  "Ako je rezultat izjednačen nakon isteka meča, igra se na zlatni koš (ko prvi da koš, pobeđuje)",
  "Sudijina odluka je konačna. Ako igrač pređe granicu, biće kažnjen tehničkom greškom, a protivnik će u tom slučaju imati 1 slobodno bacanje",
  "U slučaju nesportske greške, protivnik će imati 2 slobodna bacanja i lopta će biti u njegovom posedu, nezavisno od datog bacanja",
  "U slučaju izuzetnog nesportskog ponašanja, igrač će biti udaljen sa terena i biti sankcionisan u skladu sa ugovorom koji je igrač potpisao pre takmičenja",
];

/** Cene kao kartice: tri broja koja igrač pamti, pa dve napomene ispod. */
const PRICES: { label: string; amount: string; unit: string; note: string; tone: "light" | "dark" }[] = [
  { label: "Kotizacija", amount: "7.500", unit: "dinara", note: "Jednom, za celu sezonu. Prijave do 1. jula imaju popust.", tone: "light" },
  {
    label: "Po meču",
    amount: "750",
    unit: "dinara",
    note: "Igraš dva meča dnevno, pa pri dolasku plaćaš 1.500 dinara.",
    tone: "light",
  },
  { label: "Od Top 16", amount: "0", unit: "dinara", note: "Kad uđeš u Top 16, ne plaćaš više nijedan meč do kraja.", tone: "dark" },
];

const PRICE_NOTES = [
  { icon: "card", text: "Biće omogućeno i plaćanje karticom." },
  {
    icon: "doc",
    text: "Ugovor koji igrači potpisuju pre turnira detaljnije propisuje novčane sankcije ako igrač ne izvrši svoje obaveze (npr. ne javi na vreme da ne dolazi na meč).",
  },
] as const;

const LIST =
  "list-decimal font-medium text-black marker:font-medium xl:text-[32px] xl:leading-[1.4] xl:tracking-[0.02em]";

export function Rules() {
  return (
    <section className="bg-white pb-16 pt-[53px] font-site xl:pb-[160px] xl:pt-[160px]">
      <div className={CONTAINER}>
        <h2
          id="pravilnik-i-cene"
          className={`${ANCHOR_TARGET} text-center text-2xl font-extrabold leading-[1.2] xl:text-[42px]`}
        >
          PRAVILNIK LIGE
        </h2>
        <ol className={`${LIST} mt-16 pl-[60px] pr-[30px] text-base leading-[1.3] tracking-[0.09em] xl:mt-[112px] xl:pl-20 xl:pr-0`}>
          {RULES.map((r) => (
            <li key={r} className="pl-2 xl:pl-6">
              {r}
            </li>
          ))}
        </ol>

        <h2 className="mt-[90px] text-center text-[34px] font-bold leading-[1.2] xl:mt-[125px] xl:text-[42px]">CENE</h2>
        <div className="mt-12 grid gap-4 md:grid-cols-3 xl:mt-[72px] xl:gap-6">
          {PRICES.map((p, i) => (
            <InView key={p.label} delay={i * 0.08} className="h-full">
              <PriceCard {...p} />
            </InView>
          ))}
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:mt-6 xl:gap-6">
          {PRICE_NOTES.map((n, i) => (
            <InView key={n.icon} delay={0.24 + i * 0.08} className="h-full">
              <p className="flex h-full gap-4 rounded-2xl border border-black/10 p-5 text-[15px] leading-[1.5] text-black/70 xl:p-6 xl:text-base">
                <NoteIcon kind={n.icon} />
                <span>{n.text}</span>
              </p>
            </InView>
          ))}
        </div>
      </div>
    </section>
  );
}

function PriceCard({ label, amount, unit, note, tone }: (typeof PRICES)[number]) {
  const dark = tone === "dark";
  return (
    <div
      className={`relative flex h-full flex-col overflow-hidden rounded-3xl p-6 xl:p-8 ${
        dark ? "bg-black text-white" : "bg-[#f4f4f5] text-black"
      }`}
    >
      {dark && (
        <div aria-hidden className="pointer-events-none absolute -right-16 -top-16 size-48 rounded-full bg-prize-gold/25 blur-3xl" />
      )}
      <p className={`relative text-sm font-bold uppercase tracking-[0.08em] ${dark ? "text-prize-gold" : "text-black/50"}`}>{label}</p>
      <p className="relative mt-4 flex items-baseline gap-2">
        <span className="text-[52px] font-extrabold leading-none tracking-[-0.04em] tabular-nums xl:text-[64px]">{amount}</span>
        <span className={`text-lg font-semibold ${dark ? "text-white/60" : "text-black/50"}`}>{unit}</span>
      </p>
      <p className={`relative mt-5 text-base leading-[1.5] ${dark ? "text-white/75" : "text-black/65"}`}>{note}</p>
    </div>
  );
}

function NoteIcon({ kind }: { kind: "card" | "doc" }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden className="mt-0.5 size-5 shrink-0 text-black" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      {kind === "card" ? (
        <>
          <rect x="2.5" y="5" width="19" height="14" rx="2.5" />
          <path d="M2.5 10h19M6.5 15h4" />
        </>
      ) : (
        <>
          <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
          <path d="M14 3v5h5M9 13h6M9 17h4" />
        </>
      )}
    </svg>
  );
}
