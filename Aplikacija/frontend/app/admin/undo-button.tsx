"use client";

import { useCallback, useEffect } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { errorMessage } from "@/lib/api/client";
import { useLastUndoable, useUndo } from "@/lib/api/use-undo";

/**
 * UC-ADM-23 — „Poništi" (Ctrl+Z) za poslednju izmenu.
 * <p>
 * Stek je na serveru, pa dugme radi i posle osvežavanja strane, i sutradan: pokazuje šta će
 * tačno da poništi, a uzastopni klik ide korak dublje.
 */
export function UndoButton() {
  const last = useLastUndoable();
  const undo = useUndo();

  const run = useCallback(async () => {
    if (undo.isPending) {
      return;
    }
    try {
      const info = await undo.mutateAsync();
      toast.success("Poništeno: " + (info.description ?? "poslednja izmena"));
    } catch (error) {
      toast.error(errorMessage(error, "Poništavanje nije uspelo"));
    }
  }, [undo]);

  // Ctrl+Z (Cmd+Z na Macu). Dok je fokus u polju za unos, precica ostaje pregledacu —
  // tamo Ctrl+Z vraca otkucani tekst, a ne poslednju izmenu u bazi.
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key.toLowerCase() !== "z" || !(event.ctrlKey || event.metaKey) || event.shiftKey) {
        return;
      }
      if (isTextEntry(event.target)) {
        return;
      }
      event.preventDefault();
      void run();
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [run]);

  const entry = last.data;
  const disabled = undo.isPending || last.isLoading || !entry;

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={run}
      disabled={disabled}
      title={entry ? "Ctrl+Z — " + entry.description : "Nema izmene koja se može poništiti"}
      className="h-9 min-w-0 md:h-8 md:max-w-40 xl:max-w-56 2xl:max-w-72"
    >
      <span aria-hidden>↶</span>
      {/* Na telefonu opis ne staje u zaglavlje i gura stranu u stranu — vidi se u obaveštenju posle klika. */}
      <span className="truncate">
        Poništi
        {entry ? <span className="hidden md:inline">: {entry.description}</span> : null}
      </span>
    </Button>
  );
}

function isTextEntry(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false;
  }
  return (
    target.isContentEditable ||
    target instanceof HTMLInputElement ||
    target instanceof HTMLTextAreaElement ||
    target instanceof HTMLSelectElement
  );
}
