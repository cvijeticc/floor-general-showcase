"use client";

import { CalendarClock, Pencil, Trash2, Trophy, UserRoundPen } from "lucide-react";
import { useMemo, useRef, useState } from "react";
import { toast } from "sonner";
import { cn } from "cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { errorMessage } from "@/lib/api/client";
import { useCourts, useSeasonPlayers } from "@/lib/api/queries";
import { searchPlayers } from "@/lib/player-search";
import { PlayerCombobox, type ComboboxPlayer } from "../player-combobox";
import { splitSlot, toIsoSlot } from "../mecevi/types";
import {
  type RoundInfo,
  type SeriesInfo,
  useAddGame,
  useBracket,
  useCorrectGame,
  useCreateBracket,
  useDeleteGame,
  usePlayoffStage,
  useReopen,
  useSchedule,
  useSetPlayers,
  useUpdateRound,
  useWalkover,
} from "./use-playoff";

const SELECT_CLASS =
  "h-11 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 md:h-9";

const STATUS: Record<string, { label: string; variant: "default" | "secondary" | "outline" }> = {
  PENDING: { label: "Čeka igrače", variant: "outline" },
  READY: { label: "Spremno", variant: "secondary" },
  IN_PROGRESS: { label: "U toku", variant: "default" },
  FINISHED: { label: "Završeno", variant: "secondary" },
};

function code(series: SeriesInfo) {
  return `M${String(series.number ?? 0).padStart(2, "0")}`;
}

/**
 * Rok 3 — unos playoff-a (BR-08, BR-09). Jedan ekran po kolu, kartica po seriji: upiše se rezultat
 * meča i pritisne Enter; kad neko dođe do potrebnog broja pobeda, server ga sam postavi u sledeće
 * kolo (a poraženog iz polufinala u meč za 3. mesto). Svaka radnja ide na stek „Poništi" (Ctrl+Z).
 */
export function PlayoffBoard() {
  const { stage, isLoading: stageLoading } = usePlayoffStage();
  const bracket = useBracket(stage?.id);
  const createBracket = useCreateBracket();
  const [roundIndex, setRoundIndex] = useState<number | null>(null);
  const [query, setQuery] = useState("");
  const [openOnly, setOpenOnly] = useState(false);

  const rounds = bracket.data?.rounds ?? [];
  // Prvo kolo koje nije završeno — tu admin uveče upisuje.
  const current = rounds.findIndex((r) => (r.series ?? []).some((s) => s.status !== "FINISHED" && !s.thirdPlace));
  const index = roundIndex ?? (current < 0 ? Math.max(0, rounds.length - 1) : current);
  const round = rounds[index];

  const visible = useMemo(() => {
    let list = round?.series ?? [];
    if (openOnly) list = list.filter((s) => s.status !== "FINISHED");
    if (query.trim()) {
      const ids = new Set(
        searchPlayers(
          query,
          list.flatMap((s) => [s.player1, s.player2].filter((p): p is NonNullable<typeof p> => !!p)),
          (p) => ({ name: p.name ?? "", nickname: p.nickname ?? "" }),
        ).map((p) => p.id),
      );
      list = list.filter((s) => ids.has(s.player1?.id) || ids.has(s.player2?.id));
    }
    return list;
  }, [round, openOnly, query]);

  if (stageLoading || bracket.isLoading) {
    return <p className="text-sm text-muted-foreground">Učitavanje…</p>;
  }
  if (!stage) {
    return (
      <p className="text-sm text-muted-foreground">
        Tekuća sezona nema playoff fazu (KNOCKOUT). Dodajte je na ekranu Sezone, sa veličinom drveta.
      </p>
    );
  }
  if (rounds.length === 0) {
    return (
      <div className="space-y-3 rounded-xl border bg-background p-5">
        <p className="text-sm">
          Faza <b>{stage.name}</b> još nema drvo. Pravi se prema veličini drveta faze: sva kola, serije i meč za
          3. mesto. Parovi prvog kola se zatim unose na kartici svake serije.
        </p>
        <Button
          disabled={createBracket.isPending}
          onClick={async () => {
            try {
              await createBracket.mutateAsync(stage.id as number);
              toast.success("Drvo je napravljeno");
            } catch (error) {
              toast.error(errorMessage(error, "Drvo nije napravljeno"));
            }
          }}
        >
          Napravi drvo
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <nav aria-label="Kola" className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 md:mx-0 md:flex-wrap md:px-0">
        {rounds.map((r, i) => {
          const main = (r.series ?? []).filter((s) => !s.thirdPlace);
          const done = main.filter((s) => s.status === "FINISHED").length;
          return (
            <button
              key={r.id}
              type="button"
              onClick={() => setRoundIndex(i)}
              aria-current={i === index ? "true" : undefined}
              className={cn(
                "shrink-0 rounded-full border px-4 py-2 text-sm font-medium transition-colors",
                i === index ? "border-foreground bg-foreground text-background" : "bg-background hover:bg-muted",
              )}
            >
              {r.name}
              <span className={cn("ml-2 tabular-nums", i === index ? "opacity-70" : "text-muted-foreground")}>
                {done}/{main.length}
              </span>
            </button>
          );
        })}
      </nav>

      {round && <RoundFormat key={round.id} round={round} />}

      <div className="flex flex-wrap items-center gap-3">
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Nađi igrača u ovom kolu"
          className="h-11 w-full sm:w-72 md:h-9"
        />
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={openOnly} onChange={(e) => setOpenOnly(e.target.checked)} className="size-4" />
          Samo nerešene
        </label>
        <span className="text-sm text-muted-foreground">
          Upiši rezultat i pritisni Enter — pobednik ide sam u sledeće kolo.
        </span>
      </div>

      {visible.length === 0 ? (
        <p className="py-8 text-center text-sm text-muted-foreground">Nema serija za prikaz.</p>
      ) : (
        <div className="grid gap-3 md:grid-cols-2">
          {visible.map((series) => (
            <SeriesEditor key={series.id} series={series} round={round as RoundInfo} />
          ))}
        </div>
      )}
    </div>
  );
}

/** Naziv i format kola — broj pobeda u seriji i format meča (prazno = format sezone). */
function RoundFormat({ round }: { round: RoundInfo }) {
  const update = useUpdateRound();
  const [name, setName] = useState(round.name ?? "");
  const [wins, setWins] = useState(String(round.winsRequired ?? 1));
  const [target, setTarget] = useState(round.ownFormat ? String(round.targetScore ?? "") : "");
  const [max, setMax] = useState(round.ownFormat ? String(round.maxScore ?? "") : "");
  const [duration, setDuration] = useState(round.ownFormat ? String(round.durationMinutes ?? "") : "");

  return (
    <details className="rounded-xl border bg-background px-4 py-3">
      <summary className="cursor-pointer text-sm">
        <b>{round.name}</b> — {round.winsRequired === 1 ? "jedna utakmica" : `na ${round.winsRequired} pobede`}, do{" "}
        {round.targetScore} ili {round.durationMinutes} min (najviše {round.maxScore})
        {!round.ownFormat && <span className="text-muted-foreground"> · format sezone</span>}
      </summary>
      <form
        className="mt-3 grid gap-3 sm:grid-cols-5"
        onSubmit={async (event) => {
          event.preventDefault();
          try {
            await update.mutateAsync({
              roundId: round.id as number,
              name,
              winsRequired: Number(wins),
              targetScore: target ? Number(target) : undefined,
              maxScore: max ? Number(max) : undefined,
              durationMinutes: duration ? Number(duration) : undefined,
            });
            toast.success(`Kolo „${name}" je sačuvano`);
          } catch (error) {
            toast.error(errorMessage(error, "Kolo nije sačuvano"));
          }
        }}
      >
        <Field label="Naziv">
          <Input value={name} onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Pobeda za prolaz">
          <Input inputMode="numeric" value={wins} onChange={(e) => setWins(e.target.value)} />
        </Field>
        <Field label="Do poena (prazno = sezona)">
          <Input inputMode="numeric" value={target} onChange={(e) => setTarget(e.target.value)} />
        </Field>
        <Field label="Najviše poena">
          <Input inputMode="numeric" value={max} onChange={(e) => setMax(e.target.value)} />
        </Field>
        <Field label="Minuta">
          <div className="flex gap-2">
            <Input inputMode="numeric" value={duration} onChange={(e) => setDuration(e.target.value)} />
            <Button type="submit" disabled={update.isPending}>
              Sačuvaj
            </Button>
          </div>
        </Field>
      </form>
    </details>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      {children}
    </div>
  );
}

type Panel = null | "walkover" | "players" | "schedule";

function SeriesEditor({ series, round }: { series: SeriesInfo; round: RoundInfo }) {
  const addGame = useAddGame();
  const correctGame = useCorrectGame();
  const deleteGame = useDeleteGame();
  const walkover = useWalkover();
  const reopen = useReopen();
  const [score1, setScore1] = useState("");
  const [score2, setScore2] = useState("");
  const [editing, setEditing] = useState<number | null>(null);
  const [panel, setPanel] = useState<Panel>(null);
  const firstInput = useRef<HTMLInputElement>(null);

  const p1 = series.player1;
  const p2 = series.player2;
  const games = series.games ?? [];
  const decided = series.winnerId != null;
  const canPlay = !!p1 && !!p2 && !decided && !series.walkover;
  const single = round.winsRequired === 1;
  const status = series.walkover ? { label: "Službeno", variant: "outline" as const } : STATUS[series.status ?? "READY"];

  async function submitGame(event: React.FormEvent) {
    event.preventDefault();
    if (score1 === "" || score2 === "") return;
    try {
      const after = await addGame.mutateAsync({ seriesId: series.id as number, score1: Number(score1), score2: Number(score2) });
      setScore1("");
      setScore2("");
      if (after.winnerId != null) {
        const winner = after.winnerId === p1?.id ? p1 : p2;
        toast.success(`${code(series)} je rešena — dalje ide ${winner?.nickname}`);
      } else {
        toast.success(`${code(series)}: upisan ${games.length + 1}. meč ${score1}:${score2}`);
        firstInput.current?.focus();
      }
    } catch (error) {
      toast.error(errorMessage(error, "Meč nije upisan"));
    }
  }

  return (
    <article
      className={cn(
        "space-y-3 rounded-xl border bg-background p-3 md:p-4",
        series.status === "IN_PROGRESS" && "border-orange-400",
        decided && "bg-muted/40",
      )}
    >
      <header className="flex flex-wrap items-center gap-2 text-sm">
        <span className="font-mono text-xs font-semibold text-muted-foreground">{code(series)}</span>
        {series.thirdPlace && <span className="font-medium">Za 3. mesto</span>}
        <Badge variant={status.variant}>{status.label}</Badge>
        {series.scheduledStart && (
          <span className="text-xs text-muted-foreground">
            {formatSlot(series.scheduledStart)}
            {series.courtName ? ` · ${series.courtName}` : ""}
          </span>
        )}
        <span className="ml-auto flex gap-1">
          <IconButton label="Termin serije" onClick={() => setPanel(panel === "schedule" ? null : "schedule")}>
            <CalendarClock className="size-4" />
          </IconButton>
          {games.length === 0 && !decided && (
            <IconButton label="Uredi par" onClick={() => setPanel(panel === "players" ? null : "players")}>
              <UserRoundPen className="size-4" />
            </IconButton>
          )}
        </span>
      </header>

      <div className="space-y-1">
        <PlayerLine player={p1} seed={series.seed1} wins={series.wins1 ?? 0} won={decided && series.winnerId === p1?.id} single={single} games={games} slot={1} />
        <PlayerLine player={p2} seed={series.seed2} wins={series.wins2 ?? 0} won={decided && series.winnerId === p2?.id} single={single} games={games} slot={2} />
      </div>

      {games.length > 0 && (
        <ol className="space-y-1 text-sm">
          {games.map((game, i) =>
            editing === game.matchId ? (
              <GameEdit
                key={game.matchId}
                initial={[game.score1 ?? 0, game.score2 ?? 0]}
                pending={correctGame.isPending}
                onCancel={() => setEditing(null)}
                onSave={async (a, b) => {
                  try {
                    await correctGame.mutateAsync({
                      seriesId: series.id as number,
                      matchId: game.matchId as number,
                      score1: a,
                      score2: b,
                    });
                    setEditing(null);
                    toast.success(`${code(series)}: ${game.gameNumber}. meč ispravljen na ${a}:${b}`);
                  } catch (error) {
                    toast.error(errorMessage(error, "Ispravka nije sačuvana"));
                  }
                }}
              />
            ) : (
              <li key={game.matchId} className="flex items-center gap-2 rounded-md bg-muted/60 px-2 py-1">
                <span className="w-14 text-xs text-muted-foreground">{game.gameNumber}. meč</span>
                <span className="font-semibold tabular-nums">
                  {game.score1}:{game.score2}
                </span>
                <span className="ml-auto flex gap-1">
                  <IconButton label="Ispravi rezultat" onClick={() => setEditing(game.matchId as number)}>
                    <Pencil className="size-3.5" />
                  </IconButton>
                  {i === games.length - 1 && (
                    <IconButton
                      label="Obriši poslednji meč"
                      onClick={async () => {
                        try {
                          await deleteGame.mutateAsync({ seriesId: series.id as number, matchId: game.matchId as number });
                          toast.success(`${code(series)}: obrisan ${game.gameNumber}. meč (Ctrl+Z vraća)`);
                        } catch (error) {
                          toast.error(errorMessage(error, "Meč nije obrisan"));
                        }
                      }}
                    >
                      <Trash2 className="size-3.5" />
                    </IconButton>
                  )}
                </span>
              </li>
            ),
          )}
        </ol>
      )}

      {canPlay && (
        <form onSubmit={submitGame} className="flex items-end gap-2">
          <div className="min-w-0 flex-1 space-y-1">
            <Label htmlFor={`s1-${series.id}`} className="block truncate text-xs text-muted-foreground">
              {p1?.nickname}
            </Label>
            <Input
              ref={firstInput}
              id={`s1-${series.id}`}
              inputMode="numeric"
              pattern="[0-9]*"
              value={score1}
              onChange={(e) => setScore1(e.target.value.replace(/\D/g, ""))}
              className="h-11 text-center text-lg font-semibold tabular-nums md:h-9 md:text-base"
              placeholder="0"
            />
          </div>
          <span className="pb-2 text-muted-foreground">:</span>
          <div className="min-w-0 flex-1 space-y-1">
            <Label htmlFor={`s2-${series.id}`} className="block truncate text-xs text-muted-foreground">
              {p2?.nickname}
            </Label>
            <Input
              id={`s2-${series.id}`}
              inputMode="numeric"
              pattern="[0-9]*"
              value={score2}
              onChange={(e) => setScore2(e.target.value.replace(/\D/g, ""))}
              className="h-11 text-center text-lg font-semibold tabular-nums md:h-9 md:text-base"
              placeholder="0"
            />
          </div>
          <Button type="submit" className="h-11 md:h-9" disabled={addGame.isPending || score1 === "" || score2 === ""}>
            {single ? "Upiši" : `Upiši ${games.length + 1}. meč`}
          </Button>
        </form>
      )}

      <div className="flex flex-wrap gap-2">
        {p1 && p2 && !decided && (
          <Button variant="outline" size="sm" onClick={() => setPanel(panel === "walkover" ? null : "walkover")}>
            Službeni rezultat
          </Button>
        )}
        {series.walkover && (
          <Button
            variant="outline"
            size="sm"
            disabled={reopen.isPending}
            onClick={async () => {
              try {
                await reopen.mutateAsync(series.id as number);
                toast.success(`${code(series)}: službeni rezultat je poništen`);
              } catch (error) {
                toast.error(errorMessage(error, "Službeni rezultat nije poništen"));
              }
            }}
          >
            Poništi službeni rezultat
          </Button>
        )}
        {decided && !series.walkover && (
          <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
            <Trophy className="size-4" /> Dalje ide {series.winnerId === p1?.id ? p1?.nickname : p2?.nickname}
          </span>
        )}
      </div>

      {panel === "walkover" && p1 && p2 && (
        <WalkoverPanel
          series={series}
          pending={walkover.isPending}
          onConfirm={async (winnerId, reason) => {
            try {
              await walkover.mutateAsync({ seriesId: series.id as number, winnerId, reason });
              setPanel(null);
              toast.success(`${code(series)}: službeni rezultat upisan`);
            } catch (error) {
              toast.error(errorMessage(error, "Službeni rezultat nije upisan"));
            }
          }}
        />
      )}
      {panel === "players" && <PlayersPanel series={series} onDone={() => setPanel(null)} />}
      {panel === "schedule" && <SchedulePanel series={series} onDone={() => setPanel(null)} />}
    </article>
  );
}

function PlayerLine({
  player,
  seed,
  wins,
  won,
  single,
  games,
  slot,
}: {
  player: SeriesInfo["player1"];
  seed?: string;
  wins: number;
  won: boolean;
  single: boolean;
  games: NonNullable<SeriesInfo["games"]>;
  slot: 1 | 2;
}) {
  if (!player) {
    return <p className="text-sm text-muted-foreground">— čeka se pobednik prethodne serije</p>;
  }
  return (
    <p className="flex items-center gap-2 text-sm">
      <span className={cn("truncate", won ? "font-bold" : "font-medium")}>{player.nickname}</span>
      <span className="truncate text-xs text-muted-foreground">{player.name}</span>
      {seed && <span className="shrink-0 text-xs text-muted-foreground">· {seed}</span>}
      <span className="ml-auto shrink-0 tabular-nums">
        {single ? (games[0] ? (slot === 1 ? games[0].score1 : games[0].score2) : "") : games.length ? <b>{wins}</b> : ""}
      </span>
    </p>
  );
}

function GameEdit({
  initial,
  pending,
  onSave,
  onCancel,
}: {
  initial: [number, number];
  pending: boolean;
  onSave: (a: number, b: number) => void;
  onCancel: () => void;
}) {
  const [a, setA] = useState(String(initial[0]));
  const [b, setB] = useState(String(initial[1]));
  return (
    <li>
      <form
        className="flex items-center gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          onSave(Number(a), Number(b));
        }}
      >
        <Input autoFocus inputMode="numeric" value={a} onChange={(e) => setA(e.target.value.replace(/\D/g, ""))} className="h-9 w-16 text-center" />
        <span>:</span>
        <Input inputMode="numeric" value={b} onChange={(e) => setB(e.target.value.replace(/\D/g, ""))} className="h-9 w-16 text-center" />
        <Button type="submit" size="sm" disabled={pending}>
          Sačuvaj
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={onCancel}>
          Odustani
        </Button>
      </form>
    </li>
  );
}

function WalkoverPanel({
  series,
  pending,
  onConfirm,
}: {
  series: SeriesInfo;
  pending: boolean;
  onConfirm: (winnerId: number, reason?: string) => void;
}) {
  const [reason, setReason] = useState("");
  const p1 = series.player1;
  const p2 = series.player2;
  return (
    <div className="space-y-2 rounded-lg border border-dashed p-3">
      <p className="text-sm">Ko se nije pojavio ili je predao? Dalje ide:</p>
      <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Razlog (npr. nije došao)" />
      <div className="flex flex-wrap gap-2">
        {[p1, p2].map((p) =>
          p ? (
            <Button key={p.id} size="sm" disabled={pending} onClick={() => onConfirm(p.id as number, reason || undefined)}>
              Dalje ide {p.nickname}
            </Button>
          ) : null,
        )}
      </div>
    </div>
  );
}

function PlayersPanel({ series, onDone }: { series: SeriesInfo; onDone: () => void }) {
  const { players } = useSeasonPlayers();
  const setPlayers = useSetPlayers();
  const [first, setFirst] = useState<number | "">(series.player1?.id ?? "");
  const [second, setSecond] = useState<number | "">(series.player2?.id ?? "");
  const [seed1, setSeed1] = useState(series.seed1 ?? "");
  const [seed2, setSeed2] = useState(series.seed2 ?? "");
  const options: ComboboxPlayer[] = players.map((p) => ({
    id: p.id,
    nickname: p.nickname ?? "",
    fullName: p.fullName ?? "",
    hint: p.groupName,
  }));
  return (
    <div className="space-y-2 rounded-lg border border-dashed p-3">
      <p className="text-sm">Par serije (npr. zamena igrača koji je odustao). Menja se dok serija nema nijedan meč.</p>
      <div className="grid gap-2 sm:grid-cols-[1fr_8rem]">
        <PlayerCombobox id={`p1-${series.id}`} players={options} value={first} onChange={setFirst} />
        <Input value={seed1} onChange={(e) => setSeed1(e.target.value)} placeholder="npr. 1. Gr. 1" />
        <PlayerCombobox id={`p2-${series.id}`} players={options} value={second} onChange={setSecond} />
        <Input value={seed2} onChange={(e) => setSeed2(e.target.value)} placeholder="npr. 7. Gr. 2" />
      </div>
      <Button
        size="sm"
        disabled={setPlayers.isPending}
        onClick={async () => {
          try {
            await setPlayers.mutateAsync({
              seriesId: series.id as number,
              player1Id: first === "" ? null : first,
              player2Id: second === "" ? null : second,
              seed1: seed1 || undefined,
              seed2: seed2 || undefined,
            });
            toast.success(`${code(series)}: par je sačuvan`);
            onDone();
          } catch (error) {
            toast.error(errorMessage(error, "Par nije sačuvan"));
          }
        }}
      >
        Sačuvaj par
      </Button>
    </div>
  );
}

function SchedulePanel({ series, onDone }: { series: SeriesInfo; onDone: () => void }) {
  const courts = useCourts();
  const schedule = useSchedule();
  const slot = splitSlot(series.scheduledStart);
  const [date, setDate] = useState(slot.date);
  const [time, setTime] = useState(slot.time);
  const [courtId, setCourtId] = useState<number | "">(series.courtId ?? "");
  return (
    <div className="space-y-2 rounded-lg border border-dashed p-3">
      <p className="text-sm">Termin serije — svi mečevi serije se igraju istog dana.</p>
      <div className="flex flex-wrap gap-2">
        <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} className="h-11 w-44 md:h-9" />
        <Input type="time" value={time} onChange={(e) => setTime(e.target.value)} className="h-11 w-32 md:h-9" />
        <select
          className={SELECT_CLASS}
          value={courtId}
          onChange={(e) => setCourtId(e.target.value === "" ? "" : Number(e.target.value))}
        >
          <option value="">Teren —</option>
          {(courts.data ?? []).map((court) => (
            <option key={court.id} value={court.id}>
              {court.name}
            </option>
          ))}
        </select>
        <Button
          size="sm"
          className="h-11 md:h-9"
          disabled={schedule.isPending}
          onClick={async () => {
            try {
              await schedule.mutateAsync({
                seriesId: series.id as number,
                scheduledStart: toIsoSlot(date, time) ?? null,
                courtId: courtId === "" ? null : courtId,
              });
              toast.success(`${code(series)}: termin je sačuvan`);
              onDone();
            } catch (error) {
              toast.error(errorMessage(error, "Termin nije sačuvan"));
            }
          }}
        >
          Sačuvaj termin
        </Button>
      </div>
    </div>
  );
}

function IconButton({ label, onClick, children }: { label: string; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      type="button"
      title={label}
      aria-label={label}
      onClick={onClick}
      className="flex size-9 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground md:size-8"
    >
      {children}
    </button>
  );
}

const DAYS = ["ned", "pon", "uto", "sre", "čet", "pet", "sub"];

function formatSlot(iso: string) {
  const [date, time] = iso.split("T");
  const [y, m, d] = date.split("-").map(Number);
  const day = DAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()];
  const hhmm = (time ?? "").slice(0, 5);
  return `${day} ${String(d).padStart(2, "0")}.${String(m).padStart(2, "0")}.${hhmm && hhmm !== "00:00" ? ` ${hhmm}` : ""}`;
}
