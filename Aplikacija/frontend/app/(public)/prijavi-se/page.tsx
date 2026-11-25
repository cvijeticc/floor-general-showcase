import type { Metadata } from "next";
import Link from "next/link";
import { CONTAINER } from "@/components/site/nav";
import { SignupWizard } from "./signup-wizard";

export const metadata: Metadata = {
  title: "Prijava u ligu",
  description:
    "Prijavi se u Floor General 1 na 1 ligu u četiri kratka koraka. Nagradni fond 650.000 dinara, Hoop and Pump sala, Surčin/Ledine, Beograd.",
  alternates: { canonical: "/prijavi-se" },
};

/**
 * Prijava u ligu (D-20) — odvojena od /kontakt: ovde je samo forma, u četiri koraka, pa na kraju
 * veliko „Prijavi se". Kontakt podaci i mapa su na /kontakt.
 */
export default function SignupPage() {
  return (
    <section className="relative isolate overflow-hidden bg-black pb-24 pt-14 font-site text-white xl:pb-[120px] xl:pt-[100px]">
      <div aria-hidden className="pointer-events-none absolute -left-40 top-10 -z-10 size-[460px] rounded-full bg-playoff/15 blur-3xl" />
      <div className={CONTAINER}>
        <div className="mx-auto max-w-[640px]">
        <p className="flex items-center gap-3 text-base text-white/70">
          <span aria-hidden className="h-px w-12 bg-white/70" />
          Floor General 1 na 1 liga
        </p>
        <h1 className="mt-4 text-[44px] font-bold leading-[1.1] tracking-[-0.04em] xl:text-[60px]">Prijava u ligu</h1>
        <p className="mt-4 text-lg text-white/65">
          Četiri kratka koraka, manje od dva minuta. Pitanja? Pozovi nas ili piši —{" "}
          <Link href="/kontakt" className="font-semibold text-white underline underline-offset-4">
            kontakt
          </Link>
          .
        </p>
        <div className="mt-10">
          <SignupWizard />
        </div>
        </div>
      </div>
    </section>
  );
}
