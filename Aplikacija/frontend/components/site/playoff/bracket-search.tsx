"use client";

import { Search, X } from "lucide-react";
import { useEffect, useId, useMemo, useRef, useState } from "react";
import { PlayerAvatar } from "@/components/site/results/player-avatar";
import type { PlayerStop } from "@/lib/bracket";
import { playerThumb } from "@/lib/player-photos";
import { searchPlayers } from "@/lib/player-search";
import type { PublicRound } from "@/lib/site-api";

const MAX_SUGGESTIONS = 8;

/**
 * „Pronađi igrača u drvetu" na /playoff. Isto pretraživanje kao na /rezultati (bez dijakritika,
 * trpi jednu slovnu grešku), ali umesto profila vodi do igračeve poslednje serije: ko je stigao
 * do polufinala vodi se na polufinale. Taster „/" stavlja kursor u polje.
 * ARIA 1.2 combobox: fokus ostaje u polju, izabrana opcija ide kroz aria-activedescendant.
 */
export function BracketSearch({
  stops,
  rounds,
  selected,
  onSelect,
}: {
  stops: PlayerStop[];
  rounds: PublicRound[];
  selected: PlayerStop | null;
  onSelect: (stop: PlayerStop | null) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const listId = useId();
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);

  const trimmed = query.trim();
  const results = useMemo(
    () =>
      trimmed
        ? searchPlayers(trimmed, stops, (s) => ({ nickname: s.player.nickname, name: s.player.name }), MAX_SUGGESTIONS)
        : [],
    [trimmed, stops],
  );
  const expanded = open && trimmed.length > 0 && (!selected || selected.player.nickname !== query);

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      const target = event.target as HTMLElement | null;
      if (event.key !== "/" || event.metaKey || event.ctrlKey || event.altKey) return;
      if (target && (target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName))) return;
      event.preventDefault();
      inputRef.current?.focus();
      inputRef.current?.select();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  function pick(stop: PlayerStop) {
    setQuery(stop.player.nickname);
    setOpen(false);
    setActive(-1);
    onSelect(stop);
    // Na telefonu tastatura zaklanja pola ekrana — skloni je da se vidi kartica.
    inputRef.current?.blur();
  }

  function clear() {
    setQuery("");
    setActive(-1);
    onSelect(null);
    inputRef.current?.focus();
  }

  function onKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setOpen(true);
      setActive((i) => Math.min(i + 1, results.length - 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActive((i) => Math.max(i - 1, -1));
    } else if (event.key === "Enter") {
      event.preventDefault();
      const choice = results[active] ?? results[0];
      if (choice) pick(choice);
    } else if (event.key === "Escape") {
      if (expanded) setOpen(false);
      else if (query) clear();
    }
  }

  return (
    <div role="search" className="relative w-full md:w-[320px]">
      <label htmlFor={`${listId}-input`} className="sr-only">
        Pronađi igrača u drvetu po nadimku, imenu ili prezimenu
      </label>
      <Search aria-hidden className="pointer-events-none absolute left-3.5 top-1/2 size-[18px] -translate-y-1/2 text-black/45" />
      <input
        ref={inputRef}
        id={`${listId}-input`}
        type="search"
        autoComplete="off"
        spellCheck={false}
        enterKeyHint="search"
        placeholder="Pronađi igrača u drvetu"
        value={query}
        onChange={(event) => {
          setQuery(event.target.value);
          setActive(-1);
          setOpen(true);
          if (selected) onSelect(null);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onKeyDown={onKeyDown}
        role="combobox"
        aria-expanded={expanded}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={expanded && active >= 0 ? `${listId}-${active}` : undefined}
        className="h-11 w-full rounded-full bg-white pl-10 pr-10 text-base text-black shadow-sm outline-none ring-1 ring-black/[0.06] transition-shadow duration-200 placeholder:text-black/45 focus-visible:ring-2 focus-visible:ring-playoff [&::-webkit-search-cancel-button]:hidden"
      />
      {query ? (
        <button
          type="button"
          onClick={clear}
          aria-label="Obriši pretragu"
          className="absolute right-1.5 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-full text-black/55 transition-[background-color,transform] duration-150 ease-out hover:bg-black/5 active:scale-[0.94]"
        >
          <X className="size-4" />
        </button>
      ) : (
        <kbd
          aria-hidden
          className="pointer-events-none absolute right-3 top-1/2 hidden -translate-y-1/2 rounded border border-black/15 px-1.5 text-[11px] font-semibold text-black/40 md:block"
        >
          /
        </kbd>
      )}

      {expanded && (
        <ul
          id={listId}
          role="listbox"
          aria-label="Igrači u drvetu"
          // Klik na predlog ne sme da skine fokus sa polja pre nego što se izbor obradi.
          onMouseDown={(event) => event.preventDefault()}
          className="bracket-search-list absolute inset-x-0 top-full z-40 mt-2 max-h-[min(420px,60svh)] origin-top overflow-y-auto rounded-2xl bg-white p-1.5 text-black shadow-2xl ring-1 ring-black/10"
        >
          {results.length === 0 ? (
            <li className="px-3 py-3 text-sm text-black/60">
              Nema igrača „{trimmed}“ u drvetu. Probaj nadimak ili prezime.
            </li>
          ) : (
            results.map((stop, i) => (
              <li
                key={stop.player.id}
                id={`${listId}-${i}`}
                role="option"
                aria-selected={i === active}
                onMouseEnter={() => setActive(i)}
                onClick={() => pick(stop)}
                className={`flex cursor-pointer items-center gap-3 rounded-xl px-3 py-2 ${i === active ? "bg-black/[0.06]" : ""}`}
              >
                <PlayerAvatar name={stop.player.name} src={playerThumb(stop.player)} size={36} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-bold">{stop.player.nickname}</span>
                  <span className="block truncate text-sm text-black/55">{stop.player.name}</span>
                </span>
                <StopBadge stop={stop} rounds={rounds} />
              </li>
            ))
          )}
        </ul>
      )}
      <p aria-live="polite" className="sr-only">
        {selected
          ? `${selected.player.nickname}: ${stopText(selected, rounds)}`
          : expanded
            ? results.length > 0
              ? `Pronađeno igrača: ${results.length}`
              : "Nema igrača"
            : ""}
      </p>
    </div>
  );
}

/** „Polufinale" / „Ispao · Top 32" / „Šampion" — koliko je igrač stigao. */
export function stopText(stop: PlayerStop, rounds: PublicRound[]): string {
  const round = rounds[stop.roundIndex]?.name ?? "";
  if (stop.state === "champion") return "Šampion";
  if (stop.state === "third") return "3. mesto";
  if (stop.state === "out") return `Ispao · ${stop.series.thirdPlace ? "Za 3. mesto" : round}`;
  return stop.series.thirdPlace ? "Za 3. mesto" : round;
}

function StopBadge({ stop, rounds }: { stop: PlayerStop; rounds: PublicRound[] }) {
  const alive = stop.state === "playing" || stop.state === "champion";
  return (
    <span
      className={`shrink-0 rounded-full px-2 py-0.5 text-[11px] font-bold ${
        alive ? "bg-playoff/10 text-playoff" : "bg-black/[0.05] text-black/50"
      }`}
    >
      {stopText(stop, rounds)}
    </span>
  );
}
