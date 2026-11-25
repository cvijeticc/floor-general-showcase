import { describeForm } from "@/components/site/results/form-chips";
import type { PublicFormItem } from "@/lib/site-api";
import { roundLabel } from "@/lib/site-api";
import { plural, signed } from "@/lib/stat-format";

const PLOT_HEIGHT = "h-[168px]";

/**
 * „Forma — poslednjih 10" (UC-PUB-06): razlika u poenima po meču kao stubići oko nule — pobeda
 * gore (zeleno), poraz dole (crveno), od starijeg ka novijem. Boja nikad nije sama: smer stubića
 * i slovo P/I ispod nose isto. Detalj meča je u oblačiću (miš i tastatura), a sve vrednosti
 * su i u spisku „Mečevi" ispod — oblačić ništa ne krije.
 */
export function FormChart({ items }: { items: PublicFormItem[] }) {
  if (items.length === 0) return null;

  const largest = Math.max(...items.map((item) => Math.abs(item.scored - item.conceded)));
  // Okrugla, simetrična skala: 12:0 i 2:1 na istom grafiku, oba čitljiva.
  const scale = Math.max(2, Math.ceil(largest / 2) * 2);
  const columns = { gridTemplateColumns: `repeat(${items.length}, minmax(0, 1fr))` };

  return (
    <section aria-labelledby="forma">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <h2 id="forma" className="text-2xl font-bold">
          Forma
        </h2>
        <p className="text-sm text-black/50">
          poslednjih {items.length} {plural(items.length, "meč", "meča", "mečeva")} · razlika u poenima
        </p>
      </div>

      <div className="mt-4 rounded-2xl border border-black/10 px-3 pb-3 pt-5 sm:px-5">
        <div className="flex">
          <div
            aria-hidden
            className={`flex w-8 shrink-0 flex-col justify-between pr-2 text-right text-xs tabular-nums text-black/45 ${PLOT_HEIGHT}`}
          >
            <span className="-translate-y-1/2">{signed(scale)}</span>
            <span>0</span>
            <span className="translate-y-1/2">{signed(-scale)}</span>
          </div>

          <div className={`relative min-w-0 flex-1 ${PLOT_HEIGHT}`}>
            <div aria-hidden className="absolute inset-x-0 top-0 border-t border-black/[0.07]" />
            <div aria-hidden className="absolute inset-x-0 top-1/2 border-t border-black/30" />
            <div aria-hidden className="absolute inset-x-0 bottom-0 border-t border-black/[0.07]" />

            <ol aria-label="Razlika u poenima po meču, od starijeg ka novijem" className="relative grid h-full" style={columns}>
              {items.map((item, i) => {
                const margin = item.scored - item.conceded;
                const height = `${(Math.abs(margin) / scale) * 50}%`;
                const edge = i < 2 ? "left-0" : i > items.length - 3 ? "right-0" : "left-1/2 -translate-x-1/2";
                return (
                  <li
                    key={item.matchId}
                    tabIndex={0}
                    aria-label={describeForm(item)}
                    className="group relative flex justify-center outline-none"
                  >
                    {/* Stubić ≤ 24px, zaobljen na kraju podatka, ravan na nuli. */}
                    <span
                      aria-hidden
                      className={`absolute w-[min(24px,60%)] transition-opacity group-hover:opacity-80 group-focus-visible:outline-2 group-focus-visible:outline-offset-2 group-focus-visible:outline-black ${
                        item.won ? "bottom-1/2 rounded-t bg-emerald-700" : "top-1/2 rounded-b bg-red-700"
                      }`}
                      style={{ height }}
                    />
                    <span
                      role="tooltip"
                      className={`pointer-events-none absolute -top-2 z-10 hidden -translate-y-full whitespace-nowrap rounded-lg bg-black px-2.5 py-1.5 text-xs text-white shadow-lg group-hover:block group-focus-visible:block ${edge}`}
                    >
                      <span className="text-sm font-bold">
                        {item.scored}:{item.conceded}
                      </span>{" "}
                      {item.won ? "pobeda" : "poraz"} · {item.opponent.nickname} · {roundLabel(item)}
                    </span>
                  </li>
                );
              })}
            </ol>
          </div>
        </div>

        <div aria-hidden className="ml-8 mt-2 grid" style={columns}>
          {items.map((item) => (
            <div key={item.matchId} className="flex flex-col items-center gap-1">
              <span
                className={`inline-flex size-5 items-center justify-center rounded text-[11px] font-bold text-white ${
                  item.won ? "bg-emerald-700" : "bg-red-700"
                }`}
              >
                {item.won ? "P" : "I"}
              </span>
              <span className="text-[11px] tabular-nums text-black/45">{item.roundName ? item.roundName.replace("Top ", "T") : `${item.roundNumber}.`}</span>
            </div>
          ))}
        </div>
        <p aria-hidden className="ml-8 mt-1 text-[11px] text-black/40">
          kolo · starije → novije
        </p>
      </div>
    </section>
  );
}
