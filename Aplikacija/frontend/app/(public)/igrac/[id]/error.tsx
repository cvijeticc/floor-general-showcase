"use client";

import { CONTAINER } from "@/components/site/nav";

/** Backend ne odgovara (npr. tokom deploy-a): poruka i dugme, a strana ostaje 500 da je nginx ne kešira. */
export default function PlayerError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <section className="bg-black pb-24 pt-16 font-site text-white xl:pt-24">
      <div className={CONTAINER}>
        <h1 className="text-[42px] font-bold leading-[1.2] xl:text-[56px]">Profil trenutno nije dostupan</h1>
        <p className="mt-3 max-w-xl text-lg text-white/70">Pokušaj ponovo za par minuta.</p>
        <button
          type="button"
          onClick={() => reset()}
          className="mt-8 inline-flex rounded-full bg-white px-6 py-3 font-bold text-black transition-colors duration-300 hover:bg-[#ccc]"
        >
          Pokušaj ponovo
        </button>
      </div>
    </section>
  );
}
