"use client";

import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { errorMessage } from "@/lib/api/client";
import { SEASON_STATUS_LABEL, formatDate, useSeasons } from "./use-seasons";

export function SeasonsList() {
  const seasons = useSeasons();

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

  // Najnovija sezona ide prva.
  const rows = [...(seasons.data ?? [])].reverse();
  if (rows.length === 0) {
    return <p className="text-sm text-muted-foreground">Još nema napravljenih sezona.</p>;
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {rows.map((season) => (
        <Link key={season.id} href={`/admin/sezone/${season.id}`} className="group">
          <Card className="h-full transition-colors group-hover:border-primary">
            <CardHeader>
              <div className="flex items-start justify-between gap-2">
                <CardTitle className="text-xl">{season.name}</CardTitle>
                {season.status ? (
                  <Badge variant={season.status === "ACTIVE" ? "default" : "secondary"}>
                    {SEASON_STATUS_LABEL[season.status] ?? season.status}
                  </Badge>
                ) : null}
              </div>
              <CardDescription>{season.leagueName}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-1 text-sm text-muted-foreground">
              <p>
                {formatDate(season.startDate)} – {formatDate(season.endDate)}
              </p>
              <p>{season.playerCount} igrača</p>
            </CardContent>
          </Card>
        </Link>
      ))}
    </div>
  );
}
