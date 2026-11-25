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
import { api, errorMessage } from "@/lib/api/client";
import { useCreatePlayer, useUpdatePlayer } from "./use-players";
import type { CreatePlayerCmd, PlayerInfo } from "./types";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Kad je prosledjen, forma menja postojeceg igraca; inace pravi novog. */
  playerId?: number;
};

type FormState = {
  firstName: string;
  lastName: string;
  nickname: string;
  birthYear: string;
  phone: string;
  heightCm: string;
  weightKg: string;
  paymentNote: string;
  note: string;
};

const EMPTY: FormState = {
  firstName: "",
  lastName: "",
  nickname: "",
  birthYear: "",
  phone: "",
  heightCm: "",
  weightKg: "",
  paymentNote: "",
  note: "",
};

function toNumber(value: string): number | undefined {
  const trimmed = value.trim();
  return trimmed === "" ? undefined : Number(trimmed);
}

export function PlayerFormDialog({ open, onOpenChange, playerId }: Props) {
  const isEdit = playerId != null;
  const [form, setForm] = useState<FormState>(EMPTY);

  /**
   * Izmena se uvek radi nad PUNIM zapisom: lista (PlayerResult) ne nosi uplatu ni napomenu,
   * pa bi slanje PUT-a iz nje tiho obrisalo oba polja. Zato se detalj dovlaci pre popune forme.
   */
  const detail = useQuery({
    queryKey: ["player", playerId],
    enabled: open && isEdit,
    queryFn: async () => {
      const { data, error } = await api.GET("/api/v1/players/{id}", {
        params: { path: { id: playerId as number } },
      });
      if (error) {
        throw error;
      }
      return data as PlayerInfo;
    },
  });

  useEffect(() => {
    if (!open) {
      return;
    }
    if (!isEdit) {
      setForm(EMPTY);
      return;
    }
    const player = detail.data;
    if (player) {
      setForm({
        firstName: player.firstName ?? "",
        lastName: player.lastName ?? "",
        nickname: player.nickname ?? "",
        birthYear: player.birthYear?.toString() ?? "",
        phone: player.phone ?? "",
        heightCm: player.heightCm?.toString() ?? "",
        weightKg: player.weightKg?.toString() ?? "",
        paymentNote: player.paymentNote ?? "",
        note: player.note ?? "",
      });
    }
  }, [open, isEdit, detail.data]);

  const createPlayer = useCreatePlayer();
  const updatePlayer = useUpdatePlayer();
  const isSaving = createPlayer.isPending || updatePlayer.isPending;
  const isLoadingDetail = isEdit && detail.isLoading;

  function set<K extends keyof FormState>(key: K, value: string) {
    setForm((previous) => ({ ...previous, [key]: value }));
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();

    const cmd: CreatePlayerCmd = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      nickname: form.nickname.trim(),
      birthYear: toNumber(form.birthYear),
      phone: form.phone.trim() || undefined,
      heightCm: toNumber(form.heightCm),
      weightKg: toNumber(form.weightKg),
      paymentNote: form.paymentNote.trim() || undefined,
      note: form.note.trim() || undefined,
    };

    try {
      if (isEdit) {
        await updatePlayer.mutateAsync({ id: playerId, cmd });
        toast.success("Igrač " + cmd.firstName + " " + cmd.lastName + " je izmenjen");
      } else {
        await createPlayer.mutateAsync(cmd);
        toast.success("Igrač " + cmd.firstName + " " + cmd.lastName + " je dodat");
      }
      onOpenChange(false);
    } catch (error) {
      toast.error(errorMessage(error, "Igrač nije sačuvan"));
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <form onSubmit={submit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{isEdit ? "Izmena igrača" : "Novi igrač"}</DialogTitle>
            <DialogDescription>
              Ime, prezime i nadimak su obavezni — liga igrače vodi po nadimku.
            </DialogDescription>
          </DialogHeader>

          {isLoadingDetail ? (
            <p className="py-6 text-sm text-muted-foreground">Učitavanje podataka o igraču…</p>
          ) : detail.error ? (
            <p role="alert" className="py-6 text-sm text-destructive">
              {errorMessage(detail.error, "Podaci o igraču nisu učitani")}
            </p>
          ) : (
            <div className="grid gap-4 sm:grid-cols-3">
              <Field label="Ime" required>
                <Input
                  value={form.firstName}
                  onChange={(e) => set("firstName", e.target.value)}
                  required
                  maxLength={60}
                />
              </Field>
              <Field label="Prezime" required>
                <Input
                  value={form.lastName}
                  onChange={(e) => set("lastName", e.target.value)}
                  required
                  maxLength={60}
                />
              </Field>
              <Field label="Nadimak" required>
                <Input
                  value={form.nickname}
                  onChange={(e) => set("nickname", e.target.value)}
                  required
                  maxLength={60}
                />
              </Field>

              <Field label="Godište">
                <Input
                  type="number"
                  inputMode="numeric"
                  min={1900}
                  max={2020}
                  value={form.birthYear}
                  onChange={(e) => set("birthYear", e.target.value)}
                />
              </Field>
              <Field label="Visina (cm)">
                <Input
                  type="number"
                  min={100}
                  max={260}
                  value={form.heightCm}
                  onChange={(e) => set("heightCm", e.target.value)}
                />
              </Field>
              <Field label="Težina (kg)">
                <Input
                  type="number"
                  min={30}
                  max={250}
                  value={form.weightKg}
                  onChange={(e) => set("weightKg", e.target.value)}
                />
              </Field>

              <Field label="Telefon" className="sm:col-span-2">
                <Input value={form.phone} onChange={(e) => set("phone", e.target.value)} maxLength={30} />
              </Field>
              <Field label="Uplata">
                <Input
                  value={form.paymentNote}
                  onChange={(e) => set("paymentNote", e.target.value)}
                  maxLength={40}
                  placeholder="npr. 6500din"
                />
              </Field>

              <Field label="Napomena" className="sm:col-span-3">
                <Textarea
                  value={form.note}
                  onChange={(e) => set("note", e.target.value)}
                  maxLength={500}
                  rows={2}
                  placeholder="Kada može da igra, ograničenja u terminima…"
                />
              </Field>
            </div>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Odustani
            </Button>
            <Button type="submit" disabled={isSaving || isLoadingDetail}>
              {isSaving ? "Čuvanje…" : "Sačuvaj"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function Field({
  label,
  required,
  className,
  children,
}: {
  label: string;
  required?: boolean;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={"space-y-1.5 " + (className ?? "")}>
      <Label>
        {label}
        {required ? <span className="text-destructive"> *</span> : null}
      </Label>
      {children}
    </div>
  );
}
