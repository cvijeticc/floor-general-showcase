// Samo tipovi — modul ostaje bez zavisnosti, pa ga `npm test` (Node bez bundler-a) učitava direktno.
import type { PublicBracket, PublicPlayer, PublicRound, PublicSeries } from "@/lib/site-api";

/**
 * Raspored drveta playoff-a na ekranu (po uzoru na SofaScore): kolo od kog se gleda je zbijeno,
 * a svaka serija sledećeg kola stoji tačno na sredini između svoje dve serije iz prethodnog.
 * Kad se pređe na sledeće kolo, ono postaje zbijeno — drvo se ne razvlači u visinu.
 *
 * Mere su u „jedinicama" (visina kartice + razmak), pa isti brojevi važe i na telefonu i na
 * računaru — samo jedinica u CSS-u ima drugu veličinu (--bracket-unit).
 */

/** Razmak između kartica kao deo jedinice: kartica = 6/7 jedinice, razmak = 1/7. */
export const CARD_FRACTION = 6 / 7;

/**
 * Vrh kartice u jedinicama: kolona {@code column} je udaljenost od kola od kog se gleda (0 = to
 * kolo), {@code index} je pozicija serije u kolu (0 = prva odozgo).
 */
export function cardTop(column: number, index: number): number {
  if (column <= 0) return index;
  const span = 2 ** column;
  return index * span + (span - 1) / 2;
}

/** Sredina kartice po visini, u jedinicama — tu se kače linije. */
export function cardMiddle(column: number, index: number): number {
  return cardTop(column, index) + CARD_FRACTION / 2;
}

/** Gde stoji meč za 3. mesto: ispod finala, sa malo vazduha. */
export function thirdPlaceTop(finalColumn: number): number {
  return cardTop(finalColumn, 0) + 1.5;
}

/** Visina drveta u jedinicama kad se gleda od kola {@code start}. */
export function bracketHeight(rounds: PublicRound[], start: number): number {
  const first = mainSeries(rounds[start]).length;
  const last = rounds.length - 1;
  const hasThird = rounds[last]?.series.some((s) => s.thirdPlace) ?? false;
  const finalColumn = last - start;
  const thirdBottom = hasThird ? thirdPlaceTop(finalColumn) + CARD_FRACTION + 0.35 : 0;
  return Math.max(first, thirdBottom, cardTop(finalColumn, 0) + 1);
}

/** Serije kola bez meča za 3. mesto (on se crta posebno, ispod finala). */
export function mainSeries(round: PublicRound | undefined): PublicSeries[] {
  return round ? round.series.filter((s) => !s.thirdPlace) : [];
}

/**
 * Kolo od kog se drvo otvara: prvo koje nije završeno (tu se trenutno igra), a kad je sve
 * odigrano — finale. Kao na SofaScore-u: posetilac odmah vidi tekuće kolo.
 */
export function currentRoundIndex(bracket: PublicBracket): number {
  const index = bracket.rounds.findIndex((round) =>
    round.series.some((s) => s.status !== "FINISHED" && !s.thirdPlace),
  );
  if (index < 0) return Math.max(0, bracket.rounds.length - 1);
  // Kolo u kome još niko nije stigao do svih parova: gledaj od prethodnog, da se vidi odakle dolaze.
  const round = bracket.rounds[index];
  const waiting = round.series.every((s) => s.status === "PENDING");
  return waiting && index > 0 ? index - 1 : index;
}

/** „M01", „M63" — kao na kosturu lige. */
export function seriesCode(series: Pick<PublicSeries, "number">): string {
  return `M${String(series.number).padStart(2, "0")}`;
}

/** Format kola rečima: „Na 2 pobede · do 11 ili 8 min" / „Jedna utakmica · do 16 ili 15 min". */
export function roundFormat(round: Pick<PublicRound, "winsRequired" | "targetScore" | "durationMinutes">): string {
  const series = round.winsRequired === 1 ? "Jedna utakmica" : `Na ${round.winsRequired} pobede`;
  return `${series} · do ${round.targetScore} ili ${round.durationMinutes} min`;
}

/** Ko je pobedio seriju: 1, 2 ili null dok traje. */
export function winnerSide(series: PublicSeries): 1 | 2 | null {
  if (series.winnerId == null) return null;
  if (series.player1?.id === series.winnerId) return 1;
  if (series.player2?.id === series.winnerId) return 2;
  return null;
}

/** Kod serije iz koje stiže igrač, za praznu karticu: „Pobednik M01" (kao na kosturu lige). */
export function feederLabel(rounds: PublicRound[], roundIndex: number, series: PublicSeries, slot: 1 | 2): string {
  if (roundIndex === 0) return "—";
  const previous = mainSeries(rounds[roundIndex - 1]);
  if (series.thirdPlace) {
    const semi = previous[slot - 1];
    return semi ? `Poraženi ${seriesCode(semi)}` : "—";
  }
  const feeder = previous[(series.position - 1) * 2 + (slot - 1)];
  return feeder ? `Pobednik ${seriesCode(feeder)}` : "—";
}

/** Sve serije u kojima igra igrač — „put do titule" za isticanje u drvetu. */
export function seriesOfPlayer(bracket: PublicBracket, playerId: number): Set<number> {
  const ids = new Set<number>();
  for (const round of bracket.rounds) {
    for (const s of round.series) {
      if (s.player1?.id === playerId || s.player2?.id === playerId) ids.add(s.id);
    }
  }
  return ids;
}

/** Šampion, kad je finale odigrano. */
export function champion(bracket: PublicBracket) {
  const final = mainSeries(bracket.rounds[bracket.rounds.length - 1])[0];
  if (!final) return null;
  const side = winnerSide(final);
  return side === 1 ? final.player1 : side === 2 ? final.player2 : null;
}

const SHORT_DAYS = ["Ned", "Pon", "Uto", "Sre", "Čet", "Pet", "Sub"];

/** „Sub 14.11. · 18:00 · Teren A" — kratko, za karticu u drvetu. */
export function shortWhen(iso: string | null, court: string | null): string {
  const parts: string[] = [];
  if (iso) {
    const [y, m, d] = iso.slice(0, 10).split("-").map(Number);
    const day = SHORT_DAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()];
    parts.push(`${day} ${String(d).padStart(2, "0")}.${String(m).padStart(2, "0")}.`);
    const time = iso.length >= 16 ? iso.slice(11, 16) : "";
    if (time && time !== "00:00") parts.push(time);
  }
  if (court) parts.push(court);
  return parts.join(" · ");
}

/** Gde je igrač stigao: poslednja serija u kojoj igra (najdalje kolo) i šta se u njoj desilo. */
export type PlayerStop = {
  player: PublicPlayer;
  roundIndex: number;
  series: PublicSeries;
  /** „playing" dok serija traje ili čeka, „out" kad je izgubio, „champion" kad je osvojio finale. */
  state: "playing" | "out" | "champion" | "third";
};

/**
 * Svi igrači iz drveta, svaki sa poslednjom serijom koju igra — za pretragu na /playoff: ko traži
 * igrača koji je stigao do polufinala vodi se na polufinale, a ne na njegov prvi meč.
 */
export function playerStops(bracket: PublicBracket): PlayerStop[] {
  const stops = new Map<number, PlayerStop>();
  const lastIndex = bracket.rounds.length - 1;
  bracket.rounds.forEach((round, roundIndex) => {
    for (const series of round.series) {
      for (const player of [series.player1, series.player2]) {
        if (!player) continue;
        const previous = stops.get(player.id);
        // Kasnije kolo uvek pobeđuje; u istom kolu (finale i meč za 3. mesto) važi finale.
        if (previous && previous.roundIndex === roundIndex && !series.thirdPlace) continue;
        stops.set(player.id, { player, roundIndex, series, state: stopState(series, player.id, roundIndex === lastIndex) });
      }
    }
  });
  return [...stops.values()];
}

function stopState(series: PublicSeries, playerId: number, finalRound: boolean): PlayerStop["state"] {
  if (series.winnerId == null) return "playing";
  if (series.winnerId !== playerId) return "out";
  if (!finalRound) return "playing";
  return series.thirdPlace ? "third" : "champion";
}
