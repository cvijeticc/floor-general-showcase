import type { Metadata } from "next";
import { CtaSection } from "@/components/site/cta-section";
import { ArrowLink } from "@/components/site/arrow-button";
import { CONTAINER, SIGNUP_HREF } from "@/components/site/nav";
import { LegacySignupRedirect } from "./legacy-signup-redirect";

export const metadata: Metadata = {
  title: "Kontakt",
  description: "Kontaktiraj Floor General 1 na 1 ligu: telefon, mejl i adresa sale. Hoop and Pump sala, Surčin/Ledine, Beograd.",
  alternates: { canonical: "/kontakt" },
};

/** Samo kontakt podaci i mapa; prijava u ligu je na /prijavi-se. */
export default function ContactPage() {
  return (
    <>
      <LegacySignupRedirect />
      <section id="hero" className="bg-black pb-20 pt-16 font-site text-white xl:pb-[120px] xl:pt-[140px]">
        <div className={`${CONTAINER} grid gap-14 lg:grid-cols-[448px_480px] lg:items-start lg:justify-between`}>
          <div>
            <h1 className="text-[56px] leading-[1.2] font-medium tracking-[-0.05em] xl:text-[72px]">Kontakt</h1>
            <hr className="mt-10 border-[#555]" />

            <ul className="mt-10 space-y-6 text-base">
              <li className="flex items-center gap-3">
                <PhoneIcon />
                <a href="tel:+381603536222" className="hover:underline">
                  +381603536222
                </a>
              </li>
              <li className="flex items-center gap-3">
                <MailIcon />
                <a href="mailto:floorgeneral.rs@gmail.com" className="hover:underline">
                  floorgeneral.rs@gmail.com
                </a>
              </li>
              <li className="flex items-start gap-3">
                <PinIcon />
                <span>Hoop and Pump sala, Mije Aleksića 26, Surčin/Ledine, Beograd</span>
              </li>
            </ul>

            <div className="mt-10 aspect-[448/311] overflow-hidden">
              <iframe
                title="Mapa — Hoop and Pump sala"
                src="https://maps.google.com/maps?q=Hoop%20and%20Pump&z=15&output=embed"
                className="size-full border-0"
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
              />
            </div>
          </div>

          <aside className="rounded-3xl bg-[linear-gradient(rgba(46,46,46,0.45),rgba(30,30,30,0.7))] p-8 ring-1 ring-white/10 lg:mt-[118px] xl:p-10">
            <p className="text-sm font-semibold uppercase tracking-wide text-white/50">Hoćeš da igraš?</p>
            <h2 className="mt-3 text-[32px] font-bold leading-tight tracking-[-0.03em]">Prijava u ligu</h2>
            <p className="mt-3 text-lg text-white/65">
              Četiri kratka koraka, manje od dva minuta. Javljamo ti se na telefon ili mejl.
            </p>
            <div className="mt-8">
              <ArrowLink href={SIGNUP_HREF}>Prijavi se</ArrowLink>
            </div>
          </aside>
        </div>
      </section>
      <CtaSection />
    </>
  );
}

function PhoneIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-4 shrink-0" fill="currentColor" aria-hidden>
      <path d="M6.6 10.8a15.1 15.1 0 0 0 6.6 6.6l2.2-2.2a1 1 0 0 1 1-.25 11.4 11.4 0 0 0 3.6.57 1 1 0 0 1 1 1V20a1 1 0 0 1-1 1A17 17 0 0 1 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1c0 1.25.2 2.45.57 3.6a1 1 0 0 1-.25 1z" />
    </svg>
  );
}

function MailIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-4 shrink-0" fill="currentColor" aria-hidden>
      <path d="M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 4.2-8 5-8-5V6l8 5 8-5z" />
    </svg>
  );
}

function PinIcon() {
  return (
    <svg viewBox="0 0 24 24" className="mt-0.5 size-5 shrink-0" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
      <path d="M12 21s-7-6.1-7-11a7 7 0 0 1 14 0c0 4.9-7 11-7 11z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}
