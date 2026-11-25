"use client";

import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { errorMessage } from "@/lib/api/client";
import { useStage } from "../../tabele/use-standings";
import {
  SEASON_STATUS_LABEL,
  STAGE_TYPE_LABEL,
  type StageResult,
  formatDate,
  useSeason,
  useSeasonStages,
} from "../use-seasons";

export function SeasonDetail({ seasonId }: { seasonId: number }) {
  const season = useSeason(seasonId);
  const stages = useSeasonStages(seasonId);

  if (season.isLoading) {
    return <p className="text-sm text-muted-foreground">Učitavanje…</p>;
  }
  // Pogresan ID u adresi ne vodi na 404 nego na poruku sa povratkom na spisak.
  if (season.error || !season.data) {
    return (
      <div className="space-y-3">
        <p role="alert" className="text-sm text-destructive">
          {errorMessage(season.error, "Sezona nije pronađena")}
        </p>
        <Button asChild variant="outline">
          <Link href="/admin/sezone">← Nazad na sezone</Link>
        </Button>
      </div>
    );
  }

  const s = season.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <Link href="/admin/sezone" className="text-sm text-muted-foreground hover:text-foreground">
            ← Sezone
          </Link>
          <h1 className="flex items-center gap-3 text-2xl font-semibold tracking-tight">
            Sezona {s.name}
            {s.status ? (
              <Badge variant={s.status === "ACTIVE" ? "default" : "secondary"}>
                {SEASON_STATUS_LABEL[s.status] ?? s.status}
              </Badge>
            ) : null}
          </h1>
          <p className="text-sm text-muted-foreground">
            {s.leagueName} · {formatDate(s.startDate)} – {formatDate(s.endDate)} · {s.playerCount}{" "}
            igrača
          </p>
        </div>
        <Button asChild>
          <Link href={`/admin/grupe?sezona=${s.id}`}>Grupe ove sezone →</Link>
        </Button>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Bodovanje (BR-01)</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-2 gap-y-1 text-sm">
              <dt className="text-muted-foreground">Pobeda</dt>
              <dd>{s.pointsForWin}</dd>
              <dt className="text-muted-foreground">Poraz</dt>
              <dd>{s.pointsForLoss}</dd>
              <dt className="text-muted-foreground">Predaja / nedolazak</dt>
              <dd>{s.pointsForForfeit}</dd>
              <dt className="text-muted-foreground">Službeni rezultat</dt>
              <dd>
                {s.forfeitWinnerScore}:{s.forfeitLoserScore}
              </dd>
            </dl>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Format meča (D-18)</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-2 gap-y-1 text-sm">
              <dt className="text-muted-foreground">Trajanje</dt>
              <dd>{s.durationMinutes} min</dd>
              <dt className="text-muted-foreground">Rani kraj posle</dt>
              <dd>{s.targetScore} poena</dd>
              <dt className="text-muted-foreground">Najveći rezultat</dt>
              <dd>{s.maxScore} poena</dd>
            </dl>
          </CardContent>
        </Card>
      </div>

      <div className="space-y-3">
        <h2 className="text-lg font-semibold">Faze</h2>
        {stages.isLoading ? <p className="text-sm text-muted-foreground">Učitavanje…</p> : null}
        {stages.error ? (
          <p role="alert" className="text-sm text-destructive">
            {errorMessage(stages.error, "Faze nisu učitane")}
          </p>
        ) : null}
        {stages.data?.length === 0 ? (
          <p className="text-sm text-muted-foreground">Sezona još nema faza.</p>
        ) : null}
        <div className="grid gap-4 md:grid-cols-2">
          {stages.data?.map((stage) => <StageCard key={stage.id} stage={stage} />)}
        </div>
      </div>
    </div>
  );
}

/** Parametri formata su na fazi (D-05) — prikazuju se onako kako su uneti, nista se ne racuna. */
function StageCard({ stage }: { stage: StageResult }) {
  const info = useStage(stage.id);
  const d = info.data;

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-2">
          <CardTitle>
            {stage.orderNo}. {stage.name}
          </CardTitle>
          <Badge variant="outline">{STAGE_TYPE_LABEL[stage.type ?? ""] ?? stage.type}</Badge>
        </div>
      </CardHeader>
      <CardContent className="text-sm">
        {info.isLoading ? <p className="text-muted-foreground">Učitavanje…</p> : null}
        {d && stage.type === "GROUP" ? (
          <dl className="grid grid-cols-2 gap-y-1">
            <dt className="text-muted-foreground">Grupe</dt>
            <dd>
              {d.groupCount} × {d.groupSize} igrača
            </dd>
            <dt className="text-muted-foreground">Krugova</dt>
            <dd>{d.roundCount}</dd>
            <dt className="text-muted-foreground">Prolazi direktno</dt>
            <dd>prvih {d.directAdvance} iz grupe</dd>
            <dt className="text-muted-foreground">Dodatno prolazi</dt>
            <dd>{d.additionalAdvance}</dd>
          </dl>
        ) : null}
        {d && stage.type === "KNOCKOUT" ? <p>Drvo od {d.bracketSize} igrača</p> : null}
      </CardContent>
    </Card>
  );
}
