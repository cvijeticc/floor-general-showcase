import Image from "next/image";
import Link from "next/link";

const SOCIAL = [
  { label: "Instagram", href: "https://www.instagram.com/floorgeneral.rs" },
  { label: "TikTok", href: "https://www.tiktok.com/@floorgeneral.rs" },
  { label: "YouTube", href: "https://youtube.com/@floorgeneral_rs" },
];

const LEGAL = [
  { label: "Politika privatnosti", href: "/dokumenti/politika-privatnosti.pdf" },
  { label: "Uslovi korišćenja i prodaje kotizacija", href: "/dokumenti/uslovi-koriscenja.pdf" },
  { label: "Politika reklamacija i povraćaja sredstava", href: "/dokumenti/politika-reklamacija.pdf" },
];

export function SiteFooter() {
  return (
    <footer className="bg-black pb-16 pt-[74px] font-site text-white xl:pb-[120px] xl:pt-[90px]">
      <div className="mx-auto grid max-w-[1020px] gap-10 px-5 text-center md:grid-cols-[minmax(0,390px)_215px_auto] md:gap-[70px] md:text-left">
        <Link href="/#hero" className="flex items-center justify-center gap-1.5 self-start md:justify-start">
          <Image src="/site/logo.png" alt="" width={47} height={47} className="h-[47px] w-auto" />
          <span className="text-base font-bold">Floor General</span>
        </Link>

        <div>
          <p className="text-base">DRUŠTVENE MREŽE</p>
          <ul className="mt-4 space-y-4 text-sm">
            {SOCIAL.map((s) => (
              <li key={s.href}>
                <a
                  href={s.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-site-muted transition-colors duration-500 hover:text-white"
                >
                  {s.label}
                </a>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <p className="text-base">PODACI FIRME</p>
          <ul className="mt-4 space-y-4 text-sm">
            <li>Naziv: Floor General DOO</li>
            <li>MB: 22193651</li>
            <li>PIB: 115689713</li>
            <li>Adresa: Cvijićeva 63, Beograd, Srbija</li>
            <li>
              Email:{" "}
              <a href="mailto:floorgeneral.rs@gmail.com" className="hover:underline">
                floorgeneral.rs@gmail.com
              </a>
            </li>
          </ul>
        </div>
      </div>

      <ul className="mt-12 flex flex-col items-center gap-0 px-5 font-site-alt text-sm md:items-start md:px-10 xl:mt-[60px]">
        {LEGAL.map((l) => (
          <li key={l.href}>
            <a href={l.href} target="_blank" rel="noopener" className="block px-2.5 py-2.5 leading-[14px] hover:underline">
              {l.label}
            </a>
          </li>
        ))}
      </ul>
    </footer>
  );
}
