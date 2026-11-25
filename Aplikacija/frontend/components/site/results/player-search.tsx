"use client";

import Form from "next/form";
import { useRouter } from "next/navigation";
import { useEffect, useId, useMemo, useRef, useState } from "react";
import { searchPlayers } from "@/lib/player-search";
import { PlayerAvatar } from "./player-avatar";

export type SearchOption = {
  id: number;
  nickname: string;
  name: string;
  groupName: string;
  href: string;
  thumb: string | null;
};

const MAX_SUGGESTIONS = 8;

/**
 * „Pronađi igrača" (UC-PUB-10) — svaki igrač na /rezultati prvo traži sebe. Predlozi rade bez
 * dijakritika i trpe jednu slovnu grešku („milos risic" → Miloš Ristić); strelice + Enter otvaraju
 * profil, Enter bez izbora vodi na tab „Igrači" sa svim poklapanjima. Bez JS-a je obična GET forma.
 * ARIA 1.2 combobox: fokus ostaje u polju, izabrana opcija ide kroz aria-activedescendant.
 */
export function PlayerSearch({ players, defaultValue = "" }: { players: SearchOption[]; defaultValue?: string }) {
  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);
  const listId = useId();
  const [query, setQuery] = useState(defaultValue);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [maxHeight, setMaxHeight] = useState<number>();

  const trimmed = query.trim();
  const results = useMemo(
    () => (trimmed ? searchPlayers(trimmed, players, (p) => p, MAX_SUGGESTIONS) : []),
    [trimmed, players],
  );
  const expanded = open && trimmed.length > 0;

  function openList() {
    // Na laptopu 14" (551px visine) polje je pri dnu crne trake — lista ne sme da ode ispod ekrana.
    const rect = inputRef.current?.getBoundingClientRect();
    if (rect) setMaxHeight(Math.max(180, window.innerHeight - rect.bottom - 16));
    setOpen(true);
  }

  // Polje je nekontrolisano: na sporom telefonu čovek počne da kuca pre nego što se JS učita,
  // a kontrolisano polje bi mu pri hidrataciji obrisalo tekst. Ovde se otkucano samo preuzme.
  useEffect(() => {
    const input = inputRef.current;
    if (!input || input.value === defaultValue) return;
    setQuery(input.value);
    if (document.activeElement === input) {
      setMaxHeight(Math.max(180, window.innerHeight - input.getBoundingClientRect().bottom - 16));
      setOpen(true);
    }
  }, [defaultValue]);

  function go(option: SearchOption) {
    setOpen(false);
    router.push(option.href);
  }

  function onKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      if (!expanded) openList();
      setActive((i) => Math.min(i + 1, results.length - 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActive((i) => Math.max(i - 1, -1));
    } else if (event.key === "Enter") {
      const pick = results[active] ?? (results.length === 1 ? results[0] : undefined);
      if (expanded && pick) {
        event.preventDefault();
        go(pick);
      }
      // Inače forma ide na /rezultati?tab=igraci&q=… — spisak svih poklapanja.
    } else if (event.key === "Escape") {
      setOpen(false);
      setActive(-1);
    }
  }

  return (
    <Form action="/rezultati" role="search" className="relative w-full lg:w-[340px]">
      <input type="hidden" name="tab" value="igraci" />
      <label htmlFor={`${listId}-input`} className="sr-only">
        Pronađi igrača po nadimku, imenu ili prezimenu
      </label>
      <svg
        aria-hidden
        viewBox="0 0 24 24"
        className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-black/45"
        fill="none"
        stroke="currentColor"
        strokeWidth={2.2}
        strokeLinecap="round"
      >
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-3.5-3.5" />
      </svg>
      <input
        ref={inputRef}
        id={`${listId}-input`}
        name="q"
        type="search"
        autoComplete="off"
        spellCheck={false}
        enterKeyHint="search"
        placeholder="Pronađi igrača"
        defaultValue={defaultValue}
        onChange={(event) => {
          setQuery(event.target.value);
          setActive(-1);
          openList();
        }}
        onFocus={openList}
        onBlur={() => setOpen(false)}
        onKeyDown={onKeyDown}
        role="combobox"
        aria-expanded={expanded}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={expanded && active >= 0 ? `${listId}-${active}` : undefined}
        className="h-12 w-full rounded-full bg-white pl-12 pr-5 text-base text-black outline-none placeholder:text-black/45 focus-visible:ring-4 focus-visible:ring-prize-gold/60 [&::-webkit-search-cancel-button]:hidden"
      />

      {expanded && (
        <ul
          id={listId}
          role="listbox"
          aria-label="Igrači"
          style={{ maxHeight }}
          // Klik na predlog ne sme da skine fokus sa polja pre nego što se izbor obradi.
          onMouseDown={(event) => event.preventDefault()}
          className="absolute inset-x-0 top-full z-40 mt-2 overflow-y-auto rounded-2xl bg-white p-1.5 text-black shadow-2xl ring-1 ring-black/10"
        >
          {results.length === 0 ? (
            <li className="px-3 py-3 text-sm text-black/60">Nema igrača za „{trimmed}“. Probaj nadimak ili prezime.</li>
          ) : (
            results.map((p, i) => (
              <li
                key={p.id}
                id={`${listId}-${i}`}
                role="option"
                aria-selected={i === active}
                onMouseEnter={() => setActive(i)}
                onClick={() => go(p)}
                className={`flex cursor-pointer items-center gap-3 rounded-xl px-3 py-2 ${i === active ? "bg-black/[0.06]" : ""}`}
              >
                <PlayerAvatar name={p.name} src={p.thumb} size={36} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-bold">{p.nickname}</span>
                  <span className="block truncate text-sm text-black/55">{p.name}</span>
                </span>
                <span className="shrink-0 text-xs text-black/50">{p.groupName}</span>
              </li>
            ))
          )}
        </ul>
      )}
      <p aria-live="polite" className="sr-only">
        {expanded ? (results.length > 0 ? `Pronađeno igrača: ${results.length}` : "Nema igrača") : ""}
      </p>
    </Form>
  );
}
