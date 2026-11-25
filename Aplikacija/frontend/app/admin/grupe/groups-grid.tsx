"use client";

import Link from "next/link";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { api, errorMessage, unwrap } from "@/lib/api/client";
import type { GroupResult } from "@/lib/api/queries";
import { SeasonLink } from "../sezone/season-link";
import { useSeasons } from "../sezone/use-seasons";

export function GroupsGrid({ initialSeasonId }: { initialSeasonId?: number }) {
  const seasons = useSeasons();
  const [pickedSeasonId, setPickedSeasonId] = useState<number | undefined>(initialSeasonId);

  const seasonList = seasons.data ?? [];
  // Bez izbora vodi se najnovija sezona (poslednja napravljena), kao i na ostalim ekranima.
  const seasonId =
    seasonList.find((season) => season.id === pickedSeasonId)?.id ?? seasonList.at(-1)?.id;

  const groups = useQuery({
    queryKey: ["season-groups", seasonId],
    enabled: seasonId != null,
    queryFn: () =>
      unwrap<GroupResult[]>(
        api.GET("/api/v1/seasons/{seasonId}/groups", {
          params: { path: { seasonId: seasonId as number } },
        }),
      ),
  });

  // „Grupa 10" ide posle „Grupe 9", ne posle „Grupe 1".
  const sorted = [...(groups.data ?? [])].sort((a, b) =>
    (a.name ?? "").localeCompare(b.name ?? "", "sr", { numeric: true }),
  );

  if (seasons.isLoading) {
    return <p className="text-sm text-muted-foreground">Učitavanje…</p>;
  }
  if (seasons.error) {
    return (
      <p role="alert" className="text-sm text-destructive">
        {errorMessage(seasons.error, "Sezone nisu učitane")}
      </p>
    );
  }
  if (seasonList.length === 0) {
    return <p className="text-sm text-muted-foreground">Još nema napravljenih sezona.</p>;
  }

  return (
    <div className="space-y-4">
      {seasonList.length > 1 ? (
        <div className="flex flex-wrap gap-2" role="tablist" aria-label="Sezona">
          {[...seasonList].reverse().map((season) => (
            <Button
              key={season.id}
              role="tab"
              aria-selected={season.id === seasonId}
              variant={season.id === seasonId ? "default" : "outline"}
              size="sm"
              onClick={() => setPickedSeasonId(season.id)}
            >
              {season.name}
            </Button>
          ))}
        </div>
      ) : (
        <SeasonLink />
      )}

      {groups.isLoading ? <p className="text-sm text-muted-foreground">Učitavanje…</p> : null}
      {groups.error ? (
        <p role="alert" className="text-sm text-destructive">
          {errorMessage(groups.error, "Grupe nisu učitane")}
        </p>
      ) : null}
      {groups.data && sorted.length === 0 ? (
        <p className="text-sm text-muted-foreground">Ova sezona još nema grupa.</p>
      ) : null}

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {sorted.map((group) => (
          <Link
            key={group.id}
            href={`/admin/grupe/${group.id}`}
            className="flex aspect-square flex-col items-center justify-center gap-2 rounded-xl border bg-background p-4 text-center shadow-xs transition-colors hover:border-primary hover:bg-primary/5"
          >
            <span className="text-2xl font-semibold tracking-tight">{group.name}</span>
            <span className="text-sm text-muted-foreground tabular-nums">
              {group.playerCount}
              {group.groupSize ? ` / ${group.groupSize}` : ""} igrača
            </span>
          </Link>
        ))}
      </div>
    </div>
  );
}
