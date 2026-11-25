import Image from "next/image";
import { ANCHOR_TARGET, CONTAINER } from "../nav";
import { CountUp } from "./count-up";

/**
 * Brojevi ispod liste. Original je imao 200+ / 12+ / 100+ (plan za sezonu sa 200 igrača);
 * ovde su stvarni brojevi sezone 2026/2027: 100 igrača, 13 nedelja (19.09.–20.12.),
 * ~60 mečeva subotom i ~60 nedeljom. Bez „+" (zahtev lige 11.11.2026.) — to su tačni brojevi.
 */
const STATS = [
  { value: 100, label: "Broj igrača" },
  { value: 13, label: "Nedelja" },
  { value: 60, label: "Utakmica dnevno" },
];

export function Offer() {
  return (
    <section className="bg-white bg-[linear-gradient(#fff_2.7%,rgba(237,237,237,0.76)_47.3%,rgba(0,0,0,0.47)_100%)] pb-[104px] pt-16 font-site text-black xl:pb-[100px] xl:pt-[100px]">
      <div className={CONTAINER}>
        <h2 id="sta-nudimo" className={`${ANCHOR_TARGET} text-center text-[42px] font-bold leading-[1.2]`}>
          ŠTA NUDIMO
        </h2>

        <div className="mt-[62px] flex flex-col gap-12 lg:mt-[93px] lg:flex-row lg:items-center lg:justify-between">
          <ol className="max-w-[847px] list-decimal pl-[31px] text-[25px] leading-[1.4] font-medium tracking-[-0.03em] lg:pl-10 lg:text-[32px] lg:leading-[1.3]">
            <li>
              Nagradni fond za prva 3 mesta - <b className="font-bold">650.000</b> dinara
            </li>
            <li>
              Live stream na <b className="font-bold lg:font-medium">YouTube</b>-u svakog odigranog meča
            </li>
            <li>
              Najbolji mečevi će biti postavljeni na sve naše društvene mreže (
              <b className="font-bold">Instagram, TikTok, YouTube Shorts</b>)
            </li>
            <li>
              Besplatan <b className="font-bold">parking</b>
            </li>
            <li>Moderna sala (Hoop and Pump)</li>
            <li>Sudije, zapisnički sto</li>
            <li>Profesionalno obezbeđenje</li>
          </ol>

          <div className="relative h-[210px] w-full overflow-hidden rounded-2xl lg:h-[145px] lg:w-[242px] lg:shrink-0">
            <Image src="/site/logo-tamni.png" alt="Floor General — 1 on 1 League" fill sizes="350px" className="object-contain" />
          </div>
        </div>

        <dl className="mt-24 grid grid-cols-2 gap-x-8 gap-y-12 lg:mt-[80px] lg:flex lg:gap-16">
          {STATS.map((s) => (
            <div key={s.label} className="flex flex-col-reverse border-t border-white/70 pt-11 lg:w-[232px]">
              <dt className="mt-2 text-base">{s.label}</dt>
              <dd className="text-[40px] leading-none font-medium lg:text-[52px]">
                <CountUp to={s.value} suffix="" />
              </dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
