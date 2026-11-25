import Image from "next/image";
import { ArrowLink } from "./arrow-button";
import { SIGNUP_HREF } from "./nav";

/** Završni poziv „Dokaži da si najbolji!" — isti blok stoji na dnu početne i kontakt strane. */
export function CtaSection() {
  return (
    <section className="relative isolate flex min-h-[532px] items-center justify-center overflow-hidden px-5 py-20 text-center font-site text-white xl:min-h-[523px]">
      <Image src="/site/cta.png" alt="" fill sizes="100vw" className="-z-20 object-cover" />
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(100%_66%_at_50%_50%,rgba(0,0,0,0.82)_25%,rgba(0,0,0,0)_79%)] xl:bg-[radial-gradient(48%_50%_at_50%_50%,rgba(0,0,0,0.55)_0%,rgba(0,0,0,0.5)_41%,rgba(0,0,0,0.35)_70%,rgba(0,0,0,0)_100%)]" />

      <div className="flex flex-col items-center">
        <h2 className="text-4xl leading-[1.3] font-medium tracking-[-0.03em] xl:text-[52px]">Dokaži da si najbolji!</h2>
        <p className="mt-4 max-w-[544px] text-xl leading-[1.5] font-bold tracking-[0.06em]">
          Prijavi se još danas. Mesta su ograničena.
        </p>
        <div className="mt-8">
          <ArrowLink href={SIGNUP_HREF}>Prijavi se u ligu</ArrowLink>
        </div>
      </div>
    </section>
  );
}
