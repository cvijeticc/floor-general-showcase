"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api/client";
import type {
  BulkResultsInfo,
  CreateMatchCmd,
  MatchInfo,
  MatchRow,
  MatchStatus,
  UpdateMatchCmd,
} from "./types";

export type MatchFilters = {
  groupId: number | "";
  round: string;
  date: string;
  status: MatchStatus | "";
  /** Svi mecevi jednog igraca — „istorija" (backend filter playerId). */
  playerId: number | "";
  /** Samo mecevi protiv ovog protivnika; filtrira se u pregledacu, nad mecevima igraca. */
  opponentId: number | "";
};

export const EMPTY_FILTERS: MatchFilters = {
  groupId: "",
  round: "",
  date: "",
  status: "",
  playerId: "",
  opponentId: "",
};

export function useMatches(filters: MatchFilters) {
  // Protivnik se ne salje backendu — promena protivnika ne treba nov upit.
  const { opponentId, ...server } = filters;
  const query = useQuery({
    queryKey: ["matches", server],
    queryFn: () =>
      unwrap<MatchRow[]>(
        api.GET("/api/v1/matches", {
          params: {
            query: {
              groupId: filters.groupId === "" ? undefined : filters.groupId,
              playerId: filters.playerId === "" ? undefined : filters.playerId,
              round: filters.round === "" ? undefined : Number(filters.round),
              date: filters.date || undefined,
              status: filters.status || undefined,
            },
          },
        }),
      ),
  });

  const rows =
    opponentId === "" || query.data == null
      ? query.data
      : query.data.filter((match) => match.player1Id === opponentId || match.player2Id === opponentId);
  return { ...query, data: rows };
}

/**
 * Posle svake izmene se osvezavaju mecevi, tabele grupa i stek ponistavanja — dugme „Poništi"
 * mora odmah da pokaze bas onu radnju koja se upravo desila.
 */
function useInvalidateMatches() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: ["matches"] });
    void queryClient.invalidateQueries({ queryKey: ["standings"] });
    void queryClient.invalidateQueries({ queryKey: ["undo"] });
  };
}

/** UC-ADM-08 — zakazivanje ili unos vec odigranog meca (rezultat se sme poslati odmah, D-07). */
export function useCreateMatch() {
  const invalidate = useInvalidateMatches();
  return useMutation({
    mutationFn: (cmd: CreateMatchCmd) => unwrap<MatchInfo>(api.POST("/api/v1/matches", { body: cmd })),
    onSuccess: invalidate,
  });
}

/** Termin, teren, kolo i napomena — rezultat se ovde ne dira. */
export function useUpdateMatch() {
  const invalidate = useInvalidateMatches();
  return useMutation({
    mutationFn: ({ id, cmd }: { id: number; cmd: UpdateMatchCmd }) =>
      unwrap<MatchInfo>(api.PUT("/api/v1/matches/{id}", { params: { path: { id } }, body: cmd })),
    onSuccess: invalidate,
  });
}

export function useDeleteMatch() {
  const invalidate = useInvalidateMatches();
  return useMutation({
    mutationFn: (id: number) => unwrap(api.DELETE("/api/v1/matches/{id}", { params: { path: { id } } })),
    onSuccess: invalidate,
  });
}

/**
 * UC-ADM-09 — masovni unos celog kola u jednom pozivu. Ovo je glavni slucaj upotrebe:
 * uvece jedna osoba unosi ~60 rezultata (CLAUDE.md §3.1).
 */
export function useEnterResults() {
  const invalidate = useInvalidateMatches();
  return useMutation({
    mutationFn: (results: { matchId: number; scorePlayer1: number; scorePlayer2: number }[]) =>
      unwrap<BulkResultsInfo>(api.PUT("/api/v1/matches/results", { body: { results } })),
    onSuccess: invalidate,
  });
}

/** UC-ADM-11 — ispravka vec upisanog rezultata; razlog je obavezan. */
export function useCorrectResult() {
  const invalidate = useInvalidateMatches();
  return useMutation({
    mutationFn: ({
      id,
      scorePlayer1,
      scorePlayer2,
      reason,
    }: {
      id: number;
      scorePlayer1: number;
      scorePlayer2: number;
      reason: string;
    }) =>
      unwrap(
        api.PATCH("/api/v1/matches/{id}/result", {
          params: { path: { id } },
          body: { scorePlayer1, scorePlayer2, reason },
        }),
      ),
    onSuccess: invalidate,
  });
}

/** UC-ADM-10 — nedolazak ili predaja; rezultat se uzima iz parametara sezone (BR-10.2). */
export function useForfeit() {
  const invalidate = useInvalidateMatches();
  return useMutation({
    mutationFn: ({
      id,
      losingPlayerId,
      type,
      note,
    }: {
      id: number;
      /** Nema ga kod DOUBLE_FORFEIT — kažnjeni su obojica (D-23). */
      losingPlayerId?: number;
      type: "WALKOVER" | "FORFEIT" | "DOUBLE_FORFEIT";
      note: string;
    }) =>
      unwrap(
        api.POST("/api/v1/matches/{id}/forfeit", {
          params: { path: { id } },
          body: { losingPlayerId, type, note },
        }),
      ),
    onSuccess: invalidate,
  });
}

/** Odlaganje: nov termin + obavezan razlog (UC-ADM-08). */
export function usePostponeMatch() {
  const invalidate = useInvalidateMatches();
  return useMutation({
    mutationFn: ({ id, scheduledStart, note }: { id: number; scheduledStart: string; note: string }) =>
      unwrap<MatchInfo>(
        api.PATCH("/api/v1/matches/{id}/postpone", {
          params: { path: { id } },
          body: { scheduledStart, note },
        }),
      ),
    onSuccess: invalidate,
  });
}
