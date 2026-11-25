"use client";

import Image from "next/image";
import Link from "next/link";
import { motion, useMotionValueEvent, useScroll, useTransform } from "motion/react";
import { useRef, useState } from "react";
import { ANCHOR_TARGET, CONTAINER } from "../nav";

const IMAGES = ["/site/kako-1.png", "/site/kako-2.png", "/site/kako-3.png"];

/**
 * Format sezone 2026/2027 (potvrdio naručilac 05.10.2026.): jedna grupna faza 10 × 10,
 * dvokružno, 6 direktno + 4 najbolja sedmoplasirana = 64 → knockout na 2 pobede → Final Four.
 * Original je opisivao plan za 200 igrača (dve grupne faze) — vidi CLAUDE.md §6.6.
 * Playoff deo je usklađen sa obaveštenjem lige igračima (08. i 09.11.2026.).
 */
const BLOCKS: { key: string; body: React.ReactNode }[] = [
  {
    key: "grupna",
    body: (
      <>
        <p>
          Liga počinje od <b>19.09.2026</b> i završava se <b>20.12.2026.</b>
        </p>
        <p>
          1.&nbsp; <b>GRUPNA FAZA</b> (19. septembar - 8. novembar) — završena
          <br />
          <b>100</b> igrača je podeljeno u <b>10 grupa</b> od po 10 igrača
        </p>
        <p>
          Svako igra sa svakim iz grupe dva puta, što znači da ćeš u grupi odigrati <b>18 mečeva</b>
        </p>
        <p>
          Najboljih <b>6</b> iz svake grupe i <b>4 najbolja sedmoplasirana</b> igrača prolaze u knockout fazu
        </p>
        <p>
          VAŽNO - UTAKMICE SE IGRAJU SAMO <b>SUBOTOM I NEDELJOM</b> kako bi svi prijavljeni mogli da igraju
        </p>
        <p>
          Igraćeš subotom ili nedeljom i tog dana ćeš odigrati dva meča. Tvoja grupa će završiti u roku od 1h i
          15min
        </p>
      </>
    ),
  },
  {
    key: "knockout",
    body: (
      <>
        <p>
          2.&nbsp; <b>KNOCKOUT FAZA — PLAYOFF</b> (počinje <b>14. i 15. novembra</b>)
        </p>
        <p>
          Najboljih <b>64</b> igrača se ukrštaju u knockout fazi i do četvrtfinala se igra na{" "}
          <b>2 dobijene utakmice</b> — ceo krug istog dana, dva ili tri meča
        </p>
        <p>Pobednik serije ide dalje, poraženi ispada: 64 → 32 → 16 → 8 → 4</p>
        <p>
          Od <b>Top 32</b> se igra samo na terenu A, sa dve sudije, komentatorom i prenosom uživo. Od{" "}
          <b>Top 16</b> igrači ne plaćaju ništa do kraja takmičenja
        </p>
        <p>
          <Link href="/playoff" className="font-bold underline underline-offset-4">
            Pogledaj drvo playoff-a →
          </Link>
        </p>
      </>
    ),
  },
  {
    key: "final-four",
    body: (
      <>
        <p>
          3.&nbsp; <b>FINAL FOUR</b> (do 20. decembra)
        </p>
        <p>
          4 najbolja igrača odlaze na <b>FINAL FOUR</b> gde se igra jedna utakmica, do <b>16 poena</b> ili{" "}
          <b>15 minuta</b>, uz meč za 3. mesto
        </p>
        <p>
          Pobednik turnira osvaja <b className="text-prize-gold">500.000</b> dinara, drugoplasirani{" "}
          <b className="text-prize-silver">100.000</b> dinara, a trećeplasirani{" "}
          <b className="text-prize-bronze">50.000</b> dinara
        </p>
      </>
    ),
  },
];

export function HowItWorks() {
  const blockRefs = useRef<(HTMLDivElement | null)[]>([]);
  const [active, setActive] = useState(0);
  const { scrollY } = useScroll();

  // Slika desno prati blok čiji je vrh prešao sredinu ekrana (na originalu ~300 ms crossfade).
  useMotionValueEvent(scrollY, "change", () => {
    const mid = window.innerHeight * 0.55;
    let next = 0;
    blockRefs.current.forEach((el, i) => {
      if (el && el.getBoundingClientRect().top < mid) next = i;
    });
    setActive((prev) => (prev === next ? prev : next));
  });

  return (
    <section className="bg-[linear-gradient(#000_0%,rgb(99,99,99)_100%)] pb-16 pt-16 font-site text-white xl:pb-[250px] xl:pt-[100px]">
      <div className={CONTAINER}>
        <h2
          id="kako-se-igra-liga"
          className={`${ANCHOR_TARGET} text-center text-[28px] font-bold leading-[1.2] xl:text-[42px]`}
        >
          KAKO SE IGRA LIGA
        </h2>

        <div className="mt-14 grid gap-[70px] xl:mt-[93px] xl:grid-cols-[544px_448px] xl:justify-between xl:gap-y-[100px]">
          <div className="flex flex-col gap-12 xl:gap-[100px]">
            {BLOCKS.map((block, i) => (
              <div key={block.key} ref={(el) => void (blockRefs.current[i] = el)}>
                <div className="relative mb-8 aspect-[334/256] overflow-hidden rounded-2xl xl:hidden">
                  <Image src={IMAGES[i]} alt="" fill sizes="(max-width: 1280px) 90vw, 448px" className="object-cover" />
                </div>
                <FadeBlock>{block.body}</FadeBlock>
              </div>
            ))}
            <ChangesNotice />
          </div>

          <div className="hidden xl:block">
            {/* 600px kao na originalu, ali nikad više od ekrana: na laptopu (prozor ~550px) donjih
                200px slike se inače nikad ne vidi. Kadar je pomeren naviše da lopta ostane u slici. */}
            <div className="sticky top-[150px] mt-[113px] h-[min(600px,calc(100svh-174px))] overflow-hidden rounded-2xl">
              {IMAGES.map((src, i) => (
                <motion.div
                  key={src}
                  className="absolute inset-0"
                  initial={false}
                  animate={{ opacity: i === active ? 1 : 0 }}
                  transition={{ duration: 0.3 }}
                >
                  <Image src={src} alt="" fill sizes="448px" className="object-cover object-[50%_25%]" />
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

/**
 * Napomena ispod rasporeda (zahtev lige 11.11.2026.): datumi su plan, a ako se nešto neočekivano
 * promeni, liga javlja igračima.
 */
function ChangesNotice() {
  return (
    <aside className="flex gap-4 rounded-2xl bg-white/[0.07] p-5 ring-1 ring-white/15 backdrop-blur-sm xl:p-6">
      <svg viewBox="0 0 24 24" aria-hidden className="mt-0.5 size-6 shrink-0 text-prize-gold" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
        <circle cx="12" cy="12" r="9.5" />
        <path d="M12 7.5v5.5M12 16.5h.01" />
      </svg>
      <p className="text-lg leading-[1.45] font-medium xl:text-xl">
        <b className="font-extrabold">Moguće su promene.</b> U slučaju neočekivanih promena termina ili rasporeda,
        sve igrače ćemo obavestiti na vreme.
      </p>
    </aside>
  );
}

/**
 * Tekst izranja dok se skroluje: providan kad mu vrh uđe na dno ekrana, pun kad ceo blok
 * stane u ekran (izmereno na originalu: opseg fade-a je jednak visini bloka).
 */
function FadeBlock({ children }: { children: React.ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({ target: ref, offset: ["start end", "end end"] });
  const opacity = useTransform(scrollYProgress, [0, 1], [0, 1]);

  return (
    <motion.div
      ref={ref}
      style={{ opacity }}
      className="space-y-[34px] text-2xl leading-[1.4] font-medium tracking-[-0.03em] xl:space-y-[38px] xl:text-[30px] xl:leading-[1.3] [&_b]:font-extrabold"
    >
      {children}
    </motion.div>
  );
}
