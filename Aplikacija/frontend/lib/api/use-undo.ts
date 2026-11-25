"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

export type UndoEntryResult = components["schemas"]["UndoEntryResult"];
export type UndoInfo = components["schemas"]["UndoInfo"];

/**
 * UC-ADM-23 — sta ce „Poništi" uraditi sledece.
 * Backend vraca 204 kad nema sta da se ponisti, pa se to ovde pretvara u `null`.
 */
export function useLastUndoable() {
  return useQuery<UndoEntryResult | null>({
    queryKey: ["undo", "last"],
    queryFn: async () => {
      const { data, error, response } = await api.GET("/api/v1/undo/last", {});
      if (response.status === 204) {
        return null;
      }
      if (error) {
        throw error;
      }
      return data ?? null;
    },
  });
}

/**
 * Ponistavanje moze da dirne bilo koji ekran (igraca, grupu, meč, tabelu), pa se posle njega
 * osvezava sve — ciljano invalidiranje bi pre ili kasnije promasilo neki ekran.
 */
export function useUndo() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => unwrap<UndoInfo>(api.POST("/api/v1/undo", {})),
    onSettled: () => queryClient.invalidateQueries(),
  });
}
