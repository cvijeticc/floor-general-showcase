"use client";

import { useQuery } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

export type SeasonResult = components["schemas"]["SeasonResult"];
export type SeasonInfo = components["schemas"]["SeasonInfo"];
export type StageResult = components["schemas"]["StageResult"];

/** Sve sezone lige — za sada samo 2026/2027, ali ekran je pravljen za vise njih. */
export function useSeasons() {
  return useQuery({
    queryKey: ["seasons"],
    queryFn: () => unwrap<SeasonResult[]>(api.GET("/api/v1/seasons", {})),
  });
}

export function useSeason(seasonId: number) {
  return useQuery({
    queryKey: ["season", seasonId],
    enabled: Number.isFinite(seasonId),
    queryFn: () =>
      unwrap<SeasonInfo>(api.GET("/api/v1/seasons/{id}", { params: { path: { id: seasonId } } })),
  });
}

/** Faze sezone, poredjane onako kako se igraju (`orderNo`). */
export function useSeasonStages(seasonId: number) {
  return useQuery({
    queryKey: ["season-stages", seasonId],
    enabled: Number.isFinite(seasonId),
    queryFn: async () => {
      const stages = await unwrap<StageResult[]>(
        api.GET("/api/v1/seasons/{seasonId}/stages", { params: { path: { seasonId } } }),
      );
      return [...stages].sort((a, b) => (a.orderNo ?? 0) - (b.orderNo ?? 0));
    },
  });
}

export const SEASON_STATUS_LABEL: Record<string, string> = {
  DRAFT: "U pripremi",
  ACTIVE: "U toku",
  FINISHED: "Završena",
};

export const STAGE_TYPE_LABEL: Record<string, string> = {
  GROUP: "Grupna faza",
  KNOCKOUT: "Playoff",
};

/** Datum sa backenda (yyyy-MM-dd) u formatu lige dd.MM.yyyy (CLAUDE.md §4). */
export function formatDate(value?: string) {
  if (!value) {
    return "—";
  }
  const [year, month, day] = value.split("-");
  return `${day}.${month}.${year}.`;
}
