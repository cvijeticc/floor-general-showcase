"use client";

import { useState } from "react";
import { toast } from "sonner";
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
import { cn } from "cn";
import { PlayerFormDialog } from "./player-form";
import { WithdrawDialog } from "./status-dialog";
import { STATUS_LABELS, STATUS_ORDER, type PlayerResult, type PlayerStatus } from "./types";
import {
  useChangePlayerStatus,
  useDeletePlayer,
  usePlayerGroupNames,
  usePlayers,
  useSeasonGroups,
  type PlayerFilters,
} from "./use-players";

const EMPTY_FILTERS: PlayerFilters = { search: "", status: "", groupId: "" };

export function PlayersTable() {
  const [filters, setFilters] = useState<PlayerFilters>(EMPTY_FILTERS);
  const [formOpen, setFormOpen] = useState(false);
  const [editedId, setEditedId] = useState<number | undefined>(undefined);
  const [withdrawing, setWithdrawing] = useState<PlayerResult | undefined>(undefined);

  const players = usePlayers(filters);
  const { groups } = useSeasonGroups();
  const groupNames = usePlayerGroupNames(groups);

  const changeStatus = useChangePlayerStatus();
  const deletePlayer = useDeletePlayer();

  const rows = players.data ?? [];
  const isFiltered = filters.search !== "" || filters.status !== "" || filters.groupId !== "";

  function openNew() {
    setEditedId(undefined);
    setFormOpen(true);
  }

  function openEdit(id: number) {
    setEditedId(id);
    setFormOpen(true);
  }

  async function onChangeStatus(player: PlayerResult, status: PlayerStatus) {
    if (player.id == null) {
      return;
    }
    try {
      await changeStatus.mutateAsync({ id: player.id, status });
      toast.success((player.nickname || player.fullName) + ": " + STATUS_LABELS[status].toLowerCase());
    } catch (error) {
      toast.error(errorMessage(error, "Status nije promenjen"));
    }
  }

  async function onDelete(player: PlayerResult) {
    if (player.id == null) {
      return;
    }
    try {
      await deletePlayer.mutateAsync(player.id);
      toast.success("Igrač " + player.fullName + " je obrisan");
    } catch (error) {
      // Backend odbija brisanje igraca koji ima meceve ili je u grupi — poruka je vec na srpskom.
      toast.error(errorMessage(error, "Igrač nije obrisan"));
    }
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3 md:flex md:flex-wrap md:items-end">
        <div className="col-span-2 space-y-1.5 md:w-64">
          <Label htmlFor="pretraga">Pretraga</Label>
          <Input
            id="pretraga"
            type="search"
            autoComplete="off"
            autoCorrect="off"
            autoCapitalize="none"
            spellCheck={false}
            className="h-11 md:h-9"
            placeholder="Ime, prezime ili nadimak…"
            value={filters.search}
            onChange={(e) => setFilters((f) => ({ ...f, search: e.target.value }))}
          />
        </div>

        <div className="space-y-1.5 md:w-40">
          <Label htmlFor="grupa">Grupa</Label>
          <select
            id="grupa"
            className="h-11 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-xs md:h-9 md:text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
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

        <div className="space-y-1.5 md:w-44">
          <Label htmlFor="status">Status</Label>
          <select
            id="status"
            className="h-11 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-xs md:h-9 md:text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
            value={filters.status}
            onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value as PlayerStatus | "" }))}
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
          <Button variant="outline" className="h-11 md:h-9" onClick={() => setFilters(EMPTY_FILTERS)}>
            Poništi filtere
          </Button>
        ) : null}

        <Button className={cn("h-11 md:ml-auto md:h-9", !isFiltered && "col-span-2")} onClick={openNew}>
          Novi igrač
        </Button>
      </div>

      {/* Telefon: kartice sa velikim dugmićima; široki ekran: tabela. */}
      <ul className="space-y-3 md:hidden">
        {players.isLoading ? (
          <MobileMessage>Učitavanje…</MobileMessage>
        ) : players.error ? (
          <MobileMessage>{errorMessage(players.error, "Lista igrača nije učitana")}</MobileMessage>
        ) : rows.length === 0 ? (
          <MobileMessage>{isFiltered ? "Nijedan igrač ne odgovara filterima" : "Još nema unetih igrača"}</MobileMessage>
        ) : (
          rows.map((player) => (
            <li key={player.id} className="rounded-lg border bg-background p-3">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="truncate font-semibold">{player.nickname}</p>
                  <p className="truncate text-sm text-muted-foreground">
                    {player.fullName}
                    {player.id != null && groupNames.get(player.id) ? " · " + groupNames.get(player.id) : ""}
                  </p>
                </div>
                <StatusBadge status={player.status} />
              </div>
              {player.phone ? (
                <a href={"tel:" + player.phone} className="mt-1 inline-block text-sm tabular-nums underline-offset-2 active:underline">
                  {player.phone}
                </a>
              ) : null}
              <div className="mt-2 flex gap-1 border-t pt-2">
                <Button variant="outline" className="h-10 flex-1" onClick={() => player.id != null && openEdit(player.id)}>
                  Izmeni
                </Button>
                {player.status === "ACTIVE" ? (
                  <Button variant="outline" className="h-10 flex-1" onClick={() => setWithdrawing(player)}>
                    Odustao
                  </Button>
                ) : (
                  <Button variant="outline" className="h-10 flex-1" onClick={() => onChangeStatus(player, "ACTIVE")}>
                    Vrati
                  </Button>
                )}
                <Button variant="outline" className="h-10 flex-1" onClick={() => onDelete(player)}>
                  Obriši
                </Button>
              </div>
            </li>
          ))
        )}
      </ul>

      <div className="hidden rounded-lg border bg-background md:block">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10 text-right tabular-nums">#</TableHead>
              <TableHead>Nadimak</TableHead>
              <TableHead>Ime i prezime</TableHead>
              <TableHead>Grupa</TableHead>
              <TableHead className="text-right">Godište</TableHead>
              <TableHead className="text-right">Visina</TableHead>
              <TableHead className="text-right">Težina</TableHead>
              <TableHead>Telefon</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Radnje</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {players.isLoading ? (
              <MessageRow>Učitavanje…</MessageRow>
            ) : players.error ? (
              <MessageRow variant="error">
                {errorMessage(players.error, "Lista igrača nije učitana")}
              </MessageRow>
            ) : rows.length === 0 ? (
              <MessageRow>
                {isFiltered ? "Nijedan igrač ne odgovara filterima" : "Još nema unetih igrača"}
              </MessageRow>
            ) : (
              rows.map((player, index) => (
                <TableRow key={player.id}>
                  <TableCell className="text-right text-muted-foreground tabular-nums">
                    {index + 1}
                  </TableCell>
                  <TableCell className="font-medium">{player.nickname}</TableCell>
                  <TableCell>{player.fullName}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {player.id != null ? (groupNames.get(player.id) ?? "—") : "—"}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{player.birthYear ?? "—"}</TableCell>
                  <TableCell className="text-right tabular-nums">
                    {player.heightCm != null ? player.heightCm + " cm" : "—"}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">
                    {player.weightKg != null ? player.weightKg + " kg" : "—"}
                  </TableCell>
                  <TableCell className="tabular-nums">{player.phone ?? "—"}</TableCell>
                  <TableCell>
                    <StatusBadge status={player.status} />
                  </TableCell>
                  <TableCell className="text-right whitespace-nowrap">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => player.id != null && openEdit(player.id)}
                    >
                      Izmeni
                    </Button>
                    {player.status === "ACTIVE" ? (
                      <Button variant="ghost" size="sm" onClick={() => setWithdrawing(player)}>
                        Odustao
                      </Button>
                    ) : (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => onChangeStatus(player, "ACTIVE")}
                      >
                        Vrati
                      </Button>
                    )}
                    <Button variant="ghost" size="sm" onClick={() => onDelete(player)}>
                      Obriši
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <p className="text-sm text-muted-foreground">
        Prikazano {rows.length} {rows.length === 1 ? "igrač" : "igrača"}
        {isFiltered ? " (filtrirano)" : ""}.
      </p>

      <PlayerFormDialog open={formOpen} onOpenChange={setFormOpen} playerId={editedId} />
      <WithdrawDialog player={withdrawing} onClose={() => setWithdrawing(undefined)} />
    </div>
  );
}

/** BR-10 — status se vidi na prvi pogled, bez citanja teksta u redu. */
function StatusBadge({ status }: { status: PlayerResult["status"] }) {
  if (!status) {
    return null;
  }
  const variant =
    status === "ACTIVE" ? "secondary" : status === "WITHDRAWN" ? "outline" : "destructive";
  return <Badge variant={variant}>{STATUS_LABELS[status]}</Badge>;
}

function MessageRow({
  children,
  variant,
}: {
  children: React.ReactNode;
  variant?: "error";
}) {
  return (
    <TableRow>
      <TableCell
        colSpan={10}
        className={
          "py-8 text-center text-sm " +
          (variant === "error" ? "text-destructive" : "text-muted-foreground")
        }
        role={variant === "error" ? "alert" : undefined}
      >
        {children}
      </TableCell>
    </TableRow>
  );
}

function MobileMessage({ children }: { children: React.ReactNode }) {
  return <li className="rounded-lg border bg-background py-8 text-center text-sm text-muted-foreground">{children}</li>;
}
