type NavItem = {
  label: string;
  href: string;
  /**
   * Dugme umesto običnog linka: „white" je belo dugme („Rezultati"), „playoff" je narandžasto —
   * playoff je počeo i treba da se vidi prvi (zahtev korisnika 10.11.2026.).
   */
  highlight?: "white" | "playoff";
  /**
   * Samo na širokom ekranu (2xl+): na laptopu od 1280px osam stavki i dva dugmeta ne staju u red,
   * pa „Početna" ustupa mesto — logo ionako vodi na početnu. Na telefonu je u meniju uvek.
   */
  wideOnly?: boolean;
};

/**
 * Stavke glavnog menija javnog sajta. Redosled prati Framer original, uz tabove „Rezultati" i
 * „Playoff" — prvo Rezultati, odmah zatim Playoff.
 */
export const NAV_ITEMS: readonly NavItem[] = [
  { label: "Početna", href: "/", wideOnly: true },
  { label: "Šta nudimo", href: "/#sta-nudimo" },
  { label: "Kako se igra liga", href: "/#kako-se-igra-liga" },
  { label: "Rezultati", href: "/rezultati", highlight: "white" },
  { label: "Playoff", href: "/playoff", highlight: "playoff" },
  { label: "Pravilnik i cene", href: "/#pravilnik-i-cene" },
  { label: "Kontakt", href: "/kontakt" },
  { label: "O nama", href: "/#o-nama" },
];

export const SIGNUP_HREF = "/prijavi-se";

/**
 * Za naslov sekcije na koji vodi sidro iz menija (/#sta-nudimo...): posle klika naslov staje
 * 32px (telefon) odnosno 48px (desktop) ispod menija od 53/93px. Sidro je na naslovu, ne na
 * sekciji — sekcije imaju po 100–160px gornjeg razmaka, pa bi ispod menija ostala praznina.
 */
export const ANCHOR_TARGET = "scroll-mt-[85px] xl:scroll-mt-[141px]";

/** Zajednička širina sadržaja: 1120px kao na originalu (160px margine na 1440px ekranu). */
export const CONTAINER = "mx-auto w-full max-w-[1120px] px-5 md:px-10 xl:px-0";
