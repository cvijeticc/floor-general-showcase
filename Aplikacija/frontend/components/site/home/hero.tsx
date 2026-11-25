"use client";

import Image from "next/image";
import { motion, useMotionValue, useReducedMotion, useScroll, useTransform } from "motion/react";
import { useEffect, useRef } from "react";
import { ArrowLink } from "../arrow-button";
import { Appear } from "../motion";
import { CONTAINER, SIGNUP_HREF } from "../nav";

const TITLE = "1 na 1 Liga";

/**
 * Hero početne strane. Kao na originalu:
 * - slika ulazi iz scale 1.05 za 2 s, tekst se pojavljuje redom (0,8 / 1,0 / 1,2 / 1,4 s)
 * - naslov ulazi slovo po slovo: blur 6px → 0, scale 1.4 → 1 (spring), ~33 ms razmaka
 * - cela sekcija se pri skrolu pomera upola sporije od strane (parallax 0.5)
 *
 * Bolje od originala: tekst ide parallax-om samo dok ne dodirne dno sekcije, pa nastavlja sa
 * stranom. Na originalu ga `overflow-hidden` seče, pa se na laptopu (prozor ~550px) 2. i 3. mesto
 * nikad ne vide.
 */
export function Hero() {
  const reduce = useReducedMotion();
  const { scrollY } = useScroll();
  const sectionRef = useRef<HTMLElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);
  // Koliko tekst sme da se spusti a da ga donja ivica sekcije ne odseče.
  const maxShift = useMotionValue(0);

  useEffect(() => {
    const section = sectionRef.current;
    const content = contentRef.current;
    if (!section || !content) return;
    // offsetTop/offsetHeight ne vide transform, pa sam parallax ne utiče na merenje.
    const measure = () =>
      maxShift.set(Math.max(0, section.offsetHeight - content.offsetTop - content.offsetHeight));
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(section);
    observer.observe(content);
    return () => observer.disconnect();
  }, [maxShift]);

  const backgroundY = useTransform(scrollY, (v) => (reduce ? 0 : v * 0.5));
  const contentY = useTransform([scrollY, maxShift], ([v, max]: number[]) => (reduce ? 0 : Math.min(v * 0.5, max)));

  return (
    <section
      ref={sectionRef}
      id="hero"
      className="relative isolate min-h-[765px] overflow-hidden bg-black font-site text-white xl:h-svh xl:min-h-[860px]"
    >
      <motion.div style={{ y: backgroundY }} className="absolute inset-0 -z-10">
        <motion.div
          className="absolute inset-0"
          initial={{ opacity: 0, scale: 1.05 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 2, ease: [0.12, 0.23, 0.5, 1] }}
        >
          <Image
            src="/site/hero.png"
            alt=""
            fill
            priority
            sizes="100vw"
            className="object-cover object-[72%_50%] xl:object-center"
          />
        </motion.div>
        <div className="absolute inset-0 bg-[linear-gradient(rgba(0,0,0,0)_0%,rgba(0,0,0,0.48)_34.7%,rgba(3,3,3,0.77)_53.6%,rgba(3,3,3,0.79)_76.6%,rgba(0,0,0,0.85)_100%)] xl:bg-[radial-gradient(33%_42%_at_69%_59.4%,rgba(0,0,0,0)_0%,rgba(0,0,0,0.4)_100%)]" />
      </motion.div>

      {/* 90px kao na originalu (1280 × 551 i 1280 × 495: tekst počinje 90px ispod menija). */}
      <motion.div ref={contentRef} style={{ y: contentY }} className={`${CONTAINER} pb-16 pt-[67px] xl:pt-[90px]`}>
        <Appear delay={0.8} className="flex items-center gap-3">
          <span aria-hidden className="h-px w-12 bg-white" />
          <span className="text-base">Floor General</span>
        </Appear>

        <h1
          aria-label={TITLE}
          className="mt-5 text-[54px] font-extrabold leading-[1.2] tracking-[-0.05em] xl:mt-4 xl:text-[88px]"
        >
          <AnimatedLetters text={TITLE} />
        </h1>

        <Appear delay={1} className="mt-6 max-w-[420px] text-xl leading-[1.5] font-semibold xl:mt-4">
          <p>
            Dokaži da si najbolji i osvoji 500 000 dinara.
            <br />
            <strong className="font-black">Prva 1 na 1 liga u Srbiji.</strong>
          </p>
        </Appear>

        <Appear delay={1.2} className="mt-8 xl:mt-[42px]">
          <ArrowLink href={SIGNUP_HREF}>Prijavi se u ligu</ArrowLink>
        </Appear>

        <Appear delay={1.4} className="mt-12 xl:mt-16">
          <ol className="border-l-2 border-white/50 py-0.5 pl-6 text-2xl leading-[1.5] xl:pl-6">
            <PrizeRow
              place={1}
              amount="500.000"
              className="xl:text-[54px]"
              amountClassName="font-extrabold text-prize-gold xl:text-[61px]"
            />
            <PrizeRow place={2} amount="100.000" className="xl:text-[47px]" amountClassName="font-bold text-prize-silver" />
            <PrizeRow place={3} amount="50.000" className="xl:text-[39px]" amountClassName="font-semibold text-prize-bronze" />
          </ol>
        </Appear>
      </motion.div>
    </section>
  );
}

function PrizeRow({
  place,
  amount,
  className,
  amountClassName,
}: {
  place: number;
  amount: string;
  className: string;
  amountClassName: string;
}) {
  return (
    <li className={`flex ${className}`}>
      <span className="w-[1.2em] shrink-0 xl:w-[64px]">{place}.</span>
      <span>
        Mesto - <span className={amountClassName}>{amount}</span> dinara
      </span>
    </li>
  );
}

/** Naslov slovo po slovo; razmaci ostaju pravi razmaci da bi se reči normalno prelamale. */
function AnimatedLetters({ text }: { text: string }) {
  // Redni broj slova kroz ceo naslov (preskače razmake) -- od njega zavisi kašnjenje.
  const words = text.split(" ").map((word, w, all) => ({
    word,
    offset: all.slice(0, w).join("").length,
  }));

  return (
    <span aria-hidden>
      {words.map(({ word, offset }, w) => (
        <span key={w}>
          {w > 0 && " "}
          <span className="inline-block whitespace-nowrap">
            {[...word].map((ch, c) => {
              const delay = (offset + c) * 0.033;
              return (
                <motion.span
                  key={c}
                  className="inline-block"
                  initial={{ opacity: 0, scale: 1.4, filter: "blur(6px)" }}
                  animate={{ opacity: 1, scale: 1, filter: "blur(0px)" }}
                  transition={{
                    scale: { type: "spring", stiffness: 380, damping: 30, delay },
                    opacity: { duration: 0.35, delay },
                    filter: { duration: 0.45, delay },
                  }}
                >
                  {ch}
                </motion.span>
              );
            })}
          </span>
        </span>
      ))}
    </span>
  );
}
