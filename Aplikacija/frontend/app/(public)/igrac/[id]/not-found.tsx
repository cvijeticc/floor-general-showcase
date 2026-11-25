import Link from "next/link";
import { CONTAINER } from "@/components/site/nav";

/** 404 za profil — unutar javnog layout-a (meni i podnožje), ne Next-ova podrazumevana strana. */
export default function PlayerNotFound() {
  return (
    <section className="bg-black pb-24 pt-16 font-site text-white xl:pt-24">
      <div className={CONTAINER}>
        <h1 className="text-[42px] font-bold leading-[1.2] xl:text-[56px]">Igrač nije pronađen</h1>
        <p className="mt-3 max-w-xl text-lg text-white/70">
          Ovaj igrač ne igra u aktuelnoj sezoni ili link nije ispravan. Pronađi ga na spisku svih igrača.
        </p>
        <Link
          href="/rezultati?tab=igraci"
          className="mt-8 inline-flex rounded-full bg-white px-6 py-3 font-bold text-black transition-colors duration-300 hover:bg-[#ccc]"
        >
          Svi igrači
        </Link>
      </div>
    </section>
  );
}
