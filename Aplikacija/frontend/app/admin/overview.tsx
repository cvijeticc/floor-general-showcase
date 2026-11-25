"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { api, errorMessage } from "@/lib/api/client";

/** Prvi poziv backenda iz browsera — ujedno provera da prijava i proxy rade od kraja do kraja. */
export function AdminOverview() {
  const players = useQuery({
    queryKey: ["players"],
    queryFn: async () => {
      const { data, error } = await api.GET("/api/v1/players", {});
      if (error) {
        throw error;
      }
      return data ?? [];
    },
  });

  const seasons = useQuery({
    queryKey: ["seasons"],
    queryFn: async () => {
      const { data, error } = await api.GET("/api/v1/seasons", {});
      if (error) {
        throw error;
      }
      return data ?? [];
    },
  });

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <SummaryCard
        title="Igrači"
        href="/admin/igraci"
        description="Šifarnik igrača lige"
        isLoading={players.isLoading}
        error={players.error}
        value={players.data?.length}
        emptyLabel="Još nema unetih igrača"
      />
      <SummaryCard
        title="Sezone"
        href="/admin/sezone"
        description="Sezone sa parametrima bodovanja"
        isLoading={seasons.isLoading}
        error={seasons.error}
        value={seasons.data?.length}
        emptyLabel="Još nema napravljenih sezona"
      />
    </div>
  );
}

type SummaryCardProps = {
  title: string;
  href: string;
  description: string;
  isLoading: boolean;
  error: unknown;
  value: number | undefined;
  emptyLabel: string;
};

/** Sva cetiri stanja: ucitavanje, greska, prazno i uspeh — nijedan ekran ne sme da „cuti". */
function SummaryCard({
  title,
  href,
  description,
  isLoading,
  error,
  value,
  emptyLabel,
}: SummaryCardProps) {
  // Cela kartica je link na svoj ekran -- isto kao klik na tab u zaglavlju.
  return (
    <Link
      href={href}
      className="group block rounded-xl outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <Card className="transition-[box-shadow,transform] duration-200 ease-(--ease-out-strong) group-hover:shadow-md group-active:scale-[0.99]">
        <CardHeader>
          <CardTitle>{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <p className="text-sm text-muted-foreground">Učitavanje…</p>
          ) : null}

          {error ? (
            <p role="alert" className="text-sm text-destructive">
              {errorMessage(error, "Podaci nisu učitani")}
            </p>
          ) : null}

          {!isLoading && !error ? (
            value === 0 ? (
              <p className="text-sm text-muted-foreground">{emptyLabel}</p>
            ) : (
              <p className="text-3xl font-semibold tabular-nums">{value}</p>
            )
          ) : null}
        </CardContent>
      </Card>
    </Link>
  );
}
