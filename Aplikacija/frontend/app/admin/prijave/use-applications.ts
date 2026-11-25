"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

export type ApplicationResult = components["schemas"]["ApplicationResult"];
export type ApplicationStatus = NonNullable<ApplicationResult["status"]>;
export type SkillLevel = NonNullable<ApplicationResult["skillLevel"]>;

export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  NEW: "Nova",
  CONTACTED: "Kontaktiran",
  ACCEPTED: "Primljen",
  REJECTED: "Odbijen",
};

export const SKILL_LABELS: Record<SkillLevel, string> = {
  BEGINNER: "Početnik",
  TRAINED_BEFORE: "Trenirao pre",
  PRO: "Profi",
};

/** D-20 — prijave sa javnog sajta, najnovije prve. */
export function useApplications(status: ApplicationStatus | "") {
  return useQuery({
    queryKey: ["applications", status],
    queryFn: () =>
      unwrap<ApplicationResult[]>(
        api.GET("/api/v1/applications", { params: { query: status ? { status } : {} } }),
      ),
  });
}

/** Posle svake izmene osvežava se lista, igrači (prijem pravi igrača) i „Poništi" dugme. */
function useInvalidate() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: ["applications"] });
    void queryClient.invalidateQueries({ queryKey: ["players"] });
    void queryClient.invalidateQueries({ queryKey: ["undo"] });
  };
}

export function useChangeApplicationStatus() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: ApplicationStatus }) =>
      unwrap(api.PATCH("/api/v1/applications/{id}/status", { params: { path: { id } }, body: { status } })),
    onSuccess: invalidate,
  });
}

export function useAcceptApplication() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (id: number) =>
      unwrap<ApplicationResult>(api.POST("/api/v1/applications/{id}/accept", { params: { path: { id } } })),
    onSuccess: invalidate,
  });
}

export function useDeleteApplication() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (id: number) => unwrap(api.DELETE("/api/v1/applications/{id}", { params: { path: { id } } })),
    onSuccess: invalidate,
  });
}
