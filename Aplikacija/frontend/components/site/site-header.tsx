"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { CONTAINER, NAV_ITEMS, SIGNUP_HREF } from "./nav";

/**
 * Zaglavlje javnog sajta. Desktop: crna traka 93px, logo | linkovi, dugme „Prijavi se" desno;
 * hover na linku izvlači liniju ispod teksta sleva nadesno, a „Rezultati" su belo dugme.
 * Telefon/tablet: burger, beli panel.
 */
export function SiteHeader() {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();

  // Posle klika na stavku (i na sidro na istoj strani) meni se zatvara.
  useEffect(() => setOpen(false), [pathname]);

  return (
    <header className="sticky top-0 z-50 bg-black font-site text-white">
      <div className={`${CONTAINER} flex h-[53px] items-center justify-between xl:h-[93px]`}>
        <div className="flex items-center">
          <Logo />
          <span aria-hidden className="mx-8 hidden h-6 w-0.5 bg-white/50 xl:block" />
          <nav aria-label="Glavni meni" className="hidden items-center gap-6 xl:flex 2xl:gap-8">
            {NAV_ITEMS.map((item) =>
              item.highlight ? (
                // -mx-1: dugme se širi u razmak između stavki, pa se ostatak menija pomera malo.
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={isCurrent(pathname, item.href) ? "page" : undefined}
                  className={
                    item.highlight === "playoff"
                      ? "-mx-1 inline-flex items-center gap-2 rounded-full bg-playoff px-4 py-2 text-base font-bold text-white transition-colors duration-300 hover:bg-playoff-dark"
                      : "-mx-1 rounded-full bg-white px-4 py-2 text-base text-black transition-colors duration-300 hover:bg-[#cccccc]"
                  }
                >
                  {item.highlight === "playoff" && <LiveDot />}
                  {item.label}
                </Link>
              ) : (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`group relative whitespace-nowrap py-2 text-base ${item.wideOnly ? "hidden 2xl:block" : ""}`}
                >
                  {item.label}
                  <span
                    aria-hidden
                    className="absolute -left-0.5 -bottom-1.5 h-0.5 w-[calc(100%+4px)] origin-left scale-x-0 bg-white transition-transform duration-500 ease-[var(--ease-out-strong)] group-hover:scale-x-100"
                  />
                </Link>
              ),
            )}
          </nav>
        </div>

        <Link
          href={SIGNUP_HREF}
          className="hidden shrink-0 whitespace-nowrap rounded-full bg-white px-5 py-2 text-base font-bold text-black transition-colors duration-300 hover:bg-[#cccccc] xl:block"
        >
          Prijavi se
        </Link>

        <div className="flex items-center gap-1 xl:hidden">
          <Link
            href="/playoff"
            className="inline-flex items-center gap-1.5 rounded-full bg-playoff px-3 py-1.5 text-sm font-bold text-white"
          >
            <LiveDot />
            Playoff
          </Link>
          <button
            type="button"
            aria-label={open ? "Zatvori meni" : "Otvori meni"}
            aria-expanded={open}
            onClick={() => setOpen((o) => !o)}
            className="relative -mr-2 flex size-11 flex-col items-end justify-center gap-2"
          >
            <span className="h-0.5 w-[22px] rounded-full bg-white" />
            <span className="h-0.5 w-4 rounded-full bg-white" />
          </button>
        </div>
      </div>

      <AnimatePresence>
        {open && (
          <motion.div
            key="mobile-menu"
            initial={{ opacity: 0, y: -16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -16 }}
            transition={{ duration: 0.3, ease: [0.44, 0, 0.56, 1] }}
            className="fixed inset-x-0 top-0 z-50 max-h-dvh overflow-y-auto bg-white px-5 pb-10 text-black shadow-xl xl:hidden"
          >
            <div className="flex h-[93px] items-center justify-between border-b border-black/10">
              <Logo dark />
              <button
                type="button"
                aria-label="Zatvori meni"
                onClick={() => setOpen(false)}
                className="-mr-2 flex size-11 items-center justify-center"
              >
                <svg viewBox="0 0 24 24" className="size-6" aria-hidden>
                  <path d="M4 4l16 16M20 4L4 20" stroke="currentColor" strokeWidth="2" />
                </svg>
              </button>
            </div>
            <nav aria-label="Glavni meni" className="flex flex-col items-center gap-2 pt-8">
              {NAV_ITEMS.map((item, i) => (
                <motion.div
                  key={item.href}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.05 + i * 0.03, duration: 0.3 }}
                >
                  <Link
                    href={item.href}
                    onClick={() => setOpen(false)}
                    className={
                      item.highlight === "playoff"
                        ? "mt-1 inline-flex items-center gap-2 rounded-full bg-playoff px-6 py-3 text-lg font-bold text-white"
                        : item.highlight === "white"
                          ? "block px-4 py-3 text-lg font-bold"
                          : "block px-4 py-3 text-lg"
                    }
                  >
                    {item.highlight === "playoff" && <LiveDot />}
                    {item.label}
                  </Link>
                </motion.div>
              ))}
              <Link
                href={SIGNUP_HREF}
                onClick={() => setOpen(false)}
                className="mt-6 rounded-full bg-black px-10 py-4 text-lg font-bold text-white"
              >
                Prijavi se
              </Link>
            </nav>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}

function isCurrent(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(href + "/");
}

/** Tačka „uživo" na dugmetu Playoff — pulsira dok traje playoff; bez animacije za prefers-reduced-motion. */
function LiveDot() {
  return (
    <span aria-hidden className="relative flex size-2">
      <span className="absolute inline-flex size-full animate-ping rounded-full bg-white/80 motion-reduce:animate-none" />
      <span className="relative inline-flex size-2 rounded-full bg-white" />
    </span>
  );
}

export function Logo({ dark = false }: { dark?: boolean }) {
  return (
    <Link href="/#hero" className="flex shrink-0 items-center gap-1.5" aria-label="Floor General — početna">
      <Image
        src={dark ? "/site/logo-tamni.png" : "/site/logo.png"}
        alt=""
        width={dark ? 55 : 47}
        height={47}
        className="h-[47px] w-auto object-contain"
        priority
      />
      <span className={`whitespace-nowrap text-base font-bold ${dark ? "text-black" : "text-white"}`}>Floor General</span>
    </Link>
  );
}
