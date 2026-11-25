"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
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
import { api, errorMessage, unwrap } from "@/lib/api/client";
import { useCourts, useSeasonGroups, useSeasonPlayers } from "@/lib/api/queries";
import { PlayerCombobox, seasonPlayerOption, type ComboboxPlayer } from "../player-combobox";
import { splitSlot, todayLocalDate, toIsoSlot, type MatchInfo } from "./types";
import { useCreateMatch, useUpdateMatch } from "./use-matches";

const SELECT_CLASS =
  "h-11 md:h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Kad je prosledjen, menja se termin postojeceg meca; inace se unosi nov. */
  matchId?: number;
};

type FormState = {
  groupId: number | "";
  roundNumber: string;
  player1Id: number | "";
  player2Id: number | "";
  courtId: number | "";
  date: string;
  time: string;
  score1: string;
  score2: string;
  note: string;
};

const EMPTY: FormState = {
  groupId: "",
  roundNumber: "1",
  player1Id: "",
  player2Id: "",
  courtId: "",
  date: "",
  time: "",
  score1: "",
  score2: "",
  note: "",
};

/**
 * UC-FE-07 — unos meca: ko je igrao s kim, kog datuma, u koliko sati, na kom terenu i sa kojim
 * rezultatom. Rezultat je opcion: ako se popuni, mec se odmah cuva kao odigran, sa datumom u
 * proslosti ako treba (odluka D-07 — retroaktivan unos je normalan slucaj, ne izuzetak).
 */
export function MatchFormDialog({ open, onOpenChange, matchId }: Props) {
  const isEdit = matchId != null;
  const [form, setForm] = useState<FormState>(EMPTY);

  const { groups } = useSeasonGroups();
  const courts = useCourts();
  const { players } = useSeasonPlayers();

  /**
   * Igrac se trazi kucanjem po celoj sezoni, bez prethodnog biranja grupe: izbor prvog igraca
   * sam postavi grupu, pa se drugi trazi samo medju njegovim protivnicima iz te grupe.
   */
  function pickerOptions(excludedId: number | ""): ComboboxPlayer[] {
    return players
      .filter((player) => (form.groupId === "" || player.groupId === form.groupId) && player.id !== excludedId)
      .map((player) => seasonPlayerOption(player, form.groupId === ""));
  }

  function pickPlayer(key: "player1Id" | "player2Id", id: number | "") {
    const group = players.find((player) => player.id === id)?.groupId;
    setForm((current) => ({ ...current, [key]: id, groupId: group ?? current.groupId }));
  }

  const createMatch = useCreateMatch();
  const updateMatch = useUpdateMatch();

  const detail = useQuery({
    queryKey: ["match", matchId],
    enabled: open && isEdit,
    queryFn: () =>
      unwrap<MatchInfo>(api.GET("/api/v1/matches/{id}", { params: { path: { id: matchId as number } } })),
  });

  useEffect(() => {
    if (!open) {
      return;
    }
    if (!isEdit) {
      // Novi mec se najcesce unosi istog dana kad je odigran; vreme ostaje prazno.
      setForm({ ...EMPTY, date: todayLocalDate() });
      return;
    }
    const match = detail.data;
    if (!match) {
      return;
    }
    const slot = splitSlot(match.scheduledStart);
    setForm({
      groupId: match.groupId ?? "",
      roundNumber: String(match.roundNumber ?? 1),
      player1Id: match.player1Id ?? "",
      player2Id: match.player2Id ?? "",
      courtId: match.courtId ?? "",
      date: slot.date,
      time: slot.time,
      score1: "",
      score2: "",
      note: match.note ?? "",
    });
  }, [open, isEdit, detail.data]);

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();

    try {
      if (isEdit) {
        await updateMatch.mutateAsync({
          id: matchId as number,
          cmd: {
            roundNumber: Number(form.roundNumber),
            courtId: form.courtId === "" ? undefined : form.courtId,
            scheduledStart: toIsoSlot(form.date, form.time),
            note: form.note.trim() || undefined,
          },
        });
        toast.success("Termin meča je izmenjen");
      } else {
        if (form.groupId === "" || form.player1Id === "" || form.player2Id === "") {
          toast.error("Izaberite oba igrača iz ponuđenog spiska");
          return;
        }
        const created = await createMatch.mutateAsync({
          groupId: form.groupId,
          roundNumber: Number(form.roundNumber),
          player1Id: form.player1Id,
          player2Id: form.player2Id,
          courtId: form.courtId === "" ? undefined : form.courtId,
          scheduledStart: toIsoSlot(form.date, form.time),
          scorePlayer1: form.score1.trim() === "" ? undefined : Number(form.score1),
          scorePlayer2: form.score2.trim() === "" ? undefined : Number(form.score2),
          note: form.note.trim() || undefined,
        });
        toast.success(
          created.scorePlayer1 != null
            ? `Meč unet kao odigran: ${created.scorePlayer1}:${created.scorePlayer2}`
            : "Meč je zakazan",
        );
      }
      onOpenChange(false);
    } catch (error) {
      // Backend odbija igraca van grupe, zauzet termin i neispravan rezultat — poruka je na srpskom.
      toast.error(errorMessage(error, "Meč nije sačuvan"));
    }
  }

  const isPending = createMatch.isPending || updateMatch.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <form onSubmit={submit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{isEdit ? "Izmena meča" : "Novi meč"}</DialogTitle>
            <DialogDescription>
              {isEdit
                ? "Menjaju se kolo, termin, teren i napomena. Rezultat se unosi u tabeli, a ispravlja kroz „Ispravi“."
                : "Rezultat je opcion — ako se popuni, meč se odmah čuva kao odigran. Datum u prošlosti je dozvoljen."}
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="mec-igrac1">Igrač 1</Label>
              <PlayerCombobox
                id="mec-igrac1"
                players={pickerOptions(form.player2Id)}
                value={form.player1Id}
                onChange={(value) => pickPlayer("player1Id", value)}
                disabled={isEdit}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="mec-igrac2">Igrač 2</Label>
              <PlayerCombobox
                id="mec-igrac2"
                players={pickerOptions(form.player1Id)}
                value={form.player2Id}
                onChange={(value) => pickPlayer("player2Id", value)}
                disabled={isEdit}
                placeholder={form.player1Id === "" ? "Ukucajte ime, prezime ili nadimak" : "Protivnik iz iste grupe"}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="mec-grupa">Grupa</Label>
              <select
                id="mec-grupa"
                className={SELECT_CLASS}
                required={!isEdit}
                disabled={isEdit}
                value={form.groupId}
                onChange={(e) => {
                  const value = e.target.value === "" ? "" : Number(e.target.value);
                  setForm((current) => ({ ...current, groupId: value, player1Id: "", player2Id: "" }));
                }}
              >
                <option value="">Izaberite grupu</option>
                {groups.map((group) => (
                  <option key={group.id} value={group.id}>
                    {group.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="mec-kolo">Kolo</Label>
              <Input
                id="mec-kolo"
                className="h-11 md:h-8"
                type="number"
                inputMode="numeric"
                min={1}
                required
                value={form.roundNumber}
                onChange={(e) => set("roundNumber", e.target.value)}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="mec-datum">Datum</Label>
              <Input
                id="mec-datum"
                className="h-11 md:h-8"
                type="date"
                value={form.date}
                onChange={(e) => set("date", e.target.value)}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="mec-vreme">Vreme</Label>
              <Input
                id="mec-vreme"
                className="h-11 md:h-8"
                type="time"
                value={form.time}
                onChange={(e) => set("time", e.target.value)}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="mec-teren">Teren</Label>
              <select
                id="mec-teren"
                className={SELECT_CLASS}
                value={form.courtId}
                onChange={(e) => set("courtId", e.target.value === "" ? "" : Number(e.target.value))}
              >
                <option value="">Bez terena</option>
                {(courts.data ?? []).map((court) => (
                  <option key={court.id} value={court.id}>
                    {court.name}
                  </option>
                ))}
              </select>
            </div>

            {isEdit ? null : (
              <div className="space-y-1.5">
                <Label htmlFor="mec-rezultat1">Rezultat (ako je već odigran)</Label>
                <div className="flex items-center gap-2">
                  <Input
                    id="mec-rezultat1"
                    type="number"
                    inputMode="numeric"
                    min={0}
                    className="h-11 w-20 text-center tabular-nums md:h-8"
                    aria-label="Rezultat prvog igrača"
                    value={form.score1}
                    onChange={(e) => set("score1", e.target.value)}
                  />
                  <span className="text-muted-foreground">:</span>
                  <Input
                    type="number"
                    inputMode="numeric"
                    min={0}
                    className="h-11 w-20 text-center tabular-nums md:h-8"
                    aria-label="Rezultat drugog igrača"
                    value={form.score2}
                    onChange={(e) => set("score2", e.target.value)}
                  />
                </div>
              </div>
            )}

            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="mec-napomena">Napomena</Label>
              <Textarea
                id="mec-napomena"
                rows={2}
                maxLength={500}
                value={form.note}
                onChange={(e) => set("note", e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Odustani
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? "Čuvanje…" : "Sačuvaj"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

