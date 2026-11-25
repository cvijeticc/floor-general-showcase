"use client";

import { Fragment, useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { errorMessage } from "@/lib/api/client";
import {
  SKILL_LABELS,
  STATUS_LABELS,
  useAcceptApplication,
  useApplications,
  useChangeApplicationStatus,
  useDeleteApplication,
  type ApplicationResult,
  type ApplicationStatus,
} from "./use-applications";

const STATUSES: ApplicationStatus[] = ["NEW", "CONTACTED", "ACCEPTED", "REJECTED"];

export function ApplicationsTable() {
  const [status, setStatus] = useState<ApplicationStatus | "">("");
  const [expanded, setExpanded] = useState<number | null>(null);
  const applications = useApplications(status);
  const changeStatus = useChangeApplicationStatus();
  const accept = useAcceptApplication();
  const remove = useDeleteApplication();

  const rows = applications.data ?? [];

  async function run(action: () => Promise<unknown>, success: string, failure: string) {
    try {
      await action();
      toast.success(success);
    } catch (error) {
      toast.error(errorMessage(error, failure));
    }
  }

  return (
    <div className="space-y-4">
      <div className="space-y-1.5">
        <Label htmlFor="status-prijave">Status</Label>
        <select
          id="status-prijave"
          className="h-9 w-44 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
          value={status}
          onChange={(e) => setStatus(e.target.value as ApplicationStatus | "")}
        >
          <option value="">Sve prijave</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </select>
      </div>

      <div className="rounded-lg border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Stigla</TableHead>
              <TableHead>Nadimak</TableHead>
              <TableHead>Ime i prezime</TableHead>
              <TableHead>Telefon</TableHead>
              <TableHead className="text-right">Godište</TableHead>
              <TableHead>Nivo</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Radnje</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {applications.isLoading ? (
              <MessageRow>Učitavanje…</MessageRow>
            ) : applications.error ? (
              <MessageRow variant="error">{errorMessage(applications.error, "Prijave nisu učitane")}</MessageRow>
            ) : rows.length === 0 ? (
              <MessageRow>{status ? "Nema prijava sa ovim statusom" : "Još nema prijava sa sajta"}</MessageRow>
            ) : (
              rows.map((a) => (
                <Fragment key={a.id}>
                  <TableRow
                    className="cursor-pointer"
                    onClick={() => setExpanded((id) => (id === a.id ? null : (a.id ?? null)))}
                  >
                    <TableCell className="whitespace-nowrap text-muted-foreground tabular-nums">
                      {formatDateTime(a.createdAt)}
                    </TableCell>
                    <TableCell className="font-medium">{a.nickname}</TableCell>
                    <TableCell>{a.fullName}</TableCell>
                    <TableCell className="tabular-nums">
                      <a href={`tel:${a.phone}`} onClick={(e) => e.stopPropagation()} className="hover:underline">
                        {a.phone}
                      </a>
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{a.birthYear ?? "—"}</TableCell>
                    <TableCell>{a.skillLevel ? SKILL_LABELS[a.skillLevel] : "—"}</TableCell>
                    <TableCell>
                      <StatusBadge status={a.status} />
                    </TableCell>
                    <TableCell className="text-right whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                      {a.status !== "ACCEPTED" && (
                        <Button
                          size="sm"
                          variant="ghost"
                          disabled={accept.isPending}
                          onClick={() =>
                            run(
                              () => accept.mutateAsync(a.id!),
                              `${a.nickname} je dodat u igrače — rasporedi ga u grupu na ekranu „Grupe"`,
                              "Igrač nije napravljen",
                            )
                          }
                        >
                          Napravi igrača
                        </Button>
                      )}
                      {a.status === "NEW" && (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() =>
                            run(
                              () => changeStatus.mutateAsync({ id: a.id!, status: "CONTACTED" }),
                              `${a.nickname}: kontaktiran`,
                              "Status nije promenjen",
                            )
                          }
                        >
                          Kontaktiran
                        </Button>
                      )}
                      {(a.status === "NEW" || a.status === "CONTACTED") && (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() =>
                            run(
                              () => changeStatus.mutateAsync({ id: a.id!, status: "REJECTED" }),
                              `${a.nickname}: odbijen`,
                              "Status nije promenjen",
                            )
                          }
                        >
                          Odbij
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() =>
                          run(() => remove.mutateAsync(a.id!), `Prijava ${a.fullName} je obrisana`, "Prijava nije obrisana")
                        }
                      >
                        Obriši
                      </Button>
                    </TableCell>
                  </TableRow>
                  {expanded === a.id && <DetailsRow application={a} />}
                </Fragment>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <p className="text-sm text-muted-foreground">
        {rows.length} {rows.length === 1 ? "prijava" : "prijava"}. Klik na red otvara sve podatke.
      </p>
    </div>
  );
}

function DetailsRow({ application: a }: { application: ApplicationResult }) {
  return (
    <TableRow className="bg-muted/40 hover:bg-muted/40">
      <TableCell colSpan={8}>
        <dl className="grid gap-x-8 gap-y-2 py-2 text-sm sm:grid-cols-2 lg:grid-cols-4">
          <Detail label="Email">
            <a href={`mailto:${a.email}`} className="hover:underline">
              {a.email}
            </a>
          </Detail>
          <Detail label="Visina / težina">
            {a.heightCm ?? "—"} cm / {a.weightKg ?? "—"} kg
          </Detail>
          <Detail label="Napomena" wide>
            {a.note ?? "—"}
          </Detail>
          <Detail label="O sebi" wide>
            {a.about ?? "—"}
          </Detail>
        </dl>
      </TableCell>
    </TableRow>
  );
}

function Detail({ label, wide, children }: { label: string; wide?: boolean; children: React.ReactNode }) {
  return (
    <div className={wide ? "sm:col-span-2" : undefined}>
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="whitespace-pre-wrap">{children}</dd>
    </div>
  );
}

function StatusBadge({ status }: { status: ApplicationResult["status"] }) {
  if (!status) return null;
  const variant = status === "NEW" ? "default" : status === "REJECTED" ? "outline" : "secondary";
  return <Badge variant={variant}>{STATUS_LABELS[status]}</Badge>;
}

/** „2026-10-05T17:03:15" → „05.10.2026. 17:03" (vreme je već beogradsko, sa backenda). */
function formatDateTime(iso?: string) {
  if (!iso) return "—";
  const [y, m, d] = iso.slice(0, 10).split("-");
  return `${d}.${m}.${y}. ${iso.slice(11, 16)}`;
}

function MessageRow({ children, variant }: { children: React.ReactNode; variant?: "error" }) {
  return (
    <TableRow>
      <TableCell
        colSpan={8}
        className={"py-8 text-center text-sm " + (variant === "error" ? "text-destructive" : "text-muted-foreground")}
        role={variant === "error" ? "alert" : undefined}
      >
        {children}
      </TableCell>
    </TableRow>
  );
}
