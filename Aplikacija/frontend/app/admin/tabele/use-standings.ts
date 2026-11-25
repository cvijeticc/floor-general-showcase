"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

export type StandingResult = components["schemas"]["StandingResult"];
export type StageInfo = components["schemas"]["StageInfo"];

/** Materijalizovana tabela grupe (FR-6.5.1) — cita se, ne racuna se u letu. */
export function useStandings(groupId: number | "") {
  return useQuery({
    queryKey: ["standings", groupId],
    enabled: groupId !== "",
    queryFn: () =>
      unwrap<StandingResult[]>(
        api.GET("/api/v1/groups/{groupId}/standings", {
          params: { path: { groupId: groupId as number } },
        }),
      ),
  });
}

export type OverallStandingRow = StandingResult & { groupName?: string };

/**
 * Zbirna tabela svih igraca sezone — tabele svih grupa spojene u jednu i poredjane po
 * bodovi → koš-razlika → dati poeni. Informativna je; zvanicno rangiranje preko grupa je
 * BR-04 i radi se u Roku 3 (engine/).
 */
export function useAllStandings(groups: { id?: number; name?: string }[], enabled: boolean) {
  const groupKey = groups.map((group) => group.id).join(",");
  return useQuery({
    // Pocinje sa "standings", pa ga osvezava isto sto i tabelu jedne grupe (unos, preracun).
    queryKey: ["standings", "all", groupKey],
    enabled: enabled && groups.length > 0,
    queryFn: async () => {
      const perGroup = await Promise.all(
        groups.map(async (group) => {
          const rows = await unwrap<StandingResult[]>(
            api.GET("/api/v1/groups/{groupId}/standings", {
              params: { path: { groupId: group.id as number } },
            }),
          );
          return rows.map((row): OverallStandingRow => ({ ...row, groupName: group.name }));
        }),
      );
      return perGroup
        .flat()
        .sort(
          (a, b) =>
            (b.points ?? 0) - (a.points ?? 0) ||
            (b.pointDifference ?? 0) - (a.pointDifference ?? 0) ||
            (b.pointsFor ?? 0) - (a.pointsFor ?? 0) ||
            (a.playerName ?? "").localeCompare(b.playerName ?? "", "sr"),
        );
    },
  });
}

/**
 * Parametri faze — odatle se zna koliko ih iz grupe prolazi dalje (`directAdvance`),
 * pa tabela može da označi prolaznike. Broj se nikad ne hardkoduje (CLAUDE.md §4).
 */
export function useStage(stageId?: number) {
  return useQuery({
    queryKey: ["stage", stageId],
    enabled: stageId != null,
    queryFn: () =>
      unwrap<StageInfo>(
        api.GET("/api/v1/stages/{id}", { params: { path: { id: stageId as number } } }),
      ),
  });
}

/**
 * Ručni preračun (UC-ENG-01). Unos rezultata ga pokreće sam; ovo je za oporavak i proveru,
 * i posle uvoza podataka migracijom. Idempotentno je (BR-12.3).
 */
export function useRecomputeStandings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: number) =>
      unwrap<StandingResult[]>(
        api.POST("/api/v1/groups/{groupId}/standings/recompute", {
          params: { path: { groupId } },
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["standings"] });
    },
  });
}
