"use client";

import { animate, useInView, useReducedMotion } from "motion/react";
import { useEffect, useRef } from "react";

/** Broj koji odbroji od 0 do `to` kad uđe u ekran (brojači „200+" na originalu). */
export function CountUp({ to, suffix = "+" }: { to: number; suffix?: string }) {
  const ref = useRef<HTMLSpanElement>(null);
  const inView = useInView(ref, { once: true, margin: "0px 0px -15% 0px" });
  const reduce = useReducedMotion();

  useEffect(() => {
    const el = ref.current;
    if (!el || reduce) return;
    // Do ulaska u ekran stoji 0; tekst u HTML-u (SSR) ostaje prava vrednost.
    if (!inView) {
      el.textContent = `0${suffix}`;
      return;
    }
    const controls = animate(0, to, {
      duration: 2,
      ease: [0.12, 0.23, 0.5, 1],
      onUpdate: (v) => (el.textContent = `${Math.round(v)}${suffix}`),
    });
    return () => controls.stop();
  }, [inView, reduce, to, suffix]);

  // SSR i pretraživači vide pravu vrednost; animacija je samo ukras.
  return (
    <span ref={ref} className="tabular-nums">
      {`${to}${suffix}`}
    </span>
  );
}
