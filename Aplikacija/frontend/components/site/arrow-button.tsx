import Link from "next/link";

/**
 * Belo „pill" dugme sa crnim krugom i strelicom (PRIJAVI SE U LIGU). Na hover se pozadina
 * zatamni do #ccc, a strelica se iz ↗ okrene u → — isto kao na Framer originalu.
 */
const BASE =
  "group inline-flex items-center gap-2.5 rounded-full bg-white py-2.5 pl-5 pr-2.5 font-site text-black transition-colors duration-500 ease-[var(--ease-framer)] hover:bg-[#cccccc]";

export function ArrowLink({
  href,
  children,
  size = "lg",
}: {
  href: string;
  children: React.ReactNode;
  size?: "lg" | "md";
}) {
  return (
    <Link href={href} className={`${BASE} ${size === "lg" ? "text-[22px] font-extrabold uppercase" : "text-base font-bold"}`}>
      {children}
      <ArrowCircle />
    </Link>
  );
}

export function ArrowSubmit({ children, disabled }: { children: React.ReactNode; disabled?: boolean }) {
  return (
    <button
      type="submit"
      disabled={disabled}
      className={`${BASE} relative h-12 w-full justify-center py-1 pr-1 text-base font-bold disabled:opacity-60`}
    >
      <span className="flex-1 text-center">{children}</span>
      <ArrowCircle />
    </button>
  );
}

function ArrowCircle() {
  return (
    <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-black text-white">
      <svg
        viewBox="0 0 24 24"
        aria-hidden
        className="size-4 -rotate-45 transition-transform duration-500 ease-[var(--ease-framer)] group-hover:rotate-0"
      >
        <path d="M4 12h15m-6-6 6 6-6 6" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </span>
  );
}
