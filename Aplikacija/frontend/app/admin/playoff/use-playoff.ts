"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

export type BracketInfo = components["schemas"]["BracketInfo"];
export type RoundInfo = components["schemas"]["RoundInfo"];
export type SeriesInfo = components["schemas"]["SeriesInfo"];
export type GameInfo = components["schemas"]["GameInfo"];
export type StageResult = components["schemas"]["StageResult"];

/** Playoff (KNOCKOUT) faza tekuće sezone — poslednja po redosledu, ako ih ima više. */
export function usePlayoffStage() {
  const seasons = useQuery({
    queryKey: ["seasons"],
    queryFn: () => unwrap(api.GET("/api/v1/seasons", {})),
  });
  const seasonId = seasons.data?.at(-1)?.id;
  const stages = useQuery({
    queryKey: ["season-stages", seasonId],
    enabled: seasonId != null,
    queryFn: () =>
      unwrap<StageResult[]>(
        api.GET("/api/v1/seasons/{seasonId}/stages", { params: { path: { seasonId: seasonId as number } } }),
      ),
  });
  const stage = (stages.data ?? []).filter((s) => s.type === "KNOCKOUT").at(-1);
  return { stage, isLoading: seasons.isLoading || stages.isLoading };
}

export function useBracket(stageId: number | undefined) {
  return useQuery({
    queryKey: ["bracket", stageId],
    enabled: stageId != null,
    queryFn: () =>
      unwrap<BracketInfo>(api.GET("/api/v1/stages/{stageId}/bracket", { params: { path: { stageId: stageId as number } } })),
  });
}

/** Posle svake izmene: drvo, mečevi (admin lista) i stek „Poništi" — dugme mora da pokaže baš tu radnju. */
function useInvalidate() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: ["bracket"] });
    void queryClient.invalidateQueries({ queryKey: ["matches"] });
    void queryClient.invalidateQueries({ queryKey: ["undo"] });
  };
}

export function useAddGame() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ seriesId, score1, score2 }: { seriesId: number; score1: number; score2: number }) =>
      unwrap<SeriesInfo>(
        api.POST("/api/v1/series/{seriesId}/games", {
          params: { path: { seriesId } },
          body: { score1, score2 },
        }),
      ),
    onSettled: invalidate,
  });
}

export function useCorrectGame() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({
      seriesId,
      matchId,
      score1,
      score2,
    }: {
      seriesId: number;
      matchId: number;
      score1: number;
      score2: number;
    }) =>
      unwrap<SeriesInfo>(
        api.PUT("/api/v1/series/{seriesId}/games/{matchId}", {
          params: { path: { seriesId, matchId } },
          body: { score1, score2 },
        }),
      ),
    onSettled: invalidate,
  });
}

export function useDeleteGame() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ seriesId, matchId }: { seriesId: number; matchId: number }) =>
      unwrap(api.DELETE("/api/v1/series/{seriesId}/games/{matchId}", { params: { path: { seriesId, matchId } } })),
    onSettled: invalidate,
  });
}

export function useWalkover() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ seriesId, winnerId, reason }: { seriesId: number; winnerId: number; reason?: string }) =>
      unwrap<SeriesInfo>(
        api.POST("/api/v1/series/{seriesId}/walkover", {
          params: { path: { seriesId } },
          body: { winnerId, reason },
        }),
      ),
    onSettled: invalidate,
  });
}

export function useReopen() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (seriesId: number) =>
      unwrap<SeriesInfo>(api.DELETE("/api/v1/series/{seriesId}/walkover", { params: { path: { seriesId } } })),
    onSettled: invalidate,
  });
}

export function useSetPlayers() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({
      seriesId,
      player1Id,
      player2Id,
      seed1,
      seed2,
    }: {
      seriesId: number;
      player1Id: number | null;
      player2Id: number | null;
      seed1?: string;
      seed2?: string;
    }) =>
      unwrap<SeriesInfo>(
        api.PUT("/api/v1/series/{seriesId}/players", {
          params: { path: { seriesId } },
          body: { player1Id: player1Id ?? undefined, player2Id: player2Id ?? undefined, seed1, seed2 },
        }),
      ),
    onSettled: invalidate,
  });
}

export function useSchedule() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({
      seriesId,
      scheduledStart,
      courtId,
    }: {
      seriesId: number;
      scheduledStart: string | null;
      courtId: number | null;
    }) =>
      unwrap<SeriesInfo>(
        api.PUT("/api/v1/series/{seriesId}/schedule", {
          params: { path: { seriesId } },
          body: { scheduledStart: scheduledStart ?? undefined, courtId: courtId ?? undefined },
        }),
      ),
    onSettled: invalidate,
  });
}

export function useUpdateRound() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ roundId, ...body }: { roundId: number } & components["schemas"]["UpdateStageRoundCmd"]) =>
      unwrap<BracketInfo>(api.PUT("/api/v1/stage-rounds/{roundId}", { params: { path: { roundId } }, body })),
    onSettled: invalidate,
  });
}

export function useCreateBracket() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (stageId: number) =>
      unwrap<BracketInfo>(api.POST("/api/v1/stages/{stageId}/bracket", { params: { path: { stageId } }, body: {} })),
    onSettled: invalidate,
  });
}
