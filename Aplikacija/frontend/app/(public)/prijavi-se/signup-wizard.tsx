"use client";

import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { ArrowLeft, ArrowRight } from "lucide-react";
import Link from "next/link";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { API_PROXY_PREFIX } from "@/lib/api/config";

/** Nivoi sa Framer forme; u bazi se čuvaju kao enum (SkillLevel na backendu). */
const SKILL_LEVELS = [
  { value: "BEGINNER", label: "Početnik", hint: "Igram iz zabave" },
  { value: "TRAINED_BEFORE", label: "Trenirao sam pre", hint: "Imam klupsko iskustvo" },
  { value: "PRO", label: "Profi", hint: "Igrao sam ozbiljno" },
] as const;

type Values = {
  firstName: string;
  lastName: string;
  nickname: string;
  heightCm: string;
  weightKg: string;
  birthYear: string;
  skillLevel: string;
  note: string;
  about: string;
  phone: string;
  email: string;
  website: string;
};

const EMPTY: Values = {
  firstName: "",
  lastName: "",
  nickname: "",
  heightCm: "",
  weightKg: "",
  birthYear: "",
  skillLevel: "",
  note: "",
  about: "",
  phone: "",
  email: "",
  website: "",
};

const STEPS = [
  { title: "Ko si ti?", short: "Ime", lead: "Kako se zoveš i kako da te zovemo na terenu." },
  { title: "Malo o tebi", short: "Telo", lead: "Visina, težina i godište — samo za ligu, ne objavljujemo ih." },
  { title: "Tvoja igra", short: "Igra", lead: "Par pitanja da bismo te bolje rasporedili." },
  { title: "Još samo kontakt", short: "Kontakt", lead: "Javićemo ti se na telefon ili mejl." },
] as const;

/** Nedovršena prijava preživi osvežavanje strane (sessionStorage, samo u ovom tabu). */
const DRAFT_KEY = "fg-prijava-draft";

type Status = { kind: "idle" } | { kind: "sending" } | { kind: "sent"; nickname: string } | { kind: "error"; message: string };

const FIELD =
  "h-12 w-full rounded-xl bg-white/[0.06] px-4 text-base text-white ring-1 ring-white/10 placeholder:text-white/35 outline-none transition-[box-shadow,background-color] duration-200 focus:bg-white/[0.09] focus:ring-2 focus:ring-white/60";

/**
 * Prijava u ligu u četiri kratka koraka (odluka D-20: ide u našu bazu, tabela `applications`).
 * Svaki korak traži samo par stvari, da prijava ne deluje kao formular od dvanaest polja.
 * Polje `website` je zamka za botove — ljudi ga ne vide.
 *
 * Animacije po Emilu Kowalskom: korak klizi 24px u smeru kretanja (ne preko celog ekrana),
 * ease-out, ispod 300 ms; dugmad se na pritisak skupe na 0.97; „smanji pokrete" gasi klizanje.
 */
export function SignupWizard() {
  const reduce = useReducedMotion();
  const [values, setValues] = useState<Values>(EMPTY);
  const [step, setStep] = useState(0);
  const [direction, setDirection] = useState(1);
  const [status, setStatus] = useState<Status>({ kind: "idle" });
  const formRef = useRef<HTMLFormElement>(null);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const firstRender = useRef(true);

  useEffect(() => {
    try {
      const saved = sessionStorage.getItem(DRAFT_KEY);
      if (saved) setValues({ ...EMPTY, ...(JSON.parse(saved) as Partial<Values>) });
    } catch {
      /* privatni režim bez storage-a — prijava radi i bez nacrta */
    }
  }, []);

  useEffect(() => {
    try {
      sessionStorage.setItem(DRAFT_KEY, JSON.stringify({ ...values, website: "" }));
    } catch {
      /* isto kao gore */
    }
  }, [values]);

  // Posle prelaska na nov korak fokus ide na njegov naslov — čitač ekrana pročita gde je čovek.
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    headingRef.current?.focus({ preventScroll: true });
  }, [step]);

  const set = (name: keyof Values) => (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setValues((v) => ({ ...v, [name]: event.target.value }));

  function goTo(next: number) {
    setDirection(next > step ? 1 : -1);
    setStep(next);
    if (status.kind === "error") setStatus({ kind: "idle" });
  }

  /** Pregledač proverava samo polja trenutnog koraka (required, min, max…) i sam pokazuje grešku. */
  function stepIsValid(): boolean {
    // Po data-step, a ne po ref-u: dok stari korak izlazi, u formi su privremeno oba.
    const fields = formRef.current?.querySelectorAll<HTMLInputElement | HTMLTextAreaElement>(
      `[data-step="${step}"] input, [data-step="${step}"] textarea`,
    );
    for (const field of fields ?? []) {
      if (!field.checkValidity()) {
        field.reportValidity();
        return false;
      }
    }
    return true;
  }

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!stepIsValid()) return;
    if (step < STEPS.length - 1) {
      goTo(step + 1);
      return;
    }
    await send();
  }

  async function send() {
    const text = (v: string) => (v.trim() === "" ? null : v.trim());
    const num = (v: string) => (v.trim() === "" ? null : Number(v));
    setStatus({ kind: "sending" });
    try {
      const response = await fetch(`${API_PROXY_PREFIX}/api/v1/public/applications`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          fullName: `${values.firstName.trim()} ${values.lastName.trim()}`.trim(),
          nickname: text(values.nickname),
          heightCm: num(values.heightCm),
          weightKg: num(values.weightKg),
          birthYear: num(values.birthYear),
          phone: text(values.phone),
          skillLevel: text(values.skillLevel),
          email: text(values.email),
          note: text(values.note),
          about: text(values.about),
          website: text(values.website),
        }),
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { message?: string } | null;
        throw new Error(body?.message ?? "Prijava nije poslata. Pokušaj ponovo ili nas pozovi.");
      }
      try {
        sessionStorage.removeItem(DRAFT_KEY);
      } catch {
        /* nema storage-a */
      }
      setStatus({ kind: "sent", nickname: values.nickname.trim() });
      setValues(EMPTY);
      setStep(0);
    } catch (e) {
      setStatus({ kind: "error", message: e instanceof Error ? e.message : "Prijava nije poslata." });
    }
  }

  if (status.kind === "sent") {
    return <SignupSuccess nickname={status.nickname} onAgain={() => setStatus({ kind: "idle" })} />;
  }

  const last = step === STEPS.length - 1;
  const slide = reduce ? 0 : 24;

  return (
    <form
      ref={formRef}
      onSubmit={onSubmit}
      className="relative overflow-hidden rounded-3xl bg-[linear-gradient(rgba(46,46,46,0.45),rgba(30,30,30,0.7))] p-5 ring-1 ring-white/10 sm:p-8"
    >
      <Progress step={step} onJump={(i) => i < step && goTo(i)} />

      <AutoHeight>
        <AnimatePresence mode="popLayout" initial={false} custom={direction}>
          <motion.div
            key={step}
            data-step={step}
            custom={direction}
            variants={{
              enter: (d: number) => ({ opacity: 0, x: d * slide }),
              center: { opacity: 1, x: 0 },
              exit: (d: number) => ({ opacity: 0, x: d * -slide }),
            }}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ duration: 0.24, ease: [0.23, 1, 0.32, 1] }}
            className="pt-8"
          >
            <p className="text-sm font-semibold text-white/45">
              Korak {step + 1} od {STEPS.length}
            </p>
            <h2 ref={headingRef} tabIndex={-1} className="mt-1 text-[28px] font-bold leading-tight tracking-[-0.03em] outline-none sm:text-[32px]">
              {STEPS[step].title}
            </h2>
            <p className="mt-2 text-white/60">{STEPS[step].lead}</p>

            <div className="mt-7 space-y-5">
              {step === 0 && (
                <>
                  <div className="grid gap-5 sm:grid-cols-2">
                    <Field label="Ime" autoComplete="given-name" value={values.firstName} onChange={set("firstName")} placeholder="Marko" required maxLength={60} />
                    <Field label="Prezime" autoComplete="family-name" value={values.lastName} onChange={set("lastName")} placeholder="Marković" required maxLength={60} />
                  </div>
                  <Field
                    label="Nadimak"
                    hint="Pod ovim imenom te predstavljamo na terenu i na sajtu."
                    value={values.nickname}
                    onChange={set("nickname")}
                    placeholder="Paki, Mare, Raketa…"
                    required
                    maxLength={60}
                  />
                </>
              )}

              {step === 1 && (
                <div className="grid gap-5 sm:grid-cols-3">
                  <Field label="Visina" suffix="cm" inputMode="numeric" type="number" value={values.heightCm} onChange={set("heightCm")} placeholder="195" required min={100} max={260} />
                  <Field label="Težina" suffix="kg" inputMode="numeric" type="number" value={values.weightKg} onChange={set("weightKg")} placeholder="88" required min={30} max={250} />
                  <Field label="Godište" inputMode="numeric" type="number" value={values.birthYear} onChange={set("birthYear")} placeholder="1998" required min={1900} max={2020} />
                </div>
              )}

              {step === 2 && (
                <>
                  <fieldset>
                    <legend className="mb-2.5 text-base font-medium">Košarkaško znanje</legend>
                    <div className="grid gap-2.5 sm:grid-cols-3">
                      {SKILL_LEVELS.map((level, i) => {
                        const checked = values.skillLevel === level.value;
                        return (
                          <label
                            key={level.value}
                            className={`relative cursor-pointer rounded-xl px-4 py-3 ring-1 transition-[background-color,box-shadow,transform] duration-150 ease-out active:scale-[0.97] ${
                              checked ? "bg-white text-black ring-white" : "bg-white/[0.06] ring-white/10 hover:bg-white/[0.1]"
                            }`}
                          >
                            <input
                              type="radio"
                              name="skillLevel"
                              value={level.value}
                              checked={checked}
                              onChange={() => setValues((v) => ({ ...v, skillLevel: level.value }))}
                              required={i === 0}
                              className="peer absolute inset-0 cursor-pointer opacity-0"
                            />
                            <span className="block font-bold">{level.label}</span>
                            <span className={`block text-sm ${checked ? "text-black/60" : "text-white/50"}`}>{level.hint}</span>
                            <span className="pointer-events-none absolute inset-0 rounded-xl peer-focus-visible:ring-2 peer-focus-visible:ring-white/70" />
                          </label>
                        );
                      })}
                    </div>
                  </fieldset>
                  <Area
                    label="Kada sigurno ne možeš da igraš?"
                    hint="Opciono — npr. „ne mogu subotom pre 14h“. Igra se subotom i nedeljom."
                    value={values.note}
                    onChange={set("note")}
                    placeholder="Napomene"
                  />
                  <Area label="Nešto više o sebi" hint="Opciono" value={values.about} onChange={set("about")} placeholder="Gde si igrao, šta voliš na terenu…" />
                </>
              )}

              {step === 3 && (
                <>
                  <Field label="Telefon" type="tel" autoComplete="tel" inputMode="tel" value={values.phone} onChange={set("phone")} placeholder="+381 69 1122334" required maxLength={30} pattern="[+0-9 ()/\-]{6,30}" title="Samo cifre, razmaci i znak +" />
                  <Field label="Email" type="email" autoComplete="email" value={values.email} onChange={set("email")} placeholder="ime@primer.com" required maxLength={120} />
                  <Summary values={values} onEdit={goTo} />
                </>
              )}
            </div>
          </motion.div>
        </AnimatePresence>
      </AutoHeight>

      {/* Zamka za botove: sakrivena od ljudi i od čitača ekrana. */}
      <input
        type="text"
        name="website"
        tabIndex={-1}
        autoComplete="off"
        aria-hidden
        value={values.website}
        onChange={(e) => setValues((v) => ({ ...v, website: e.target.value }))}
        className="absolute size-0 opacity-0"
      />

      {status.kind === "error" && (
        <p role="alert" className="mt-6 rounded-xl bg-red-500/15 px-4 py-3 text-sm text-red-200">
          {status.message}
        </p>
      )}

      <div className={`mt-8 flex items-center gap-3 ${last ? "flex-col-reverse sm:flex-row" : ""}`}>
        {step > 0 && (
          <button
            type="button"
            onClick={() => goTo(step - 1)}
            className="inline-flex h-12 items-center gap-2 rounded-full px-5 font-bold text-white/80 ring-1 ring-white/15 transition-[background-color,transform] duration-150 ease-out hover:bg-white/[0.06] active:scale-[0.97]"
          >
            <ArrowLeft className="size-4" aria-hidden />
            Nazad
          </button>
        )}
        {last ? (
          <SubmitButton sending={status.kind === "sending"} />
        ) : (
          <button
            type="submit"
            className="group ml-auto inline-flex h-12 items-center gap-2 rounded-full bg-white pl-6 pr-5 font-bold text-black transition-[background-color,transform] duration-150 ease-out hover:bg-[#dcdcdc] active:scale-[0.97]"
          >
            Dalje
            <ArrowRight className="size-4 transition-transform duration-200 ease-out group-hover:translate-x-0.5" aria-hidden />
          </button>
        )}
      </div>
    </form>
  );
}

/** Četiri crtice sa nazivima koraka; na već pređen korak može da se vrati klikom. */
function Progress({ step, onJump }: { step: number; onJump: (i: number) => void }) {
  return (
    <ol className="grid grid-cols-4 gap-2" aria-label="Koraci prijave">
      {STEPS.map((s, i) => {
        const done = i < step;
        const current = i === step;
        return (
          <li key={s.short}>
            <button
              type="button"
              onClick={() => onJump(i)}
              disabled={!done}
              aria-current={current ? "step" : undefined}
              className="block w-full text-left disabled:cursor-default"
            >
              <span className="block h-1 overflow-hidden rounded-full bg-white/15">
                <span
                  className="block h-full origin-left rounded-full bg-white transition-transform duration-300 ease-[var(--ease-out-strong)]"
                  style={{ transform: `scaleX(${done || current ? 1 : 0})` }}
                />
              </span>
              <span
                className={`mt-2 block text-xs font-semibold transition-colors duration-200 ${current ? "text-white" : done ? "text-white/65" : "text-white/35"}`}
              >
                {s.short}
              </span>
            </button>
          </li>
        );
      })}
    </ol>
  );
}

/** Visina kartice prati korak (koraci su različite visine), umesto da skoči. */
function AutoHeight({ children }: { children: React.ReactNode }) {
  const inner = useRef<HTMLDivElement>(null);
  const [height, setHeight] = useState<number | "auto">("auto");
  useLayoutEffect(() => {
    const el = inner.current;
    if (!el) return;
    const observer = new ResizeObserver(() => setHeight(el.offsetHeight));
    observer.observe(el);
    return () => observer.disconnect();
  }, []);
  return (
    <motion.div animate={{ height }} transition={{ duration: 0.28, ease: [0.23, 1, 0.32, 1] }} className="relative">
      <div ref={inner}>{children}</div>
    </motion.div>
  );
}

/** Pregled pre slanja: šta je upisano, uz „Izmeni" koje vraća na taj korak. */
function Summary({ values, onEdit }: { values: Values; onEdit: (step: number) => void }) {
  const level = SKILL_LEVELS.find((l) => l.value === values.skillLevel)?.label ?? "—";
  const rows: { step: number; label: string; value: string }[] = [
    { step: 0, label: "Igrač", value: `${values.firstName} ${values.lastName} „${values.nickname}“` },
    { step: 1, label: "Visina · težina · godište", value: `${values.heightCm} cm · ${values.weightKg} kg · ${values.birthYear}.` },
    { step: 2, label: "Nivo", value: level },
  ];
  return (
    <dl className="divide-y divide-white/10 rounded-xl bg-black/25 px-4 ring-1 ring-white/10">
      {rows.map((row) => (
        <div key={row.label} className="flex items-center gap-3 py-3">
          <div className="min-w-0 flex-1">
            <dt className="text-xs text-white/45">{row.label}</dt>
            <dd className="truncate font-semibold">{row.value}</dd>
          </div>
          <button
            type="button"
            onClick={() => onEdit(row.step)}
            className="shrink-0 rounded-full px-3 py-1 text-sm font-semibold text-white/70 transition-colors hover:bg-white/10 hover:text-white"
          >
            Izmeni
          </button>
        </div>
      ))}
    </dl>
  );
}

/** Veliko dugme „Prijavi se" — kraj puta, pa je najistaknutije na celoj strani. */
function SubmitButton({ sending }: { sending: boolean }) {
  return (
    <button
      type="submit"
      disabled={sending}
      className="group relative h-16 w-full overflow-hidden rounded-full bg-playoff text-xl font-extrabold uppercase tracking-wide text-white shadow-[0_10px_40px_-10px_rgba(255,90,31,0.8)] transition-[background-color,transform,opacity] duration-150 ease-out hover:bg-playoff-dark active:scale-[0.97] disabled:opacity-70 sm:flex-1"
    >
      <span className={`inline-flex items-center gap-3 transition-opacity duration-150 ${sending ? "opacity-0" : ""}`}>
        Prijavi se
        <ArrowRight className="size-6 transition-transform duration-200 ease-out group-hover:translate-x-1" aria-hidden />
      </span>
      {sending && (
        <span className="absolute inset-0 flex items-center justify-center gap-3 text-base normal-case tracking-normal">
          <span aria-hidden className="size-5 animate-spin rounded-full border-2 border-white/40 border-t-white" />
          Šaljem prijavu…
        </span>
      )}
    </button>
  );
}

const BURST = Array.from({ length: 10 }, (_, i) => (i / 10) * Math.PI * 2);

/**
 * Potvrda posle slanja: krug iskoči (spring, iz 0.6 — nikad iz nule), kvačica se iscrta,
 * a oko njega se razleti par narandžastih tačaka. Ceo prizor traje oko sekunde.
 */
function SignupSuccess({ nickname, onAgain }: { nickname: string; onAgain: () => void }) {
  const reduce = useReducedMotion();
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    ref.current?.focus({ preventScroll: true });
    ref.current?.scrollIntoView({ behavior: reduce ? "auto" : "smooth", block: "center" });
  }, [reduce]);

  return (
    <div
      ref={ref}
      tabIndex={-1}
      role="status"
      className="relative overflow-hidden rounded-3xl bg-[linear-gradient(rgba(46,46,46,0.45),rgba(30,30,30,0.7))] px-6 py-14 text-center outline-none ring-1 ring-white/10 sm:px-10"
    >
      <div aria-hidden className="pointer-events-none absolute left-1/2 top-[86px] size-[340px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-playoff/20 blur-3xl" />
      <div className="relative mx-auto size-[104px]">
        {!reduce &&
          BURST.map((angle, i) => (
            <motion.span
              key={i}
              aria-hidden
              className={`absolute left-1/2 top-1/2 -ml-1 -mt-1 size-2 rounded-full ${i % 2 ? "bg-white" : "bg-playoff"}`}
              initial={{ x: 0, y: 0, opacity: 0, scale: 0.6 }}
              animate={{ x: Math.cos(angle) * 84, y: Math.sin(angle) * 84, opacity: [0, 1, 0], scale: [0.6, 1, 0.4] }}
              transition={{ delay: 0.32, duration: 0.8, ease: [0.23, 1, 0.32, 1] }}
            />
          ))}
        {!reduce && (
          <motion.span
            aria-hidden
            className="absolute inset-0 rounded-full border-2 border-playoff"
            initial={{ scale: 1, opacity: 0.8 }}
            animate={{ scale: 1.7, opacity: 0 }}
            transition={{ delay: 0.28, duration: 0.7, ease: [0.23, 1, 0.32, 1] }}
          />
        )}
        <motion.span
          className="absolute inset-0 flex items-center justify-center rounded-full bg-playoff shadow-[0_12px_40px_-8px_rgba(255,90,31,0.9)]"
          initial={reduce ? false : { scale: 0.6, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", stiffness: 420, damping: 22 }}
        >
          <svg viewBox="0 0 24 24" className="size-12" aria-hidden>
            <motion.path
              d="M5 12.5l4.5 4.5L19 7.5"
              fill="none"
              stroke="white"
              strokeWidth={2.6}
              strokeLinecap="round"
              strokeLinejoin="round"
              initial={reduce ? false : { pathLength: 0 }}
              animate={{ pathLength: 1 }}
              transition={{ delay: 0.18, duration: 0.36, ease: [0.23, 1, 0.32, 1] }}
            />
          </svg>
        </motion.span>
      </div>

      <motion.div
        initial={reduce ? false : { opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.45, duration: 0.3, ease: [0.23, 1, 0.32, 1] }}
        className="relative"
      >
        <h2 className="mt-8 text-[30px] font-bold leading-tight tracking-[-0.03em] sm:text-[36px]">
          Prijava je poslata{nickname ? `, ${nickname}` : ""}!
        </h2>
        <p className="mx-auto mt-3 max-w-md text-lg text-white/70">
          Primili smo je i javićemo ti se na telefon ili mejl koji si ostavio. Vidimo se na terenu.
        </p>
        <div className="mt-9 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <Link
            href="/"
            className="inline-flex h-12 items-center rounded-full bg-white px-6 font-bold text-black transition-[background-color,transform] duration-150 ease-out hover:bg-[#dcdcdc] active:scale-[0.97]"
          >
            Nazad na početnu
          </Link>
          <button
            type="button"
            onClick={onAgain}
            className="inline-flex h-12 items-center rounded-full px-6 font-bold text-white/75 ring-1 ring-white/15 transition-[background-color,transform] duration-150 ease-out hover:bg-white/[0.06] active:scale-[0.97]"
          >
            Prijavi još nekog
          </button>
        </div>
      </motion.div>
    </div>
  );
}

function Field({
  label,
  hint,
  suffix,
  ...input
}: { label: string; hint?: string; suffix?: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block">
      <span className="mb-2 block text-base font-medium">{label}</span>
      <span className="relative block">
        <input {...input} className={`${FIELD} ${suffix ? "pr-12" : ""} [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none`} />
        {suffix && <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-white/45">{suffix}</span>}
      </span>
      {hint && <span className="mt-1.5 block text-sm text-white/45">{hint}</span>}
    </label>
  );
}

function Area({
  label,
  hint,
  ...area
}: { label: string; hint?: string } & React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <label className="block">
      <span className="mb-2 block text-base font-medium">
        {label}
        {hint && <span className="ml-2 text-sm font-normal text-white/45">{hint}</span>}
      </span>
      <textarea {...area} maxLength={500} className={`${FIELD} h-[92px] resize-none py-3`} />
    </label>
  );
}
