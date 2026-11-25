"use client";

import { useQueries, useQuery } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

export type GroupResult = components["schemas"]["GroupResult"];
export type CourtResult = components["schemas"]["CourtResult"];
export type PlayerResult = components["schemas"]["PlayerResult"];

/**
 * Grupe tekuce sezone — treba ih i ekran „Igrači" i ekran „Mečevi", pa stoje ovde,
 * a ne u jednom od njih.
 */
export function useSeasonGroups() {
  const seasons = useQuery({
    queryKey: ["seasons"],
    queryFn: () => unwrap(api.GET("/api/v1/seasons", {})),
  });

  // Vodi se tekuca sezona; ako ih ima vise, uzima se poslednja napravljena.
  const seasonId = seasons.data?.at(-1)?.id;

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

  // Grupe se zovu „Grupa 1"…„Grupa 10"; abecedno sortiranje bi „Grupu 10" stavilo iza „Grupe 1".
  const sorted = [...(groups.data ?? [])].sort((a, b) =>
    (a.name ?? "").localeCompare(b.name ?? "", "sr", { numeric: true }),
  );

  return { seasonId, groups: sorted, isLoading: seasons.isLoading || groups.isLoading };
}

/** Tereni lige — za padajucu listu pri zakazivanju meca (u ovoj sezoni „Teren A" i „Teren B"). */
export function useCourts() {
  return useQuery({
    queryKey: ["courts"],
    queryFn: () => unwrap<CourtResult[]>(api.GET("/api/v1/courts", {})),
  });
}

/** Igraci jedne grupe — spisak iz kog se biraju protivnici. */
export function useGroupPlayers(groupId: number | "") {
  return useQuery({
    queryKey: ["players", "by-group", groupId],
    enabled: groupId !== "",
    queryFn: () =>
      unwrap<PlayerResult[]>(
        api.GET("/api/v1/players", { params: { query: { groupId: groupId as number } } }),
      ),
  });
}

export type SeasonPlayer = PlayerResult & { id: number; groupId: number; groupName: string };

/**
 * Svi igraci tekuce sezone, svaki sa svojom grupom — za izbor igraca kucanjem bez prethodnog
 * biranja grupe. Upiti su isti kao u {@link useGroupPlayers}, pa se kes deli.
 */
export function useSeasonPlayers() {
  const { groups } = useSeasonGroups();
  const results = useQueries({
    queries: groups.map((group) => ({
      queryKey: ["players", "by-group", group.id],
      queryFn: () =>
        unwrap<PlayerResult[]>(api.GET("/api/v1/players", { params: { query: { groupId: group.id } } })),
    })),
  });

  const players: SeasonPlayer[] = [];
  results.forEach((result, index) => {
    const group = groups[index];
    for (const player of result.data ?? []) {
      if (player.id != null && group?.id != null) {
        players.push({ ...player, id: player.id, groupId: group.id, groupName: group.name ?? "" });
      }
    }
  });

  return { players, isLoading: results.some((result) => result.isLoading) };
}
