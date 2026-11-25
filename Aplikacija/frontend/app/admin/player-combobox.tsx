"use client";

import { CheckIcon, XIcon } from "lucide-react";
import { useEffect, useId, useMemo, useRef, useState } from "react";
import { cn } from "cn";
import { Input } from "@/components/ui/input";
import type { SeasonPlayer } from "@/lib/api/queries";
import { searchPlayers } from "@/lib/player-search";

export type ComboboxPlayer = {
  id: number;
  nickname: string;
  fullName: string;
  /** Sitan tekst desno: grupa, „odustao"… */
  hint?: string;
  /** Prigušen red — igrač koji je odustao ili je isključen. */
  muted?: boolean;
};

const MAX_SHOWN = 30;

/**
 * Izbor igrača kucanjem, napravljen za telefon (admin unosi uveče sa telefona, CLAUDE.md §3.1).
 * <p>
 * Native {@code <datalist>} na telefonu ne prikazuje predloge pouzdano, pa je spisak ovde
 * nacrtan sam: ispod polja, u toku strane (ne lebdi, pa ga dijalog ne odseca), sa redovima
 * dovoljno visokim za prst. Pretraga je ista kao „Pronađi igrača" na javnom sajtu: bez
 * dijakritika („koco" nalazi „Kočo", „cvetkovic" nalazi „Cvetković") i trpi jednu slovnu grešku.
 */
export function PlayerCombobox({
  id,
  players,
  value,
  onChange,
  placeholder = "Ukucajte ime, prezime ili nadimak",
  disabled,
  autoFocus,
}: {
  id: string;
  players: ComboboxPlayer[];
  value: number | "";
  onChange: (value: number | "") => void;
  placeholder?: string;
  disabled?: boolean;
  autoFocus?: boolean;
}) {
  const listId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const [text, setText] = useState("");
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);

  const selected = players.find((player) => player.id === value);
  const selectedLabel = selected ? label(selected) : "";
  // Izbor se ponistava i kucanjem preko izabranog imena — tada tekst ostaje onakav kakav se kuca.
  const clearedByTyping = useRef(false);

  // Izabran igrač → u polju stoji njegovo ime; poništen izbor spolja (promena grupe) → prazno polje.
  useEffect(() => {
    if (selectedLabel || !clearedByTyping.current) {
      setText(selectedLabel);
    }
    clearedByTyping.current = false;
  }, [selectedLabel]);

  const hits = useMemo(() => {
    const query = selectedLabel && text === selectedLabel ? "" : text;
    const sorted = query.trim()
      ? searchPlayers(query, players, (player) => ({ nickname: player.nickname, name: player.fullName }))
      : [...players].sort((a, b) => a.nickname.localeCompare(b.nickname, "sr"));
    return sorted.slice(0, MAX_SHOWN);
  }, [players, selectedLabel, text]);

  // Dodir van polja i spiska zatvara spisak — na click, a ne na pointerdown. Android „nazad"
  // (prevlačenje od ivice ekrana) da bi se sklonila tastatura šalje pointerdown pa pointercancel,
  // bez click-a; sa pointerdown-om bi spisak nestao zajedno sa tastaturom. Isto radi i Radix dijalog.
  useEffect(() => {
    if (!open) {
      return;
    }
    function onClick(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
        if (selectedLabel) {
          setText(selectedLabel);
        }
      }
    }
    document.addEventListener("click", onClick, true);
    return () => document.removeEventListener("click", onClick, true);
  }, [open, selectedLabel]);

  // Na telefonu se polje podigne na vrh ekrana, da spisak stane iznad tastature.
  function scrollToTop() {
    if (!window.matchMedia("(pointer: coarse)").matches) {
      return;
    }
    window.setTimeout(() => rootRef.current?.scrollIntoView({ block: "start", behavior: "smooth" }), 300);
  }

  function choose(player: ComboboxPlayer) {
    onChange(player.id);
    setText(label(player));
    setOpen(false);
    inputRef.current?.blur();
  }

  function clear() {
    onChange("");
    setText("");
    setOpen(true);
    inputRef.current?.focus();
  }

  function onKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setOpen(true);
      setActive((index) => Math.min(index + 1, hits.length - 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActive((index) => Math.max(index - 1, 0));
    } else if (event.key === "Enter" && open) {
      // Enter bira igrača, a ne šalje formu u kojoj polje stoji.
      event.preventDefault();
      if (hits[active]) {
        choose(hits[active]);
      }
    } else if (event.key === "Escape" && open) {
      event.stopPropagation();
      setOpen(false);
    }
  }

  return (
    <div ref={rootRef} className="relative scroll-mt-16">
      <div className="relative">
        <Input
          ref={inputRef}
          id={id}
          role="combobox"
          aria-expanded={open}
          aria-controls={listId}
          aria-autocomplete="list"
          autoComplete="off"
          autoCorrect="off"
          autoCapitalize="none"
          spellCheck={false}
          enterKeyHint="search"
          autoFocus={autoFocus}
          disabled={disabled}
          placeholder={placeholder}
          className="h-11 pr-10 md:h-9"
          value={text}
          onFocus={(event) => {
            setOpen(true);
            event.currentTarget.select();
            scrollToTop();
          }}
          // Dodir na polje uvek vraća spisak, i kad je polje već u fokusu.
          onClick={() => setOpen(true)}
          onChange={(event) => {
            setText(event.target.value);
            setActive(0);
            setOpen(true);
            if (value !== "") {
              clearedByTyping.current = true;
              onChange("");
            }
          }}
          onKeyDown={onKeyDown}
        />
        {text && !disabled ? (
          <button
            type="button"
            aria-label="Obriši izbor"
            className="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-muted-foreground active:text-foreground"
            onClick={clear}
          >
            <XIcon className="size-4" />
          </button>
        ) : null}
      </div>

      {open && !disabled ? (
        <ul
          id={listId}
          role="listbox"
          className="mt-1 max-h-72 overflow-y-auto overscroll-contain rounded-lg border bg-popover p-1 shadow-md md:max-h-60"
        >
          {hits.length === 0 ? (
            <li className="px-3 py-3 text-sm text-muted-foreground">Nema igrača za „{text.trim()}“</li>
          ) : (
            hits.map((player, index) => (
              <li
                key={player.id}
                role="option"
                aria-selected={player.id === value}
                className={cn(
                  "flex min-h-11 cursor-pointer items-center gap-2 rounded-md px-3 py-2 select-none md:min-h-9",
                  index === active && "bg-muted",
                  player.muted && "opacity-60",
                )}
                // Bira se na click: prst koji krene (skrolovanje spiska) ne pravi click, pa ništa ne bira.
                // mousedown bez podrazumevane radnje drži fokus u polju, da tastatura ne skoči pre izbora.
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => choose(player)}
                onPointerEnter={() => setActive(index)}
              >
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium">{player.nickname}</span>
                  <span className="block truncate text-xs text-muted-foreground">{player.fullName}</span>
                </span>
                {player.hint ? (
                  <span className="shrink-0 text-xs text-muted-foreground">{player.hint}</span>
                ) : null}
                {player.id === value ? <CheckIcon className="size-4 shrink-0" /> : null}
              </li>
            ))
          )}
        </ul>
      ) : null}
    </div>
  );
}

/** „Kočo (Jovan Lazarević)" — nadimak prvi, jer ga liga koristi. */
function label(player: ComboboxPlayer): string {
  return player.nickname && player.nickname !== player.fullName
    ? `${player.nickname} (${player.fullName})`
    : player.fullName;
}

/** Igrač sezone kao stavka spiska: grupa desno, a odustali i isključeni prigušeni (BR-10). */
export function seasonPlayerOption(player: SeasonPlayer, showGroup = true): ComboboxPlayer {
  const out = player.status === "WITHDRAWN" ? "odustao" : player.status === "DISQUALIFIED" ? "isključen" : "";
  return {
    id: player.id,
    nickname: player.nickname || player.fullName || "",
    fullName: player.fullName ?? "",
    hint: [showGroup ? player.groupName : "", out].filter(Boolean).join(" · ") || undefined,
    muted: out !== "",
  };
}
