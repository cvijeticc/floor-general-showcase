"use client";

import { ChevronLeft, ChevronRight, Trophy } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { PlayerAvatar } from "@/components/site/results/player-avatar";
import {
  CARD_FRACTION,
  bracketHeight,
  cardMiddle,
  cardTop,
  champion,
  feederLabel,
  mainSeries,
  playerStops,
  roundFormat,
  seriesCode,
  seriesOfPlayer,
  shortWhen,
  thirdPlaceTop,
  winnerSide,
  type PlayerStop,
} from "@/lib/bracket";
import { playerThumb } from "@/lib/player-photos";
import type { PublicBracket, PublicPlayer, PublicRound, PublicSeries } from "@/lib/site-api";
import { BracketSearch } from "./bracket-search";
import { SeriesDialog } from "./series-dialog";

/** Visina zaglavlja kolone (naziv kola i format) iznad kartica. */
const HEADER = 52;

/**
 * Drvo playoff-a po uzoru na SofaScore (UC-PLA-01). Gleda se od izabranog kola: ono je zbijeno,
 * a svako sledeće kolo stoji na sredini između svojih parova. Kola se biraju čipovima, strelicama
 * ili prevlačenjem prstom; promena kola je animirana (kolone klize, razmaci se skupljaju).
 *
 * Mere su CSS promenljive (--bracket-col, --bracket-gap, --bracket-unit), pa raspored radi i pre
 * hidratacije: na telefonu je jedna kolona preko celog ekrana i viri sledeća, na računaru četiri.
 * --bracket-pad je vazduh oko kolona: drvo ima overflow-hidden, a okvir istaknute kartice (ring) i
 * puls posle pretrage crtaju se izvan kartice, pa bi bez njega prvoj koloni odsekao levu ivicu.
 * Prelaz mišem preko igrača ističe ceo njegov put kroz drvo.
 *
 * Pretraga („Pronađi igrača u drvetu") otvara drvo od kola u kome je igrač poslednji put igrao,
 * ističe mu put i spušta stranu do te kartice — ko je stigao do polufinala vodi se na polufinale.
 */
export function BracketViewer({ bracket, initialRound }: { bracket: PublicBracket; initialRound: number }) {
  const rounds = bracket.rounds;
  const last = rounds.length - 1;
  const [start, setStart] = useState(Math.min(Math.max(0, initialRound), last));
  const [hover, setHover] = useState<number | null>(null);
  const [picked, setPicked] = useState<PlayerStop | null>(null);
  // Pređeni miš ima prednost, a kad ga nema, ostaje istaknut igrač iz pretrage.
  const focus = hover ?? picked?.player.id ?? null;
  const stops = useMemo(() => playerStops(bracket), [bracket]);
  const [found, setFound] = useState<{ seriesId: number; key: number } | null>(null);
  const [open, setOpen] = useState<{ series: PublicSeries; round: PublicRound; roundIndex: number } | null>(null);

  const focusSeries = useMemo(() => (focus == null ? new Set<number>() : seriesOfPlayer(bracket, focus)), [bracket, focus]);
  const height = bracketHeight(rounds, start);
  const winner = champion(bracket);

  // Prevlačenje levo/desno menja kolo; posle prevlačenja klik na karticu se ne računa.
  const pointer = useRef<{ x: number; y: number } | null>(null);
  const swiped = useRef(false);
  const go = (to: number) => setStart(Math.min(Math.max(0, to), last));

  const barRef = useRef<HTMLDivElement>(null);
  const treeRef = useRef<HTMLDivElement>(null);
  const roundFromSearch = useRef(false);
  const firstRender = useRef(true);

  // Posle skoka na igrača ispod miša se nađe neka druga kartica i „hover" bi istakao pogrešnog
  // igrača. Zato se prelaz mišem ne računa dok se miš stvarno ne pomeri.
  const hoverLocked = useRef(false);
  const onHover = (id: number | null) => {
    if (hoverLocked.current && id != null) return;
    setHover(id);
  };

  function pick(stop: PlayerStop | null) {
    setPicked(stop);
    if (!stop) return;
    setHover(null);
    hoverLocked.current = true;
    window.addEventListener("pointermove", () => void (hoverLocked.current = false), { once: true });
    // Pretraga sama spušta stranu do igrača (efekat ispod), pa promena kola ne vraća na vrh drveta.
    if (stop.roundIndex !== start) roundFromSearch.current = true;
    setStart(stop.roundIndex);
    setFound({ seriesId: stop.series.id, key: Date.now() });
  }

  // Promena kola (čip, strelica, prevlačenje): kasnije kolo je mnogo kraće od prvog, pa bi strana
  // spuštena niz Top 64 ostala ispod drveta — na dnu strane, a polufinale i finale iznad ekrana.
  // Zato se strana vraća na vrh drveta, odmah ispod zalepljene trake, i to samo kad je vrh drveta
  // već iznad nje. Traka sa kolima centrira izabrano kolo, da se vide i susedna.
  useEffect(() => {
    const chip = barRef.current?.querySelector<HTMLElement>(`nav [data-round-index="${start}"]`);
    const nav = chip?.parentElement;
    if (chip && nav) {
      const chipBox = chip.getBoundingClientRect();
      const left = nav.scrollLeft + chipBox.left - nav.getBoundingClientRect().left - (nav.clientWidth - chipBox.width) / 2;
      nav.scrollTo({ left: Math.max(0, left), behavior: firstRender.current ? "auto" : "smooth" });
    }
    if (firstRender.current || roundFromSearch.current) {
      firstRender.current = false;
      roundFromSearch.current = false;
      return;
    }
    const tree = treeRef.current;
    const bar = barRef.current;
    if (!tree || !bar) return;
    const covered = (parseFloat(getComputedStyle(bar).top) || 0) + bar.offsetHeight;
    const treeTop = tree.getBoundingClientRect().top;
    if (treeTop < covered) {
      const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      window.scrollTo({ top: window.scrollY + treeTop - covered - 8, behavior: reduce ? "auto" : "smooth" });
    }
  }, [start]);

  // Posle izbora iz pretrage: strana se spušta tako da igračeva poslednja serija stoji na sredini
  // vidljivog dela ekrana (ispod menija i trake sa kolima). Položaj se računa iz rasporeda, a ne
  // meri sa kartice, jer kartica još klizi na novo mesto (450 ms).
  useEffect(() => {
    if (!found || !picked) return;
    const tree = treeRef.current;
    if (!tree) return;
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const scroll = (behavior: ScrollBehavior) => {
      const unit = parseFloat(getComputedStyle(tree).getPropertyValue("--bracket-unit")) || 80;
      const round = rounds[picked.roundIndex];
      const index = picked.series.thirdPlace
        ? -1
        : mainSeries(round).findIndex((s) => s.id === picked.series.id);
      const top = index < 0 ? thirdPlaceTop(last - picked.roundIndex) : cardTop(0, index);
      const cardCenter = tree.getBoundingClientRect().top + window.scrollY + HEADER + unit * (top + CARD_FRACTION / 2);
      // Koliko vrh ekrana zaklanjaju meni i traka sa kolima kad se zalepe (ne gde su sada).
      const bar = barRef.current;
      const covered = bar ? (parseFloat(getComputedStyle(bar).top) || 0) + bar.offsetHeight : 0;
      const target = cardCenter - (covered + window.innerHeight) / 2;
      const clamped = Math.min(Math.max(0, target), document.documentElement.scrollHeight - window.innerHeight);
      if (Math.abs(window.scrollY - clamped) > 4) window.scrollTo({ top: clamped, behavior });
      return Math.abs(clamped - target) > 4;
    };
    // Kad se drvo produžava (vraćanje na prvo kolo), strana je još kratka dok traje animacija
    // visine — pa se položaj proveri još jednom kad se ona završi.
    const clipped = scroll(reduce ? "auto" : "smooth");
    const again = clipped ? window.setTimeout(() => scroll(reduce ? "auto" : "smooth"), 470) : undefined;
    return () => window.clearTimeout(again);
  }, [found, picked, rounds, last]);

  return (
    <div>
      {/* Izbor kola — lepi se ispod menija dok se skroluje niz dugo prvo kolo. */}
      <div ref={barRef} className="sticky top-[53px] z-20 -mx-5 border-b border-black/10 bg-[#f4f4f5]/95 px-5 py-3 backdrop-blur md:mx-0 md:rounded-2xl md:border md:px-3 xl:top-[93px]">
        <div className="flex flex-col gap-2.5 md:flex-row md:items-center md:gap-3">
          <BracketSearch stops={stops} rounds={rounds} selected={picked} onSelect={pick} />
          <div className="flex min-w-0 flex-1 items-center gap-2">
          <button
            type="button"
            onClick={() => go(start - 1)}
            disabled={start === 0}
            aria-label="Prethodno kolo"
            className="flex size-9 shrink-0 items-center justify-center rounded-full bg-white text-black shadow-sm transition-opacity disabled:opacity-30"
          >
            <ChevronLeft className="size-5" />
          </button>
          <nav aria-label="Kola playoff-a" className="flex min-w-0 flex-1 gap-2 overflow-x-auto [scrollbar-width:none]">
            {rounds.map((round, index) => (
              <button
                key={round.roundNo}
                type="button"
                data-round-index={index}
                onClick={() => go(index)}
                aria-current={index === start ? "true" : undefined}
                className={`shrink-0 whitespace-nowrap rounded-full px-4 py-2 text-sm font-bold transition-colors duration-200 ${
                  index === start ? "bg-black text-white" : "bg-white text-black hover:bg-black/5"
                }`}
              >
                {round.name}
              </button>
            ))}
          </nav>
          <button
            type="button"
            onClick={() => go(start + 1)}
            disabled={start === last}
            aria-label="Sledeće kolo"
            className="flex size-9 shrink-0 items-center justify-center rounded-full bg-white text-black shadow-sm transition-opacity disabled:opacity-30"
          >
            <ChevronRight className="size-5" />
          </button>
          </div>
        </div>
      </div>

      <div
        ref={treeRef}
        className="relative -mx-[var(--bracket-pad)] mt-4 touch-pan-y overflow-hidden [--bracket-pad:10px] [--bracket-col:calc(100%_-_60px_-_2*var(--bracket-pad))] [--bracket-gap:22px] [--bracket-unit:77px] md:[--bracket-col:236px] md:[--bracket-gap:40px] md:[--bracket-unit:84px]"
        style={{
          height: `calc(${HEADER}px + var(--bracket-unit) * ${height})`,
          transition: "height 450ms var(--ease-out-strong)",
        }}
        onPointerDown={(event) => {
          pointer.current = { x: event.clientX, y: event.clientY };
          swiped.current = false;
        }}
        onPointerUp={(event) => {
          const from = pointer.current;
          pointer.current = null;
          if (!from) return;
          const dx = event.clientX - from.x;
          const dy = event.clientY - from.y;
          if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) {
            swiped.current = true;
            go(dx < 0 ? start + 1 : start - 1);
          }
        }}
        onClickCapture={(event) => {
          if (swiped.current) {
            event.preventDefault();
            event.stopPropagation();
            swiped.current = false;
          }
        }}
      >
        {rounds.map((round, index) => {
          const column = index - start;
          const hidden = column < 0;
          const series = mainSeries(round);
          const third = round.series.find((s) => s.thirdPlace);
          return (
            <section
              key={round.roundNo}
              aria-label={round.name}
              aria-hidden={hidden || undefined}
              className="absolute top-0"
              style={{
                left: `calc(var(--bracket-pad) + ${column} * (var(--bracket-col) + var(--bracket-gap)))`,
                width: "var(--bracket-col)",
                height: "100%",
                opacity: hidden ? 0 : 1,
                pointerEvents: hidden ? "none" : undefined,
                transition: "left 450ms var(--ease-out-strong), opacity 300ms ease-out",
              }}
            >
              <header className="flex h-[52px] flex-col justify-center">
                <h2 className="text-base font-extrabold uppercase tracking-wide">{round.name}</h2>
                <p className="text-xs text-black/50">{roundFormat(round)}</p>
              </header>

              {column > 0 && (
                <Connectors
                  feeders={mainSeries(rounds[index - 1])}
                  targets={series}
                  column={column}
                  focus={focus}
                />
              )}

              {series.map((s, position) => (
                <SeriesCard
                  key={s.id}
                  series={s}
                  round={round}
                  rounds={rounds}
                  roundIndex={index}
                  top={cardTop(Math.max(0, column), position)}
                  focus={focus}
                  inFocus={focusSeries.has(s.id)}
                  found={found?.seriesId === s.id ? found.key : null}
                  onFocus={onHover}
                  onOpen={() => setOpen({ series: s, round, roundIndex: index })}
                />
              ))}

              {third && (
                <>
                  <p
                    className="absolute inset-x-0 text-xs font-bold uppercase tracking-wide text-black/50"
                    style={{
                      top: `calc(${HEADER - 20}px + var(--bracket-unit) * ${thirdPlaceTop(Math.max(0, column))})`,
                      transition: "top 450ms var(--ease-out-strong)",
                    }}
                  >
                    Za 3. mesto
                  </p>
                  <SeriesCard
                    series={third}
                    round={round}
                    rounds={rounds}
                    roundIndex={index}
                    top={thirdPlaceTop(Math.max(0, column))}
                    focus={focus}
                    inFocus={focusSeries.has(third.id)}
                    found={found?.seriesId === third.id ? found.key : null}
                    onFocus={onHover}
                    onOpen={() => setOpen({ series: third, round, roundIndex: index })}
                  />
                </>
              )}
            </section>
          );
        })}

        {/* Šampion — kao na kosturu lige, desno od finala. */}
        <section
          aria-label="Šampion"
          className="absolute top-0"
          style={{
            left: `calc(var(--bracket-pad) + ${last + 1 - start} * (var(--bracket-col) + var(--bracket-gap)))`,
            width: "var(--bracket-col)",
            height: "100%",
            transition: "left 450ms var(--ease-out-strong)",
          }}
        >
          <header className="flex h-[52px] flex-col justify-center">
            <h2 className="text-base font-extrabold uppercase tracking-wide text-playoff">Šampion</h2>
            <p className="text-xs text-black/50">500.000 dinara</p>
          </header>
          <div
            className="absolute inset-x-0 flex items-center gap-3 rounded-xl border-2 border-playoff/60 bg-gradient-to-br from-white to-orange-50 px-3"
            style={{
              top: `calc(${HEADER}px + var(--bracket-unit) * ${cardTop(last - start, 0)})`,
              height: `calc(var(--bracket-unit) * ${CARD_FRACTION})`,
              transition: "top 450ms var(--ease-out-strong)",
            }}
          >
            <Trophy className="size-7 shrink-0 text-playoff" aria-hidden />
            {winner ? (
              <span className="min-w-0">
                <span className="block truncate text-lg font-extrabold">{winner.nickname}</span>
                <span className="block truncate text-xs text-black/55">{winner.name}</span>
              </span>
            ) : (
              <span className="text-sm font-semibold text-black/45">Ko će podići pehar?</span>
            )}
          </div>
        </section>

        {/* Senka na desnoj ivici: drvo se nastavlja. */}
        {start < last && (
          <div
            aria-hidden
            className="pointer-events-none absolute inset-y-0 right-0 w-10 bg-gradient-to-l from-[#f4f4f5] to-transparent md:w-16"
          />
        )}
      </div>

      <p className="mt-3 text-center text-xs text-black/45 md:hidden">
        Prevuci levo ili desno za sledeće kolo · dodirni seriju za mečeve
      </p>

      <SeriesDialog
        open={open}
        rounds={rounds}
        onClose={() => setOpen(null)}
      />
    </div>
  );
}

/**
 * Linije između dva kola: od sredine svake serije do sredine serije u koju vodi („]" oblik).
 * Obični elementi sa animiranim top/height — prate kartice kad se menja kolo.
 */
function Connectors({
  feeders,
  targets,
  column,
  focus,
}: {
  feeders: PublicSeries[];
  targets: PublicSeries[];
  column: number;
  focus: number | null;
}) {
  const at = (units: number) => `calc(${HEADER}px + var(--bracket-unit) * ${units})`;
  const span = (units: number) => `calc(var(--bracket-unit) * ${Math.max(0, units)})`;
  const motion = "top 450ms var(--ease-out-strong), height 450ms var(--ease-out-strong)";
  return (
    <div aria-hidden className="pointer-events-none absolute inset-y-0" style={{ left: "calc(-1 * var(--bracket-gap))", width: "var(--bracket-gap)" }}>
      {targets.map((target, j) => {
        const yT = cardMiddle(column, j);
        let lit = false;
        const halves = [0, 1].map((k) => {
          const feeder = feeders[2 * j + k];
          if (!feeder) return null;
          const yF = cardMiddle(column - 1, 2 * j + k);
          const highlighted = focus != null && feeder.winnerId === focus;
          lit = lit || highlighted;
          const color = highlighted ? "border-playoff" : feeder.winnerId != null ? "border-black/40" : "border-black/15";
          const width = highlighted ? "2px" : "1px";
          return (
            <span
              key={k}
              className={`absolute left-0 ${color} ${k === 0 ? "rounded-tr-md" : "rounded-br-md"}`}
              style={{
                top: at(k === 0 ? yF : yT),
                height: span(k === 0 ? yT - yF : yF - yT),
                width: "calc(var(--bracket-gap) / 2)",
                borderStyle: "solid",
                borderWidth: k === 0 ? `${width} ${width} 0 0` : `0 ${width} ${width} 0`,
                transition: motion,
              }}
            />
          );
        });
        return (
          <span key={target.id}>
            {halves}
            <span
              className={`absolute ${lit ? "border-playoff" : "border-black/25"}`}
              style={{
                top: at(yT),
                left: "calc(var(--bracket-gap) / 2)",
                width: "calc(var(--bracket-gap) / 2)",
                borderTopStyle: "solid",
                borderTopWidth: lit ? "2px" : "1px",
                transition: motion,
              }}
            />
          </span>
        );
      })}
    </div>
  );
}

function SeriesCard({
  series,
  round,
  rounds,
  roundIndex,
  top,
  focus,
  inFocus,
  found,
  onFocus,
  onOpen,
}: {
  series: PublicSeries;
  round: PublicRound;
  rounds: PublicRound[];
  roundIndex: number;
  top: number;
  focus: number | null;
  inFocus: boolean;
  /** Ključ poslednjeg izbora iz pretrage koji vodi na ovu karticu — svaki nov izbor ponovo „trepne". */
  found: number | null;
  onFocus: (playerId: number | null) => void;
  onOpen: () => void;
}) {
  const side = winnerSide(series);
  const meta = series.walkover
    ? "Službeno"
    : series.status === "FINISHED"
      ? "Kraj"
      : series.status === "IN_PROGRESS"
        ? "U toku"
        : shortWhen(series.scheduledStart, series.courtName);

  return (
    <button
      type="button"
      onClick={onOpen}
      aria-label={`${seriesCode(series)}: ${series.player1?.nickname ?? "?"} protiv ${series.player2?.nickname ?? "?"}`}
      className={`absolute inset-x-0 flex flex-col justify-center gap-px rounded-xl border bg-white px-2.5 text-left shadow-[0_1px_2px_rgba(0,0,0,0.05)] outline-none transition-[border-color,box-shadow] duration-200 hover:border-black/30 focus-visible:ring-2 focus-visible:ring-playoff md:px-3 ${
        inFocus ? "border-playoff ring-1 ring-playoff" : "border-black/[0.09]"
      }`}
      style={{
        top: `calc(${HEADER}px + var(--bracket-unit) * ${top})`,
        height: `calc(var(--bracket-unit) * ${CARD_FRACTION})`,
        transition: "top 450ms var(--ease-out-strong), border-color 200ms, box-shadow 200ms",
      }}
    >
      {/* Kartica do koje je dovela pretraga kratko zasvetli; nov izbor (nov ključ) ponovo pokreće animaciju. */}
      {found != null && <span key={found} aria-hidden className="bracket-found pointer-events-none absolute inset-0 rounded-xl" />}
      <span className="flex items-center justify-between text-[10px] font-bold uppercase leading-3 tracking-wide text-black/40">
        <span>{seriesCode(series)}</span>
        <span className={series.status === "IN_PROGRESS" ? "text-playoff" : ""}>{meta}</span>
      </span>
      <PlayerRow
        series={series}
        slot={1}
        player={series.player1}
        seed={roundIndex === 0 ? series.seed1 : null}
        placeholder={feederLabel(rounds, roundIndex, series, 1)}
        winner={side === 1}
        loser={side === 2}
        single={round.winsRequired === 1}
        focus={focus}
        onFocus={onFocus}
      />
      <PlayerRow
        series={series}
        slot={2}
        player={series.player2}
        seed={roundIndex === 0 ? series.seed2 : null}
        placeholder={feederLabel(rounds, roundIndex, series, 2)}
        winner={side === 2}
        loser={side === 1}
        single={round.winsRequired === 1}
        focus={focus}
        onFocus={onFocus}
      />
    </button>
  );
}

function PlayerRow({
  series,
  slot,
  player,
  seed,
  placeholder,
  winner,
  loser,
  single,
  focus,
  onFocus,
}: {
  series: PublicSeries;
  slot: 1 | 2;
  player: PublicPlayer | null;
  seed: string | null;
  placeholder: string;
  winner: boolean;
  loser: boolean;
  single: boolean;
  focus: number | null;
  onFocus: (playerId: number | null) => void;
}) {
  if (!player) {
    return (
      <span className="flex h-[22px] items-center gap-2 md:h-6">
        <span className="size-[18px] shrink-0 rounded-full border border-dashed border-black/20 md:size-5" />
        <span className="truncate text-sm text-black/35">{placeholder}</span>
      </span>
    );
  }

  const wins = slot === 1 ? series.wins1 : series.wins2;
  const focused = focus === player.id;
  return (
    <span
      className="flex h-[22px] items-center gap-2 md:h-6"
      onMouseEnter={() => onFocus(player.id)}
      onMouseLeave={() => onFocus(null)}
    >
      <PlayerAvatar name={player.name} src={playerThumb(player)} size={20} className="size-[18px] md:size-5" />
      <span
        className={`min-w-0 truncate text-sm ${winner ? "font-extrabold" : "font-semibold"} ${
          focused ? "text-playoff" : loser ? "text-black/45" : ""
        }`}
      >
        {player.nickname}
      </span>
      {seed && <span className="hidden shrink-0 text-[10px] font-semibold text-black/35 min-[400px]:inline">{seed}</span>}
      {series.walkover && winner && (
        <span className="shrink-0 rounded bg-black/[0.06] px-1 text-[10px] font-bold text-black/55">w.o.</span>
      )}
      <span className="ml-auto flex shrink-0 items-center gap-1.5 tabular-nums">
        {!single &&
          series.games.map((game) => {
            const score = slot === 1 ? game.score1 : game.score2;
            const other = slot === 1 ? game.score2 : game.score1;
            return (
              <span
                key={game.matchId}
                className={`w-4 text-right text-[11px] ${score > other ? "font-bold text-black/80" : "text-black/35"}`}
              >
                {score}
              </span>
            );
          })}
        <span className={`w-4 text-right text-sm ${winner ? "font-extrabold text-black" : "font-bold text-black/55"}`}>
          {single ? (series.games[0] ? (slot === 1 ? series.games[0].score1 : series.games[0].score2) : "") : series.games.length ? wins : ""}
        </span>
      </span>
    </span>
  );
}
