import Image from "next/image";
import { ANCHOR_TARGET } from "../nav";

/**
 * `focus` = koji deo slike staje u kvadrat (CSS object-position). Četiri slike su snimci ekrana
 * telefona (943 × 2048), pa bi centar kvadrata odsekao glavu — fokus je postavljen malo iznad glave.
 */
const TEAM = [
  {
    name: "Nikola Pavlović",
    photo: "/site/tim-nikola.jpeg",
    focus: "50% 16%",
    bio: ["Osnivač Baci5 Podcasta.", "Bavio se košarkom 15 godina."],
  },
  {
    name: "Marko Cvijetić",
    photo: "/site/tim-marko.jpeg",
    focus: "50% 50%",
    bio: ["Diplomirani inženjer Organizacionih Nauka sa FON-a.", "Bavio se košarkom 8 godina."],
  },
  {
    name: "Andrej Trnavac",
    photo: "/site/tim-andrej.jpeg",
    focus: "50% 30%",
    bio: ["Profesionalni rukometaš. Prvi golman Partizana i reprezentativac Srbije.", "Veliki ljubitelj košarke."],
  },
  {
    name: "Matija Maksimović",
    photo: "/site/tim-matija.jpeg",
    focus: "50% 32%",
    bio: ["Diplomirani ekonomista.", "Bavio se košarkom 13 godina."],
  },
  {
    name: "Aleksandar Pantelić",
    photo: "/site/tim-aleksandar.jpeg",
    focus: "50% 18%",
    bio: ["Profesionalni košarkaš. Bavi se košarkom već 20 godina."],
  },
];

/** „O nama" — cik-cak: tekst levo/slika desno, pa obrnuto. Na telefonu slika pa tekst. */
export function Team() {
  return (
    <section className="bg-[linear-gradient(#000_0%,rgb(33,33,33)_31.5%,rgb(115,115,115)_100%)] pb-10 pt-10 font-site text-white xl:pb-20 xl:pt-20">
      <h2 id="o-nama" className={`${ANCHOR_TARGET} text-center text-[42px] font-bold leading-[1.2]`}>
        O NAMA
      </h2>

      <div className="mx-auto mt-[51px] flex max-w-[960px] flex-col gap-10 px-5 md:gap-0 md:px-10 xl:px-0">
        {TEAM.map((m, i) => {
          const imageRight = i % 2 === 0;
          return (
            <article key={m.name} className="grid items-center md:grid-cols-2">
              {/* Na niskom ekranu (laptop 14") kvadrat od 480px ne staje ispod menija — tada je niži od širine. */}
              <div
                className={`relative aspect-square max-h-[calc(100svh-120px)] w-full overflow-hidden rounded-[20px] ${imageRight ? "md:order-2" : "md:w-[calc(100%-5px)]"}`}
              >
                <Image
                  src={m.photo}
                  alt={m.name}
                  fill
                  sizes="(max-width: 768px) 90vw, 480px"
                  className="object-cover"
                  style={{ objectPosition: m.focus }}
                />
              </div>
              <div className={`mt-5 md:mt-0 ${imageRight ? "md:pl-20 md:pr-10" : "md:pl-[88px] md:pr-10"}`}>
                <h3 className="font-site-alt text-2xl leading-none font-semibold tracking-[-0.03em] md:text-[32px]">
                  {m.name}
                </h3>
                <div className="mt-2.5 font-site-alt text-lg leading-[1.2] tracking-[-0.01em] text-site-muted md:mt-[15px] md:text-[22px]">
                  {m.bio.map((line) => (
                    <p key={line}>{line}</p>
                  ))}
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
