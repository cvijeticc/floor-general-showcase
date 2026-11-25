"use client";

import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { api, errorMessage, unwrap } from "@/lib/api/client";
import type { MatchRow } from "../mecevi/types";
import { hasResult } from "../mecevi/types";
import { STATUS_LABELS, type PlayerResult, type PlayerStatus } from "./types";
import { useChangePlayerStatus } from "./use-players";

/**
 * BR-10 — „Odustao" / „Isključen": igrač više ne igra do kraja sezone. Backend otkazuje sve
 * njegove neodigrane mečeve, a odigrani i tabela ostaju kakvi jesu (BR-10.5 još nije odlučeno,
 * N-14a). Pre potvrde se vidi koliko će mečeva biti otkazano; sve se vraća dugmetom „Poništi".
 */
export function WithdrawDialog({ player, onClose }: { player?: PlayerResult; onClose: () => void }) {
  const [status, setStatus] = useState<PlayerStatus>("WITHDRAWN");
  const [note, setNote] = useState("");
  const changeStatus = useChangePlayerStatus();
  const open = player != null;

  useEffect(() => {
    if (open) {
      setStatus("WITHDRAWN");
      setNote("");
    }
  }, [open]);

  const matches = useQuery({
    queryKey: ["matches", { playerId: player?.id }],
    enabled: open && player?.id != null,
    queryFn: () =>
      unwrap<MatchRow[]>(
        api.GET("/api/v1/matches", { params: { query: { playerId: player?.id as number } } }),
      ),
  });
  const toCancel = (matches.data ?? []).filter(
    (match) => !hasResult(match.status) && match.status !== "CANCELLED",
  ).length;
  const played = (matches.data ?? []).filter((match) => hasResult(match.status)).length;

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (player?.id == null) {
      return;
    }
    try {
      await changeStatus.mutateAsync({ id: player.id, status, note: note.trim() || undefined });
      toast.success(
        `${player.nickname || player.fullName}: ${STATUS_LABELS[status].toLowerCase()}` +
          (toCancel ? `, otkazano ${toCancel} ${toCancel === 1 ? "meč" : toCancel < 5 ? "meča" : "mečeva"}` : ""),
      );
      onClose();
    } catch (error) {
      toast.error(errorMessage(error, "Status nije promenjen"));
    }
  }

  return (
    <Dialog open={open} onOpenChange={(value) => !value && onClose()}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={submit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Igrač ne igra do kraja sezone</DialogTitle>
            <DialogDescription>
              {player ? `${player.nickname} (${player.fullName})` : ""}
            </DialogDescription>
          </DialogHeader>

          <fieldset className="grid grid-cols-2 gap-2">
            <legend className="sr-only">Razlog prestanka</legend>
            {(["WITHDRAWN", "DISQUALIFIED"] as const).map((value) => (
              <label
                key={value}
                className="flex h-11 cursor-pointer items-center justify-center rounded-lg border text-sm font-medium has-[:checked]:border-foreground has-[:checked]:bg-muted"
              >
                <input
                  type="radio"
                  name="status"
                  value={value}
                  className="sr-only"
                  checked={status === value}
                  onChange={() => setStatus(value)}
                />
                {STATUS_LABELS[value]}
              </label>
            ))}
          </fieldset>

          <div className="space-y-1.5">
            <Label htmlFor="odustao-razlog">Razlog</Label>
            <Textarea
              id="odustao-razlog"
              rows={2}
              maxLength={400}
              placeholder="npr. povreda kolena, odselio se…"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>

          <div className="rounded-md bg-muted/60 px-3 py-2.5 text-sm" role="status">
            {matches.isLoading ? (
              "Proveravam mečeve…"
            ) : (
              <>
                <p>
                  {toCancel > 0 ? (
                    <>
                      Neodigrani mečevi koji se otkazuju: <strong>{toCancel}</strong> — nestaju iz rasporeda.
                    </>
                  ) : (
                    "Nema zakazanih mečeva koje treba otkazati."
                  )}
                </p>
                <p className="mt-0.5 text-muted-foreground">
                  Odigrani mečevi ({played}) i tabela ostaju kakvi jesu. Greška se vraća dugmetom „Poništi“.
                </p>
              </>
            )}
          </div>

          <DialogFooter className="gap-2">
            <Button type="button" variant="ghost" className="h-11 md:h-9" onClick={onClose}>
              Zatvori
            </Button>
            <Button
              type="submit"
              variant="destructive"
              className="h-11 md:h-9"
              disabled={changeStatus.isPending || matches.isLoading}
            >
              {changeStatus.isPending ? "Čuvanje…" : `Potvrdi — ${STATUS_LABELS[status].toLowerCase()}`}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
