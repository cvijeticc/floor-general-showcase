import type { PublicFormItem } from "@/lib/site-api";
import { roundLabel } from "@/lib/site-api";

const SIZES = {
  sm: "size-5 rounded text-[11px]",
  md: "size-7 rounded-md text-[13px]",
} as const;

/**
 * Forma (UC-PUB-06): P = pobeda, I = poraz, od starijeg ka novijem meču — kao na SofaScore-u.
 * Boja nikad nije sama (WCAG): uvek i slovo, a opis meča je u `title` (miš) i u tekstu za čitač ekrana.
 * Tamnije nijanse (700) da belo slovo ima dovoljan kontrast i na malom kvadratu.
 */
export function FormChips({
  items,
  size = "sm",
  className = "",
}: {
  items: PublicFormItem[];
  size?: keyof typeof SIZES;
  className?: string;
}) {
  if (items.length === 0) return <span className="text-black/40">—</span>;

  return (
    <ol aria-label="Forma, od starijeg ka novijem meču" className={`flex items-center gap-1 ${className}`}>
      {items.map((item) => (
        <li
          key={item.matchId}
          title={describeForm(item)}
          className={`inline-flex shrink-0 items-center justify-center font-bold text-white ${SIZES[size]} ${
            item.won ? "bg-emerald-700" : "bg-red-700"
          }`}
        >
          <span aria-hidden>{item.won ? "P" : "I"}</span>
          <span className="sr-only">{describeForm(item)}</span>
        </li>
      ))}
    </ol>
  );
}

/** „Pobeda 11:8 protiv Zeka, 11. kolo" — rezultat uvek iz ugla igrača čija je forma. */
export function describeForm(item: PublicFormItem): string {
  const note =
    item.status === "WALKOVER"
      ? ", službeni rezultat"
      : item.status === "FORFEIT"
        ? ", predaja"
        : item.status === "DOUBLE_FORFEIT"
          ? ", obojica kažnjena"
          : "";
  return `${item.won ? "Pobeda" : "Poraz"} ${item.scored}:${item.conceded} protiv ${item.opponent.nickname}, ${roundLabel(item)}${note}`;
}
