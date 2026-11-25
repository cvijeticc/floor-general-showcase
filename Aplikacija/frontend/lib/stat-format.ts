/** Brojke javnog sajta onako kako se čitaju na sportskim stranama: +21, −4, 75%, 10,3. */

/** +21 / 0 / −4 — plus/minus (koš-razlika); pravi znak minus, ne crtica. */
export function signed(value: number): string {
  if (value > 0) return `+${value}`;
  if (value < 0) return `−${Math.abs(value)}`;
  return "0";
}

const DECIMAL = new Intl.NumberFormat("sr-Latn-RS", { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/** 10.25 → „10,3". */
export function decimal(value: number): string {
  return DECIMAL.format(value);
}

/** 3 od 4 → „75%"; bez mečeva „—". */
export function percent(part: number, whole: number): string {
  return whole > 0 ? `${Math.round((part / whole) * 100)}%` : "—";
}

/** Srpska množina: 1 meč, 2 meča, 5 mečeva (21 meč, 12 mečeva). */
export function plural(n: number, one: string, few: string, many: string): string {
  const mod10 = Math.abs(n) % 10;
  const mod100 = Math.abs(n) % 100;
  if (mod10 === 1 && mod100 !== 11) return one;
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
  return many;
}

/** Niz iz ugla igrača: +3 → „3 pobede", −2 → „2 poraza", 0 → „—". */
export function streakLabel(streak: number): string {
  if (streak === 0) return "—";
  const n = Math.abs(streak);
  return streak > 0 ? `${n} ${plural(n, "pobeda", "pobede", "pobeda")}` : `${n} ${plural(n, "poraz", "poraza", "poraza")}`;
}

/** Boja teksta za plus/minus: zeleno gore, crveno dole — uvek uz znak, nikad sama. */
export function differenceColor(value: number): string {
  if (value > 0) return "text-emerald-700";
  if (value < 0) return "text-red-700";
  return "";
}
