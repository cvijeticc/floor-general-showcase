"use client";

import { MenuIcon, XIcon } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { cn } from "cn";
import { Button } from "@/components/ui/button";
import { LogoutButton } from "./logout-button";
import { UndoButton } from "./undo-button";

/** Ekrani koji tek dolaze (UC-FE-03…08) se dodaju u ovu navigaciju. */
const NAVIGATION = [
  { href: "/admin", label: "Početna" },
  { href: "/admin/igraci", label: "Igrači" },
  { href: "/admin/sezone", label: "Sezone" },
  { href: "/admin/grupe", label: "Grupe" },
  { href: "/admin/playoff", label: "Playoff" },
  { href: "/admin/mecevi", label: "Mečevi" },
  { href: "/admin/tabele", label: "Tabele" },
  { href: "/admin/prijave", label: "Prijave" },
];

function isActive(pathname: string, href: string) {
  return href === "/admin" ? pathname === href : pathname === href || pathname.startsWith(href + "/");
}

/**
 * Zaglavlje admin panela. Na sirokom ekranu navigacija stoji u redu; na telefonu je iza burger
 * dugmeta, jer unos rezultata uvece cesto ide sa telefona (CLAUDE.md §3.1).
 */
export function AdminNav() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  // Izbor stranice zatvara meni.
  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!open) {
      return;
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open]);

  return (
    <header className="sticky top-0 z-40 border-b bg-background/85 backdrop-blur-md supports-[backdrop-filter]:bg-background/70">
      <div className="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4">
        <Link href="/admin" className="shrink-0 text-[15px] font-semibold tracking-tight whitespace-nowrap">
          Floor General
          <span className="ml-1.5 font-normal text-muted-foreground">admin</span>
        </Link>

        <nav className="hidden items-center gap-0.5 text-sm md:flex xl:gap-1">
          {NAVIGATION.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "whitespace-nowrap rounded-md px-2 py-1.5 text-muted-foreground transition-colors duration-150 ease-out hover:text-foreground xl:px-2.5",
                isActive(pathname, item.href) && "bg-muted font-medium text-foreground",
              )}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="ml-auto flex min-w-0 items-center gap-2">
          <UndoButton />
          <div className="hidden md:block">
            <LogoutButton />
          </div>
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            aria-label={open ? "Zatvori meni" : "Otvori meni"}
            aria-expanded={open}
            aria-controls="admin-mobile-menu"
            onClick={() => setOpen((value) => !value)}
          >
            <span className="relative size-5">
              <MenuIcon
                className={cn(
                  "absolute inset-0 size-5 transition-[opacity,transform] duration-200 ease-(--ease-out-strong)",
                  open ? "scale-75 rotate-90 opacity-0" : "opacity-100",
                )}
              />
              <XIcon
                className={cn(
                  "absolute inset-0 size-5 transition-[opacity,transform] duration-200 ease-(--ease-out-strong)",
                  open ? "opacity-100" : "scale-75 -rotate-90 opacity-0",
                )}
              />
            </span>
          </Button>
        </div>
      </div>

      {/* Mobilni meni: pozadina hvata dodir pored menija, panel klizi odozgo. */}
      <div
        className={cn(
          "fixed inset-x-0 top-14 bottom-0 bg-black/20 transition-opacity duration-200 ease-out md:hidden",
          open ? "opacity-100" : "pointer-events-none opacity-0",
        )}
        onClick={() => setOpen(false)}
        aria-hidden
      />
      <div
        id="admin-mobile-menu"
        inert={!open}
        className={cn(
          "absolute inset-x-0 top-14 origin-top border-b bg-background shadow-lg transition-[opacity,transform] duration-250 ease-(--ease-out-strong) md:hidden",
          open ? "translate-y-0 opacity-100" : "pointer-events-none -translate-y-2 opacity-0",
        )}
      >
        <nav className="mx-auto flex max-w-6xl flex-col px-2 py-2">
          {NAVIGATION.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "rounded-lg px-3 py-3 text-base text-muted-foreground transition-colors duration-150 active:bg-muted",
                isActive(pathname, item.href) && "bg-muted font-medium text-foreground",
              )}
            >
              {item.label}
            </Link>
          ))}
          <div className="mt-2 border-t px-3 pt-3 pb-1">
            <LogoutButton />
          </div>
        </nav>
      </div>
    </header>
  );
}
