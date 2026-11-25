"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { MailCheckIcon, MailWarningIcon, SendIcon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { API_PROXY_PREFIX } from "@/lib/api/config";

type MailStatus = { enabled: boolean; recipients: string[]; sender: string | null };

/**
 * D-20 — da li server šalje mejl o svakoj novoj prijavi i kome. „Pošalji probni mejl" odmah
 * proverava SMTP podešavanje: ako mejl stigne, stižu i prijave.
 *
 * Obične fetch pozive (a ne generisani klijent) ovde koristimo dok se `schema.d.ts` ne
 * regeneriše iz OpenAPI-ja sa ove dve nove rute (`npm run api:generate`).
 */
export function MailStatusCard() {
  const status = useQuery({
    queryKey: ["applications", "mail-status"],
    queryFn: async () => {
      const response = await fetch(`${API_PROXY_PREFIX}/api/v1/applications/mail-status`);
      if (!response.ok) throw new Error("Status mejla nije dostupan");
      return (await response.json()) as MailStatus;
    },
  });

  const test = useMutation({
    mutationFn: async () => {
      const response = await fetch(`${API_PROXY_PREFIX}/api/v1/applications/mail-test`, { method: "POST" });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { message?: string } | null;
        throw new Error(body?.message ?? "Probni mejl nije poslat");
      }
    },
    onSuccess: () => toast.success(`Probni mejl je poslat na ${status.data?.recipients.join(", ")}. Proveri poštu (i spam).`),
    onError: (error) => toast.error(error instanceof Error ? error.message : "Probni mejl nije poslat"),
  });

  if (status.isPending) return null;
  const data = status.data;
  const enabled = data?.enabled ?? false;

  return (
    <div
      className={`flex flex-wrap items-center gap-x-4 gap-y-3 rounded-lg border p-4 text-sm ${
        enabled ? "border-emerald-500/30 bg-emerald-500/5" : "border-amber-500/40 bg-amber-500/5"
      }`}
    >
      {enabled ? (
        <MailCheckIcon className="size-5 shrink-0 text-emerald-600" aria-hidden />
      ) : (
        <MailWarningIcon className="size-5 shrink-0 text-amber-600" aria-hidden />
      )}
      <div className="min-w-0 flex-1">
        {enabled ? (
          <>
            <p className="font-medium">Svaka nova prijava stiže i mejlom na {data?.recipients.join(", ")}</p>
            {data?.sender && <p className="text-muted-foreground">Pošiljalac: {data.sender}</p>}
          </>
        ) : (
          <>
            <p className="font-medium">Mejl o novim prijavama se ne šalje — SMTP nije podešen na serveru</p>
            <p className="text-muted-foreground">
              Prijave se i dalje upisuju ovde. Kad se podesi, obaveštenja idu na{" "}
              {data?.recipients.join(", ") || "adresu iz APPLICATIONS_MAIL_TO"}.
            </p>
          </>
        )}
      </div>
      <Button variant="outline" size="sm" onClick={() => test.mutate()} disabled={!enabled || test.isPending}>
        <SendIcon />
        {test.isPending ? "Šaljem…" : "Pošalji probni mejl"}
      </Button>
    </div>
  );
}
