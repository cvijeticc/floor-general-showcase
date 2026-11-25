"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { api, errorMessage, unwrap } from "@/lib/api/client";
import { useGroupPlayers } from "@/lib/api/queries";
import type { components } from "@/lib/api/schema";
import { STATUS_LABELS } from "../../igraci/types";

type GroupInfo = components["schemas"]["GroupInfo"];

export function GroupDetail({ groupId }: { groupId: number }) {
  const group = useQuery({
    queryKey: ["group", groupId],
    enabled: Number.isFinite(groupId),
    queryFn: () =>
      unwrap<GroupInfo>(api.GET("/api/v1/groups/{id}", { params: { path: { id: groupId } } })),
  });
  const players = useGroupPlayers(Number.isFinite(groupId) ? groupId : "");

  if (group.isLoading) {
    return <p className="text-sm text-muted-foreground">Učitavanje…</p>;
  }
  // Pogresan ID u adresi ne vodi na 404 nego na poruku sa povratkom na spisak.
  if (group.error || !group.data) {
    return (
      <div className="space-y-3">
        <p role="alert" className="text-sm text-destructive">
          {errorMessage(group.error, "Grupa nije pronađena")}
        </p>
        <Button asChild variant="outline">
          <Link href="/admin/grupe">← Nazad na grupe</Link>
        </Button>
      </div>
    );
  }

  const g = group.data;
  const rows = [...(players.data ?? [])].sort((a, b) =>
    (a.lastName ?? "").localeCompare(b.lastName ?? "", "sr"),
  );

  return (
    <div className="space-y-6">
      <div>
        <Link
          href={`/admin/grupe${g.seasonId != null ? `?sezona=${g.seasonId}` : ""}`}
          className="text-sm text-muted-foreground hover:text-foreground"
        >
          ← Grupe
        </Link>
        <h1 className="text-2xl font-semibold tracking-tight">{g.name}</h1>
        <p className="text-sm text-muted-foreground">
          {g.seasonName} · {g.stageName}
        </p>
      </div>

      {players.isLoading ? <p className="text-sm text-muted-foreground">Učitavanje…</p> : null}
      {players.error ? (
        <p role="alert" className="text-sm text-destructive">
          {errorMessage(players.error, "Igrači nisu učitani")}
        </p>
      ) : null}
      {players.data && rows.length === 0 ? (
        <p className="text-sm text-muted-foreground">U ovoj grupi još nema igrača.</p>
      ) : null}

      {rows.length > 0 ? (
        <div className="rounded-lg border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-12">#</TableHead>
                <TableHead>Igrač</TableHead>
                <TableHead>Nadimak</TableHead>
                <TableHead>Godište</TableHead>
                <TableHead>Telefon</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((player, index) => (
                <TableRow key={player.id}>
                  <TableCell className="tabular-nums text-muted-foreground">{index + 1}</TableCell>
                  <TableCell className="font-medium">{player.fullName}</TableCell>
                  <TableCell>{player.nickname}</TableCell>
                  <TableCell className="tabular-nums">{player.birthYear ?? "—"}</TableCell>
                  <TableCell className="tabular-nums">{player.phone ?? "—"}</TableCell>
                  <TableCell>
                    <Badge variant={player.status === "ACTIVE" ? "secondary" : "destructive"}>
                      {player.status ? STATUS_LABELS[player.status] : "—"}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      ) : null}
    </div>
  );
}
