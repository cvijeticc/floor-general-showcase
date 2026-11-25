"use client";

import { MotionConfig, motion } from "motion/react";

/** Framer „tween" kriva sa originala — koristi se za sve ulazne animacije na sajtu. */
export const FRAMER_EASE = [0.44, 0, 0.56, 1] as const;

/** Poštuje „smanji pokrete" iz podešavanja sistema: transformacije se gase, fade ostaje. */
export function SiteMotion({ children }: { children: React.ReactNode }) {
  return <MotionConfig reducedMotion="user">{children}</MotionConfig>;
}

/**
 * Ulaz pri učitavanju strane: providno → vidljivo uz klizanje 20px naviše (Framer „appear").
 * Vrednosti (0,5 s, kriva 0.44/0/0.56/1, kašnjenja 0,8–1,4 s) su procitane sa originala.
 */
export function Appear({
  delay = 0,
  className,
  children,
}: {
  delay?: number;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.5, ease: FRAMER_EASE }}
    >
      {children}
    </motion.div>
  );
}

/** Isto kao Appear, ali okida kad element uđe u ekran (za sekcije ispod prevoja). */
export function InView({
  delay = 0,
  className,
  children,
}: {
  delay?: number;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "0px 0px -10% 0px" }}
      transition={{ delay, duration: 0.5, ease: FRAMER_EASE }}
    >
      {children}
    </motion.div>
  );
}
