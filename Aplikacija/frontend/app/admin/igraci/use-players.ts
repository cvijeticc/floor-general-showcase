"use client";

import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api/client";
import { useSeasonGroups } from "@/lib/api/queries";
import { searchPlayers } from "@/lib/player-search";
import type {
  CreatePlayerCmd,
  GroupResult,
  PlayerResult,
  PlayerStatus,
  UpdatePlayerCmd,
} from "./types";

export type PlayerFilters = {
  search: string;
  status: PlayerStatus | "";
  groupId: number | "";
};

/**
 * Pretraga po imenu ide u pregledacu (lib/player-search), ne na backendu: „arci" mora da nadje
 * „Arči", a SQL LIKE dijakritike ne izjednacava. Igraca je ~100, pa se ucitaju svi.
 */
export function usePlayers(filters: PlayerFilters) {
  const query = useQuery({
    queryKey: ["players", { status: filters.status, groupId: filters.groupId }],
    queryFn: () =>
      unwrap<PlayerResult[]>(
        api.GET("/api/v1/players", {
          params: {
            query: {
              status: filters.status || undefined,
              groupId: filters.groupId === "" ? undefined : filters.groupId,
            },
          },
        }),
      ),
  });

  const rows =
    filters.search.trim() === "" || query.data == null
      ? query.data
      : searchPlayers(filters.search, query.data, (player) => ({
          nickname: player.nickname ?? "",
          name: player.fullName ?? "",
        }));
  return { ...query, data: rows };
}

// Grupe tekuce sezone su zajednicke sa ostalim ekranima — hook je u lib/api/queries.ts.
export { useSeasonGroups };

/**
 * Mapa igrac -> grupa. Backend namerno ne vraca grupu uz igraca: igrac moze biti u vise faza
 * iste sezone (D-04, GroupEntry), pa „njegova grupa" nije jedinstvena van konteksta faze.
 * Zato se ovde sastavlja po grupama tekuce sezone — po jedan lak upit za svaku.
 */
export function usePlayerGroupNames(groups: GroupResult[]) {
  const results = useQueries({
    queries: groups.map((group) => ({
      queryKey: ["players", "by-group", group.id],
      queryFn: () =>
        unwrap<PlayerResult[]>(
          api.GET("/api/v1/players", { params: { query: { groupId: group.id } } }),
        ),
    })),
  });

  const byPlayerId = new Map<number, string>();
  results.forEach((result, index) => {
    const groupName = groups[index]?.name;
    if (!groupName) {
      return;
    }
    for (const player of result.data ?? []) {
      if (player.id != null) {
        byPlayerId.set(player.id, groupName);
      }
    }
  });

  return byPlayerId;
}

/**
 * Posle svake izmene se osvezavaju lista, mapa grupa i stek ponistavanja — dugme „Poništi"
 * mora da pokaze bas onu radnju koja se upravo desila (UC-ADM-23).
 */
function useInvalidatePlayers() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: ["players"] });
    // Odustanak otkazuje neodigrane meceve (BR-10), pa se i oni osvezavaju.
    void queryClient.invalidateQueries({ queryKey: ["matches"] });
    void queryClient.invalidateQueries({ queryKey: ["undo"] });
  };
}

export function useCreatePlayer() {
  const invalidate = useInvalidatePlayers();
  return useMutation({
    mutationFn: (cmd: CreatePlayerCmd) => unwrap(api.POST("/api/v1/players", { body: cmd })),
    onSuccess: invalidate,
  });
}

export function useUpdatePlayer() {
  const invalidate = useInvalidatePlayers();
  return useMutation({
    mutationFn: ({ id, cmd }: { id: number; cmd: UpdatePlayerCmd }) =>
      unwrap(api.PUT("/api/v1/players/{id}", { params: { path: { id } }, body: cmd })),
    onSuccess: invalidate,
  });
}

/** BR-10 — status ide zasebnom operacijom, ne kroz PUT. */
export function useChangePlayerStatus() {
  const invalidate = useInvalidatePlayers();
  return useMutation({
    mutationFn: ({ id, status, note }: { id: number; status: PlayerStatus; note?: string }) =>
      unwrap(
        api.PATCH("/api/v1/players/{id}/status", {
          params: { path: { id } },
          body: { status, note },
        }),
      ),
    onSuccess: invalidate,
  });
}

export function useDeletePlayer() {
  const invalidate = useInvalidatePlayers();
  return useMutation({
    mutationFn: (id: number) => unwrap(api.DELETE("/api/v1/players/{id}", { params: { path: { id } } })),
    onSuccess: invalidate,
  });
}
