"use client";

import { useState, useSyncExternalStore } from "react";
import { toast } from "sonner";
import { cn } from "cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { errorMessage } from "@/lib/api/client";
import { useSeasonGroups, useSeasonPlayers, type SeasonPlayer } from "@/lib/api/queries";
import { PlayerCombobox, seasonPlayerOption } from "../player-combobox";
import { MatchFormDialog } from "./match-form";
import { CorrectionDialog, ForfeitDialog, PostponeDialog } from "./result-dialogs";
import {
  formatSlot,
  hasResult,
  STATUS_LABELS,
  STATUS_ORDER,
  type MatchRow,
  type MatchStatus,
} from "./types";
import { EMPTY_FILTERS, useDeleteMatch, useEnterResults, useMatches, type MatchFilters } from "./use-matches";

const SELECT_CLASS =
  "h-11 md:h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base md:text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50";

/** Upisani rezultati koji jos nisu poslati — kljuc je ID meca. */
type DraftScores = Record<number, { score1: string; score2: string }>;

/** Radnje nad jednim mecom — iste za red tabele i za karticu na telefonu. */
type RowActions = {
  /** Nadimak po ID-u igrača — liga ljude zna po nadimku, a lista mečeva nosi puno ime. */
  nicknames: Map<number, string>;
  scores: DraftScores;
  setScore: (matchId: number, side: "score1" | "score2", value: string) => void;
  onEdit: (match: MatchRow) => void;
  onPostpone: (match: MatchRow) => void;
  onForfeit: (match: MatchRow) => void;
  onCorrect: (match: MatchRow) => void;
  onDelete: (match: MatchRow) => void;
};

export function MatchesTable() {
  const [filters, setFilters] = useState<MatchFilters>(EMPTY_FILTERS);
  const [scores, setScores] = useState<DraftScores>({});
  const [formOpen, setFormOpen] = useState(false);
  const [editedId, setEditedId] = useState<number | undefined>(undefined);
  const [forfeitMatch, setForfeitMatch] = useState<MatchRow | undefined>(undefined);
  const [correctedMatch, setCorrectedMatch] = useState<MatchRow | undefined>(undefined);
  const [postponedMatch, setPostponedMatch] = useState<MatchRow | undefined>(undefined);

  const matches = useMatches(filters);
  const { groups } = useSeasonGroups();
  const { players } = useSeasonPlayers();
  const enterResults = useEnterResults();
  const deleteMatch = useDeleteMatch();
  const isDesktop = useIsDesktop();

  const rows = matches.data ?? [];
  const isFiltered = Object.entries(filters).some(([, value]) => value !== "");

  const filled = Object.entries(scores)
    .filter(([, value]) => value.score1.trim() !== "" && value.score2.trim() !== "")
    .map(([matchId, value]) => ({
      matchId: Number(matchId),
      scorePlayer1: Number(value.score1),
      scorePlayer2: Number(value.score2),
    }));

  function setScore(matchId: number, side: "score1" | "score2", value: string) {
    setScores((current) => {
      const row = current[matchId] ?? { score1: "", score2: "" };
      return { ...current, [matchId]: { ...row, [side]: value } };
    });
  }

  /**
   * Masovni unos: jedan poziv za celo kolo. Backend upise sve ispravne redove i vrati listu
   * odbijenih sa razlogom — odbijeni ostaju u poljima da se isprave, upisani se ciste.
   */
  async function saveAll() {
    if (filled.length === 0) {
      return;
    }
    // Fokus se sklanja iz polja da bi Ctrl+Z odmah radio kao „Poništi", a ne kao undo teksta.
    (document.activeElement as HTMLElement | null)?.blur();

    try {
      const info = await enterResults.mutateAsync(filled);
      const rejected = info.rejected ?? [];

      setScores((current) => {
        const left: DraftScores = {};
        for (const row of rejected) {
          if (row.matchId != null && current[row.matchId]) {
            left[row.matchId] = current[row.matchId];
          }
        }
        return left;
      });

      if (info.appliedCount) {
        toast.success(`Upisano ${info.appliedCount} ${info.appliedCount === 1 ? "rezultat" : "rezultata"}`);
      }
      for (const row of rejected.slice(0, 5)) {
        toast.error(`Meč br. ${row.matchId}: ${row.reason}`);
      }
    } catch (error) {
      toast.error(errorMessage(error, "Rezultati nisu upisani"));
    }
  }

  async function onDelete(match: MatchRow) {
    if (match.id == null) {
      return;
    }
    try {
      await deleteMatch.mutateAsync(match.id);
      toast.success(`Meč br. ${match.id} je obrisan`);
    } catch (error) {
      // Mec sa upisanim rezultatom backend odbija — poruka je vec na srpskom.
      toast.error(errorMessage(error, "Meč nije obrisan"));
    }
  }

  const nicknames = new Map(players.map((p) => [p.id, p.nickname ?? ""]));

  const actions: RowActions = {
    nicknames,
    scores,
    setScore,
    onEdit: (match) => {
      setEditedId(match.id as number);
      setFormOpen(true);
    },
    onPostpone: setPostponedMatch,
    onForfeit: setForfeitMatch,
    onCorrect: setCorrectedMatch,
    onDelete: (match) => void onDelete(match),
  };

  const options = players.map((p) => seasonPlayerOption(p));
  const player = players.find((p) => p.id === filters.playerId);
  const opponent = players.find((p) => p.id === filters.opponentId);

  const emptyText = opponent
    ? "Ova dva igrača još nisu imala nijedan meč"
    : isFiltered
      ? "Nijedan meč ne odgovara filterima"
      : "Još nema unetih mečeva";

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button
          className="h-11 w-full md:h-9 md:w-auto"
          onClick={() => {
            setEditedId(undefined);
            setFormOpen(true);
          }}
        >
          Novi meč
        </Button>
      </div>

      {/* Istorija igrača: „koco", pa protivnik „cvetkovic" — da li su i koliko puta igrali. */}
      <div className="grid gap-3 rounded-lg border bg-background p-3 md:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="filter-igrac">Igrač</Label>
          <PlayerCombobox
            id="filter-igrac"
            players={options}
            value={filters.playerId}
            onChange={(value) =>
              setFilters((f) => ({
                ...f,
                playerId: value,
                opponentId: value === "" || value === f.opponentId ? "" : f.opponentId,
              }))
            }
            placeholder="Svi mečevi igrača — ukucajte ime"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="filter-protivnik">Protivnik</Label>
          <PlayerCombobox
            id="filter-protivnik"
            players={options.filter((option) => option.id !== filters.playerId)}
            value={filters.opponentId}
            onChange={(value) => setFilters((f) => ({ ...f, opponentId: value }))}
            disabled={filters.playerId === ""}
            placeholder={filters.playerId === "" ? "Prvo izaberite igrača" : "Međusobni mečevi sa…"}
          />
        </div>

        {player && !matches.isLoading ? (
          <HistorySummary player={player} opponent={opponent} rows={rows} className="md:col-span-2" />
        ) : null}
      </div>

      <div className="grid grid-cols-2 gap-3 md:flex md:flex-wrap md:items-end">
        <div className="space-y-1.5 md:w-40">
          <Label htmlFor="grupa">Grupa</Label>
          <select
            id="grupa"
            className={SELECT_CLASS}
            value={filters.groupId}
            onChange={(e) =>
              setFilters((f) => ({ ...f, groupId: e.target.value === "" ? "" : Number(e.target.value) }))
            }
          >
            <option value="">Sve grupe</option>
            {groups.map((group) => (
              <option key={group.id} value={group.id}>
                {group.name}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1.5 md:w-24">
          <Label htmlFor="kolo">Kolo</Label>
          <Input
            id="kolo"
            type="number"
            inputMode="numeric"
            min={1}
            className="h-11 md:h-9"
            placeholder="sva"
            value={filters.round}
            onChange={(e) => setFilters((f) => ({ ...f, round: e.target.value }))}
          />
        </div>

        <div className="space-y-1.5 md:w-44">
          <Label htmlFor="datum">Datum</Label>
          <Input
            id="datum"
            type="date"
            className="h-11 md:h-9"
            value={filters.date}
            onChange={(e) => setFilters((f) => ({ ...f, date: e.target.value }))}
          />
        </div>

        <div className="space-y-1.5 md:w-40">
          <Label htmlFor="status">Status</Label>
          <select
            id="status"
            className={SELECT_CLASS}
            value={filters.status}
            onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value as MatchStatus | "" }))}
          >
            <option value="">Svi statusi</option>
            {STATUS_ORDER.map((status) => (
              <option key={status} value={status}>
                {STATUS_LABELS[status]}
              </option>
            ))}
          </select>
        </div>

        {isFiltered ? (
          <Button
            variant="outline"
            className="col-span-2 h-11 md:col-span-1 md:h-9"
            onClick={() => setFilters(EMPTY_FILTERS)}
          >
            Poništi filtere
          </Button>
        ) : null}
      </div>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          void saveAll();
        }}
        className="space-y-3"
      >
        {matches.isLoading ? (
          <Message>Učitavanje…</Message>
        ) : matches.error ? (
          <Message variant="error">{errorMessage(matches.error, "Lista mečeva nije učitana")}</Message>
        ) : rows.length === 0 ? (
          <Message>{emptyText}</Message>
        ) : isDesktop ? (
          <MatchesGrid rows={rows} actions={actions} />
        ) : (
          <ul className="space-y-3">
            {rows.map((match) => (
              <MatchCard key={match.id} match={match} actions={actions} />
            ))}
          </ul>
        )}

        {/* Na telefonu dugme za čuvanje stoji pri dnu ekrana, dok se kroz kolo skroluje. */}
        <div
          className={cn(
            "flex flex-wrap items-center gap-3",
            filled.length > 0 &&
              "sticky bottom-0 z-10 -mx-4 border-t bg-background/95 px-4 pt-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] backdrop-blur md:static md:mx-0 md:border-0 md:bg-transparent md:p-0 md:backdrop-blur-none",
          )}
        >
          <Button
            type="submit"
            className="h-11 w-full md:h-9 md:w-auto"
            disabled={filled.length === 0 || enterResults.isPending}
          >
            {enterResults.isPending
              ? "Upisivanje…"
              : `Sačuvaj sve rezultate${filled.length ? ` (${filled.length})` : ""}`}
          </Button>
          <p className="text-sm text-muted-foreground">
            Prikazano {rows.length} {matchWord(rows.length)}
            {isFiltered ? " (filtrirano)" : ""}.
            <span className="hidden md:inline"> Tab prelazi na sledeće polje, Enter čuva sve upisano.</span>
          </p>
        </div>
      </form>

      <MatchFormDialog open={formOpen} onOpenChange={setFormOpen} matchId={editedId} />
      <ForfeitDialog match={forfeitMatch} onClose={() => setForfeitMatch(undefined)} />
      <CorrectionDialog match={correctedMatch} onClose={() => setCorrectedMatch(undefined)} />
      <PostponeDialog match={postponedMatch} onClose={() => setPostponedMatch(undefined)} />
    </div>
  );
}

/**
 * Kratak zbir iznad liste: za jednog igrača njegov bilans, a za par igrača međusobni skor —
 * adminu je bitno da odmah vidi da li su njih dvojica već igrali i koliko puta.
 */
function HistorySummary({
  player,
  opponent,
  rows,
  className,
}: {
  player: SeasonPlayer;
  opponent?: SeasonPlayer;
  rows: MatchRow[];
  className?: string;
}) {
  const played = rows.filter((match) => hasResult(match.status));
  const wins = played.filter((match) => wonBy(match, player.id)).length;
  const upcoming = rows.filter((match) => !hasResult(match.status) && match.status !== "CANCELLED").length;
  const name = (p: SeasonPlayer) => p.nickname || p.fullName;

  return (
    <div className={cn("rounded-md bg-muted/60 px-3 py-2.5 text-sm", className)} role="status">
      {opponent ? (
        played.length === 0 ? (
          <p>
            <strong>{name(player)}</strong> i <strong>{name(opponent)}</strong> još nisu odigrali nijedan meč.
          </p>
        ) : (
          <p>
            <strong>{name(player)}</strong> i <strong>{name(opponent)}</strong> su odigrali{" "}
            <strong>
              {played.length} {matchWord(played.length)}
            </strong>
            : {name(player)} {wins} {winWord(wins)}, {name(opponent)} {played.length - wins}{" "}
            {winWord(played.length - wins)}.
          </p>
        )
      ) : (
        <p>
          <strong>{name(player)}</strong> ({player.groupName}): odigrano {played.length}{" "}
          {matchWord(played.length)} — {wins} {winWord(wins)}, {played.length - wins}{" "}
          {played.length - wins === 1 ? "poraz" : "poraza"}.
        </p>
      )}
      {upcoming > 0 ? (
        <p className="mt-0.5 text-muted-foreground">Zakazano još: {upcoming}.</p>
      ) : null}
    </div>
  );
}

/** Siroki ekran: jedna tabela, ceo red u jednoj liniji (masovni unos kola Tab-om). */
function MatchesGrid({ rows, actions }: { rows: MatchRow[]; actions: RowActions }) {
  return (
    <div className="rounded-lg border bg-background">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-14 text-right tabular-nums">Kolo</TableHead>
            <TableHead>Grupa</TableHead>
            <TableHead>Termin</TableHead>
            <TableHead>Teren</TableHead>
            <TableHead className="text-right">Igrač 1</TableHead>
            <TableHead className="w-40 text-center">Rezultat</TableHead>
            <TableHead>Igrač 2</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Radnje</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.map((match) => (
            <TableRow key={match.id}>
              <TableCell className="text-right tabular-nums">{match.roundNumber}</TableCell>
              <TableCell className="text-muted-foreground">{match.groupName ?? "—"}</TableCell>
              <TableCell className="whitespace-nowrap tabular-nums">{formatSlot(match.scheduledStart)}</TableCell>
              <TableCell className="text-muted-foreground">{match.courtName ?? "—"}</TableCell>
              <TableCell className="text-right">
                <PlayerName id={match.player1Id} name={match.player1Name} nicknames={actions.nicknames} />
              </TableCell>
              <TableCell className="text-center">
                {hasResult(match.status) ? (
                  <span className="font-semibold tabular-nums">
                    {match.scorePlayer1} : {match.scorePlayer2}
                  </span>
                ) : match.status === "CANCELLED" ? (
                  <span className="text-muted-foreground">—</span>
                ) : (
                  <div className="flex items-center justify-center gap-1">
                    <ScoreInput match={match} side="score1" actions={actions} className="h-8 w-14" />
                    <span className="text-muted-foreground">:</span>
                    <ScoreInput match={match} side="score2" actions={actions} className="h-8 w-14" />
                  </div>
                )}
              </TableCell>
              <TableCell>
                <PlayerName id={match.player2Id} name={match.player2Name} nicknames={actions.nicknames} />
              </TableCell>
              <TableCell>
                <StatusBadge status={match.status} />
              </TableCell>
              <TableCell className="text-right whitespace-nowrap">
                <MatchActions match={match} actions={actions} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

/** Telefon: svaki meč je kartica — igrači jedan ispod drugog, polja za rezultat uz ime. */
function MatchCard({ match, actions }: { match: MatchRow; actions: RowActions }) {
  const finished = hasResult(match.status);
  const meta = [
    `Kolo ${match.roundNumber}`,
    match.groupName,
    match.scheduledStart ? formatSlot(match.scheduledStart) : null,
    match.courtName,
  ].filter(Boolean);

  return (
    <li className="rounded-lg border bg-background p-3">
      <div className="flex items-start justify-between gap-2">
        <p className="text-xs text-muted-foreground">{meta.join(" · ")}</p>
        <StatusBadge status={match.status} />
      </div>

      <div className="mt-2 space-y-1.5">
        {(["score1", "score2"] as const).map((side) => {
          const name = side === "score1" ? match.player1Name : match.player2Name;
          const playerId = side === "score1" ? match.player1Id : match.player2Id;
          const score = side === "score1" ? match.scorePlayer1 : match.scorePlayer2;
          const other = side === "score1" ? match.scorePlayer2 : match.scorePlayer1;
          const won = finished && score != null && other != null && score > other;
          return (
            <div key={side} className="flex min-h-11 items-center gap-3">
              <PlayerName
                id={playerId}
                name={name}
                nicknames={actions.nicknames}
                strong={won}
                className="min-w-0 flex-1"
              />
              {finished ? (
                <span className={cn("w-14 text-center text-lg tabular-nums", won && "font-semibold")}>
                  {score}
                </span>
              ) : match.status === "CANCELLED" ? null : (
                <ScoreInput match={match} side={side} actions={actions} className="h-11 w-16" />
              )}
            </div>
          );
        })}
      </div>

      <div className="mt-2 flex flex-wrap gap-1 border-t pt-2">
        <MatchActions match={match} actions={actions} large />
      </div>
    </li>
  );
}

/** „Kočo" krupno, „Jovan Lazarević" sitno ispod — kad igrač nema nadimak, samo ime. */
function PlayerName({
  id,
  name,
  nicknames,
  strong,
  className,
}: {
  id?: number;
  name?: string;
  nicknames: Map<number, string>;
  strong?: boolean;
  className?: string;
}) {
  const nickname = id != null ? nicknames.get(id) : undefined;
  const weight = strong ? "font-semibold" : "font-medium";
  if (!nickname || nickname === name) {
    return <span className={cn("block truncate", weight, className)}>{name}</span>;
  }
  return (
    <span className={cn("block leading-tight", className)}>
      <span className={cn("block truncate", weight)}>{nickname}</span>
      <span className="block truncate text-xs text-muted-foreground">{name}</span>
    </span>
  );
}

function ScoreInput({
  match,
  side,
  actions,
  className,
}: {
  match: MatchRow;
  side: "score1" | "score2";
  actions: RowActions;
  className?: string;
}) {
  const id = match.id as number;
  return (
    <Input
      type="number"
      min={0}
      inputMode="numeric"
      enterKeyHint="next"
      aria-label={`Rezultat — ${side === "score1" ? match.player1Name : match.player2Name}`}
      className={cn("text-center tabular-nums", className)}
      value={actions.scores[id]?.[side] ?? ""}
      onChange={(e) => actions.setScore(id, side, e.target.value)}
    />
  );
}

function MatchActions({ match, actions, large }: { match: MatchRow; actions: RowActions; large?: boolean }) {
  const size = large ? "default" : "sm";
  const className = large ? "h-10 flex-1" : undefined;
  const variant = large ? "outline" : "ghost";

  if (hasResult(match.status)) {
    // Službeni rezultat sme da se zameni drugim (npr. kasnije se sazna da nisu došli obojica, D-23).
    const official = match.status !== "FINISHED";
    return (
      <>
        <Button type="button" variant={variant} size={size} className={className} onClick={() => actions.onCorrect(match)}>
          Ispravi
        </Button>
        {official ? (
          <Button type="button" variant={variant} size={size} className={className} onClick={() => actions.onForfeit(match)}>
            Službeni
          </Button>
        ) : null}
      </>
    );
  }
  return (
    <>
      <Button type="button" variant={variant} size={size} className={className} onClick={() => actions.onEdit(match)}>
        Izmeni
      </Button>
      {match.status === "CANCELLED" ? null : (
        <>
          <Button
            type="button"
            variant={variant}
            size={size}
            className={className}
            onClick={() => actions.onPostpone(match)}
          >
            Odloži
          </Button>
          <Button
            type="button"
            variant={variant}
            size={size}
            className={className}
            onClick={() => actions.onForfeit(match)}
          >
            Službeni
          </Button>
        </>
      )}
      <Button type="button" variant={variant} size={size} className={className} onClick={() => actions.onDelete(match)}>
        Obriši
      </Button>
    </>
  );
}

/** BR-10.1 — status se vidi na prvi pogled; boja nikad sama, uvek i tekst (NFR-11). */
function StatusBadge({ status }: { status?: MatchStatus }) {
  if (!status) {
    return null;
  }
  const variant =
    status === "FINISHED"
      ? "secondary"
      : status === "CANCELLED" || status === "POSTPONED"
        ? "outline"
        : status === "FORFEIT" || status === "WALKOVER" || status === "DOUBLE_FORFEIT"
          ? "destructive"
          : "outline";
  return <Badge variant={variant}>{STATUS_LABELS[status]}</Badge>;
}

function Message({ children, variant }: { children: React.ReactNode; variant?: "error" }) {
  return (
    <div
      className={cn(
        "rounded-lg border bg-background py-8 text-center text-sm",
        variant === "error" ? "text-destructive" : "text-muted-foreground",
      )}
      role={variant === "error" ? "alert" : undefined}
    >
      {children}
    </div>
  );
}

/**
 * Tabela ili kartice — crta se samo jedno, jer bi dvostruka polja za rezultat (jedna sakrivena)
 * na ~900 meceva sezone usporila telefon. Na serveru se pretpostavlja telefon.
 */
function useIsDesktop() {
  return useSyncExternalStore(
    (onChange) => {
      const query = window.matchMedia("(min-width: 768px)");
      query.addEventListener("change", onChange);
      return () => query.removeEventListener("change", onChange);
    },
    () => window.matchMedia("(min-width: 768px)").matches,
    () => false,
  );
}

function wonBy(match: MatchRow, playerId: number): boolean {
  const mine = match.player1Id === playerId ? match.scorePlayer1 : match.scorePlayer2;
  const theirs = match.player1Id === playerId ? match.scorePlayer2 : match.scorePlayer1;
  return mine != null && theirs != null && mine > theirs;
}

/** 1 meč, 2–4 meča, 5+ mečeva (11–14 mečeva). */
function matchWord(n: number): string {
  const last = n % 10;
  const lastTwo = n % 100;
  if (last === 1 && lastTwo !== 11) return "meč";
  if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) return "meča";
  return "mečeva";
}

function winWord(n: number): string {
  const last = n % 10;
  const lastTwo = n % 100;
  if (last === 1 && lastTwo !== 11) return "pobeda";
  if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) return "pobede";
  return "pobeda";
}

