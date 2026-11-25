"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import { useSeasonGroups } from "@/lib/api/queries";
import { useAllStandings, useRecomputeStandings, useStage, useStandings } from "./use-standings";

const SELECT_CLASS =
  "h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50";

/** Vrednost u padajucoj listi za zbirnu tabelu svih igraca sezone. */
const ALL = "all";

type Selection = number | typeof ALL | "";

export function StandingsTable() {
  const { groups } = useSeasonGroups();
  const [selection, setSelection] = useState<Selection>("");

  // Prva grupa se bira sama, da ekran ne docekuje praznim izborom.
  useEffect(() => {
    if (selection === "" && groups.length > 0 && groups[0].id != null) {
      setSelection(groups[0].id);
    }
  }, [groups, selection]);

  const groupId = typeof selection === "number" ? selection : "";
  const showAll = selection === ALL;

  const selectedGroup = groups.find((group) => group.id === groupId);
  const standings = useStandings(groupId);
  const allStandings = useAllStandings(groups, showAll);
  const stage = useStage(selectedGroup?.stageId ?? groups[0]?.stageId);
  const recompute = useRecomputeStandings();

  const directAdvance = stage.data?.directAdvance ?? 0;
  const groupRows = standings.data ?? [];
  const played = groupRows.reduce((sum, row) => sum + (row.played ?? 0), 0) / 2;
  const allPlayed = (allStandings.data ?? []).reduce((sum, row) => sum + (row.played ?? 0), 0) / 2;

  async function onRecompute() {
    if (groupId === "") {
      return;
    }
    try {
      await recompute.mutateAsync(groupId);
      toast.success("Tabela je preračunata");
    } catch (error) {
      toast.error(errorMessage(error, "Preračun nije uspeo"));
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="grupa">Grupa</Label>
          <select
            id="grupa"
            className={SELECT_CLASS + " w-44"}
            value={selection}
            onChange={(e) => {
              const value = e.target.value;
              setSelection(value === "" ? "" : value === ALL ? ALL : Number(value));
            }}
          >
            <option value={ALL}>Svi igrači</option>
            {groups.map((group) => (
              <option key={group.id} value={group.id}>
                {group.name}
              </option>
            ))}
          </select>
        </div>

        {showAll ? null : (
          <Button variant="outline" onClick={onRecompute} disabled={recompute.isPending || groupId === ""}>
            {recompute.isPending ? "Računanje…" : "Preračunaj"}
          </Button>
        )}

        <p className="text-sm text-muted-foreground">
          {showAll
            ? allPlayed > 0
              ? `Odigrano ${allPlayed} ${allPlayed === 1 ? "meč" : "mečeva"} u svim grupama.`
              : "Još nema odigranih mečeva."
            : played > 0
              ? `Odigrano ${played} ${played === 1 ? "meč" : "mečeva"} u ovoj grupi.`
              : "U ovoj grupi još nema odigranih mečeva."}
        </p>
      </div>

      <div className="rounded-lg border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-12 text-right">Poz.</TableHead>
              <TableHead>Igrač</TableHead>
              {showAll ? <TableHead>Grupa</TableHead> : null}
              <TableHead className="text-right" title="Odigrano mečeva">
                Odigrano
              </TableHead>
              <TableHead className="text-right" title="Pobede">
                P
              </TableHead>
              <TableHead className="text-right" title="Porazi">
                I
              </TableHead>
              <TableHead className="text-right" title="Dati poeni">
                DP
              </TableHead>
              <TableHead className="text-right" title="Primljeni poeni">
                PP
              </TableHead>
              <TableHead className="text-right" title="Koš-razlika">
                KR
              </TableHead>
              <TableHead className="text-right">Bodovi</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {showAll ? (
              allStandings.isLoading ? (
                <MessageRow>Učitavanje…</MessageRow>
              ) : allStandings.error ? (
                <MessageRow variant="error">
                  {errorMessage(allStandings.error, "Tabela nije učitana")}
                </MessageRow>
              ) : (allStandings.data ?? []).length === 0 ? (
                <MessageRow>Još nema igrača u grupama</MessageRow>
              ) : (
                (allStandings.data ?? []).map((row, index) => (
                  <StandingRow
                    key={row.playerId}
                    row={row}
                    position={index + 1}
                    groupName={row.groupName}
                    // Zbirno se oznacava direktan prolaz iz sopstvene grupe (mesto u grupi).
                    advances={directAdvance > 0 && (row.position ?? 0) <= directAdvance}
                  />
                ))
              )
            ) : standings.isLoading ? (
              <MessageRow>Učitavanje…</MessageRow>
            ) : standings.error ? (
              <MessageRow variant="error">
                {errorMessage(standings.error, "Tabela nije učitana")}
              </MessageRow>
            ) : groupRows.length === 0 ? (
              <MessageRow>U ovoj grupi još nema igrača</MessageRow>
            ) : (
              groupRows.map((row) => (
                <StandingRow
                  key={row.playerId}
                  row={row}
                  position={row.position ?? 0}
                  advances={directAdvance > 0 && (row.position ?? 0) <= directAdvance}
                />
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <p className="text-sm text-muted-foreground">
        Bodovanje: pobeda 2 · poraz 1 · predaja i nedolazak 0 (BR-01).
        {directAdvance > 0
          ? ` Prvih ${directAdvance} iz grupe prolazi direktno dalje; ostala mesta se popunjavaju
             poređenjem preko grupa, što se računa na kraju grupne faze.`
          : ""}
      </p>

      {showAll ? (
        <p className="text-sm text-muted-foreground">
          Zbirna tabela je <strong>informativna</strong>: svi igrači sezone poređani po{" "}
          <strong>bodovi → koš-razlika → dati poeni</strong>. Igrači nemaju isti broj odigranih
          mečeva, pa redosled nije konačan. Zvanično rangiranje preko grupa (BR-04) i lista 64 rade
          se pred playoff.
        </p>
      ) : (
        <p className="text-sm text-muted-foreground">
          Redosled je za sada <strong>bodovi → koš-razlika → dati poeni</strong>. Pun lanac
          tie-breaka po BR-03 (međusobni duel, mini-tabela) radi se pred playoff.
        </p>
      )}
    </div>
  );
}

type StandingRowProps = {
  row: {
    playerName?: string;
    played?: number;
    wins?: number;
    losses?: number;
    pointsFor?: number;
    pointsAgainst?: number;
    pointDifference?: number;
    points?: number;
  };
  position: number;
  groupName?: string;
  advances: boolean;
};

function StandingRow({ row, position, groupName, advances }: StandingRowProps) {
  return (
    <TableRow>
      <TableCell className="text-right tabular-nums text-muted-foreground">{position}.</TableCell>
      <TableCell className="font-medium">{row.playerName}</TableCell>
      {groupName !== undefined ? (
        <TableCell className="text-muted-foreground">{groupName}</TableCell>
      ) : null}
      <TableCell className="text-right tabular-nums">{row.played}</TableCell>
      <TableCell className="text-right tabular-nums">{row.wins}</TableCell>
      <TableCell className="text-right tabular-nums">{row.losses}</TableCell>
      <TableCell className="text-right tabular-nums">{row.pointsFor}</TableCell>
      <TableCell className="text-right tabular-nums">{row.pointsAgainst}</TableCell>
      <TableCell className="text-right tabular-nums">
        {(row.pointDifference ?? 0) > 0 ? "+" : ""}
        {row.pointDifference}
      </TableCell>
      <TableCell className="text-right font-semibold tabular-nums">{row.points}</TableCell>
      <TableCell className="text-right">
        {/* NFR-11 — oznaka nosi tekst, ne samo boju. */}
        {advances ? <Badge variant="secondary">prolaz</Badge> : null}
      </TableCell>
    </TableRow>
  );
}

function MessageRow({ children, variant }: { children: React.ReactNode; variant?: "error" }) {
  return (
    <TableRow>
      <TableCell
        colSpan={11}
        className={
          "py-8 text-center text-sm " + (variant === "error" ? "text-destructive" : "text-muted-foreground")
        }
        role={variant === "error" ? "alert" : undefined}
      >
        {children}
      </TableCell>
    </TableRow>
  );
}
