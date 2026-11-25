"use client";

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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { errorMessage } from "@/lib/api/client";
import { splitSlot, toIsoSlot, type MatchRow } from "./types";
import { useCorrectResult, useForfeit, usePostponeMatch } from "./use-matches";

const SELECT_CLASS =
  "h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50";

type DialogProps = {
  match?: MatchRow;
  onClose: () => void;
};

/**
 * UC-ADM-10 — nedolazak (WALKOVER) ili predaja (FORFEIT). Rezultat se NE unosi ručno:
 * uzima se iz parametara sezone (kažnjeni 0, protivnik 11 — BR-10.2). Obrazloženje je obavezno.
 * „Obojica" (DOUBLE_FORFEIT, D-23): niko nije došao ili su obojica predala — 0:0, poraz za oba.
 */
export function ForfeitDialog({ match, onClose }: DialogProps) {
  const forfeit = useForfeit();
  const [punished, setPunished] = useState<number | "both" | "">("");
  const [type, setType] = useState<"WALKOVER" | "FORFEIT">("WALKOVER");
  const [note, setNote] = useState("");

  useEffect(() => {
    if (match) {
      setPunished("");
      setType("WALKOVER");
      setNote("");
    }
  }, [match]);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!match?.id || punished === "") {
      return;
    }
    try {
      await forfeit.mutateAsync(
        punished === "both"
          ? { id: match.id, type: "DOUBLE_FORFEIT", note: note.trim() }
          : { id: match.id, losingPlayerId: punished, type, note: note.trim() },
      );
      toast.success("Službeni rezultat je upisan");
      onClose();
    } catch (error) {
      toast.error(errorMessage(error, "Službeni rezultat nije upisan"));
    }
  }

  return (
    <Dialog open={match != null} onOpenChange={(open) => (open ? null : onClose())}>
      <DialogContent>
        <form onSubmit={submit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Službeni rezultat</DialogTitle>
            <DialogDescription>
              {match ? `${match.player1Name} — ${match.player2Name} (kolo ${match.roundNumber})` : ""}
              . Rezultat se ne unosi ručno, uzima se iz parametara sezone.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-1.5">
            <Label htmlFor="sluzbeni-igrac">Ko je kažnjen</Label>
            <select
              id="sluzbeni-igrac"
              className={SELECT_CLASS}
              required
              value={punished}
              onChange={(e) =>
                setPunished(e.target.value === "" || e.target.value === "both" ? e.target.value : Number(e.target.value))
              }
            >
              <option value="">Izaberite igrača</option>
              {match?.player1Id != null ? (
                <option value={match.player1Id}>{match.player1Name}</option>
              ) : null}
              {match?.player2Id != null ? (
                <option value={match.player2Id}>{match.player2Name}</option>
              ) : null}
              <option value="both">Obojica (niko nije došao ili su obojica predala)</option>
            </select>
            {punished === "both" ? (
              <p className="text-xs text-muted-foreground">
                Upisuje se 0:0 bez pobednika: oba igrača dobijaju poraz i bodove kao za službeni poraz.
              </p>
            ) : null}
          </div>

          {punished === "both" ? null : (
            <div className="space-y-1.5">
              <Label htmlFor="sluzbeni-tip">Razlog</Label>
              <select
                id="sluzbeni-tip"
                className={SELECT_CLASS}
                value={type}
                onChange={(e) => setType(e.target.value as "WALKOVER" | "FORFEIT")}
              >
                <option value="WALKOVER">Nedolazak</option>
                <option value="FORFEIT">Predaja</option>
              </select>
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="sluzbeni-napomena">Obrazloženje (obavezno)</Label>
            <Textarea
              id="sluzbeni-napomena"
              rows={2}
              required
              maxLength={500}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>

          <DialogFooter>
            <Button type="button" variant="ghost" onClick={onClose}>
              Odustani
            </Button>
            <Button type="submit" disabled={forfeit.isPending}>
              {forfeit.isPending ? "Upisivanje…" : "Upiši"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

/** UC-ADM-11 — ispravka već upisanog rezultata; razlog je obavezan i ostaje uz meč. */
export function CorrectionDialog({ match, onClose }: DialogProps) {
  const correct = useCorrectResult();
  const [score1, setScore1] = useState("");
  const [score2, setScore2] = useState("");
  const [reason, setReason] = useState("");

  useEffect(() => {
    if (match) {
      setScore1(match.scorePlayer1 != null ? String(match.scorePlayer1) : "");
      setScore2(match.scorePlayer2 != null ? String(match.scorePlayer2) : "");
      setReason("");
    }
  }, [match]);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!match?.id) {
      return;
    }
    try {
      await correct.mutateAsync({
        id: match.id,
        scorePlayer1: Number(score1),
        scorePlayer2: Number(score2),
        reason: reason.trim(),
      });
      toast.success("Rezultat je ispravljen");
      onClose();
    } catch (error) {
      toast.error(errorMessage(error, "Rezultat nije ispravljen"));
    }
  }

  return (
    <Dialog open={match != null} onOpenChange={(open) => (open ? null : onClose())}>
      <DialogContent>
        <form onSubmit={submit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Ispravka rezultata</DialogTitle>
            <DialogDescription>
              {match
                ? `${match.player1Name} ${match.scorePlayer1}:${match.scorePlayer2} ${match.player2Name}`
                : ""}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-1.5">
            <Label htmlFor="ispravka-rezultat">Nov rezultat</Label>
            <div className="flex items-center gap-2">
              <Input
                id="ispravka-rezultat"
                type="number"
                min={0}
                required
                className="w-20 text-center tabular-nums"
                aria-label={`Rezultat — ${match?.player1Name ?? "prvi igrač"}`}
                value={score1}
                onChange={(e) => setScore1(e.target.value)}
              />
              <span className="text-muted-foreground">:</span>
              <Input
                type="number"
                min={0}
                required
                className="w-20 text-center tabular-nums"
                aria-label={`Rezultat — ${match?.player2Name ?? "drugi igrač"}`}
                value={score2}
                onChange={(e) => setScore2(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="ispravka-razlog">Razlog ispravke (obavezno)</Label>
            <Textarea
              id="ispravka-razlog"
              rows={2}
              required
              maxLength={500}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </div>

          <DialogFooter>
            <Button type="button" variant="ghost" onClick={onClose}>
              Odustani
            </Button>
            <Button type="submit" disabled={correct.isPending}>
              {correct.isPending ? "Čuvanje…" : "Ispravi"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

/** UC-ADM-08 — odlaganje: nov termin i obavezan razlog; meč prelazi u status „Odložen". */
export function PostponeDialog({ match, onClose }: DialogProps) {
  const postpone = usePostponeMatch();
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [note, setNote] = useState("");

  useEffect(() => {
    if (match) {
      const slot = splitSlot(match.scheduledStart);
      setDate(slot.date);
      setTime(slot.time);
      setNote("");
    }
  }, [match]);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!match?.id) {
      return;
    }
    try {
      await postpone.mutateAsync({
        id: match.id,
        scheduledStart: toIsoSlot(date, time) as string,
        note: note.trim(),
      });
      toast.success("Meč je odložen");
      onClose();
    } catch (error) {
      toast.error(errorMessage(error, "Meč nije odložen"));
    }
  }

  return (
    <Dialog open={match != null} onOpenChange={(open) => (open ? null : onClose())}>
      <DialogContent>
        <form onSubmit={submit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Odlaganje meča</DialogTitle>
            <DialogDescription>
              {match ? `${match.player1Name} — ${match.player2Name} (kolo ${match.roundNumber})` : ""}
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="odlaganje-datum">Nov datum</Label>
              <Input
                id="odlaganje-datum"
                type="date"
                required
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="odlaganje-vreme">Novo vreme</Label>
              <Input
                id="odlaganje-vreme"
                type="time"
                value={time}
                onChange={(e) => setTime(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="odlaganje-razlog">Razlog (obavezno)</Label>
            <Textarea
              id="odlaganje-razlog"
              rows={2}
              required
              maxLength={500}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>

          <DialogFooter>
            <Button type="button" variant="ghost" onClick={onClose}>
              Odustani
            </Button>
            <Button type="submit" disabled={postpone.isPending}>
              {postpone.isPending ? "Čuvanje…" : "Odloži"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
