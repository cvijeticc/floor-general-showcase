import { playerThumb } from "@/lib/player-photos";
import { TIE_BREAK_LABEL, type PublicFormItem, type PublicGroup, type PublicStandingRow } from "@/lib/site-api";
import { signed } from "@/lib/stat-format";
import { FormChips } from "./form-chips";
import { PlayerAvatar } from "./player-avatar";
import { PlayerLink } from "./player-link";

type Zone = "direct" | "extra";

/**
 * Zona prolaza (BR-05) po mestu u grupi. Brojevi dolaze iz faze (directAdvance, additionalAdvance —
 * odluka D-05), nikad iz koda: prvih N direktno, (N+1). mesto se bori za dodatna mesta.
 */
export function advanceZone(
  position: number | null | undefined,
  group: Pick<PublicGroup, "directAdvance" | "additionalAdvance">,
): Zone | null {
  if (!position || !group.directAdvance) return null;
  if (position <= group.directAdvance) return "direct";
  if (group.additionalAdvance && position === group.directAdvance + 1) return "extra";
  return null;
}

/** Boja leve ivice reda. Na ćeliji, ne na <tr> — pseudo-element u redu tabele postaje nova kolona. */
const ZONE_BORDER: Record<Zone, string> = {
  direct: "border-l-emerald-500",
  extra: "border-l-prize-gold",
};

/**
 * Legenda tabele. Kad je drvo playoff-a objavljeno, boja više ne znači „mesto koje prolazi" nego
 * „igrač je u playoff-u" — kostur lige je posle odustajanja uzimao i 7., 8. i 9. mesto (poruka 08.11.).
 */
export function Legend({ group, playoff = false }: { group: PublicGroup; playoff?: boolean }) {
  if (playoff) {
    return (
      <ul className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-black/70">
        <li className="flex items-center gap-2">
          <span className="size-3 rounded-sm bg-emerald-500" /> Igra u playoff-u (Top 64)
        </li>
        <li className="flex items-center gap-2">
          <span className="inline-flex size-4 items-center justify-center rounded-full bg-black/[0.08] text-[10px] font-bold text-black/60">
            i
          </span>{" "}
          Razdvojeni međusobnim mečevima ili koš-razlikom (pređi mišem ili dodirni)
        </li>
      </ul>
    );
  }
  if (!group.directAdvance) return null;
  return (
    <ul className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-black/70">
      <li className="flex items-center gap-2">
        <span className="size-3 rounded-sm bg-emerald-500" /> Prvih {group.directAdvance} — direktan prolaz
      </li>
      {!!group.additionalAdvance && (
        <li className="flex items-center gap-2">
          <span className="size-3 rounded-sm bg-prize-gold" /> {group.directAdvance + 1}. mesto — borba za{" "}
          {group.additionalAdvance} dodatna mesta
        </li>
      )}
    </ul>
  );
}

/**
 * Tabela grupe (UC-PUB-02). Kolone se pale po širini same tabele (container queries), ne ekrana:
 * ista tabela je široka na tabu „Tabele" za jednu grupu, a uska u dve kolone ili na profilu igrača.
 * Forma (poslednjih 5, kao na SofaScore-u) staje tek kad ima mesta — u dve kolone na laptopu se ne vidi.
 */
export function StandingsTable({
  group,
  rows,
  forms,
  highlightPlayerId,
  heading,
  playoffIds,
}: {
  group: PublicGroup;
  rows: PublicStandingRow[] | null;
  /** Forma po ID-ju igrača; bez nje nema ni kolone. */
  forms?: Map<number, PublicFormItem[]>;
  /** Red igrača čiji je profil otvoren. */
  highlightPlayerId?: number;
  heading?: React.ReactNode;
  /** Igrači u drvetu playoff-a — kad postoji, zona prolaza je „u playoff-u", ne mesto u grupi. */
  playoffIds?: Set<number>;
}) {
  return (
    <div className="@container min-w-0">
      {heading ?? <h2 className="text-2xl font-bold">{group.name}</h2>}
      {!rows || rows.length === 0 ? (
        <p className="mt-3 text-black/60">Tabela će biti objavljena posle prvog kola.</p>
      ) : (
        <div className="mt-3 overflow-x-auto rounded-2xl border border-black/10">
          <table className="w-full text-left text-[15px] tabular-nums">
            <thead className="bg-black text-xs uppercase tracking-wide text-white">
              <tr>
                <th className="border-l-4 border-l-black py-3 pl-3 pr-2 font-semibold">#</th>
                <th className="px-2 py-3 font-semibold">Igrač</th>
                <Th title="Odigrano">O</Th>
                <Th title="Pobede">P</Th>
                <Th title="Porazi">I</Th>
                <Th title="Dati poeni" className="hidden @lg:table-cell">
                  DP
                </Th>
                <Th title="Primljeni poeni" className="hidden @lg:table-cell">
                  PP
                </Th>
                <Th title="Koš-razlika">KR</Th>
                <Th title="Bodovi" className={forms ? "pr-4 @2xl:pr-2" : "pr-4"}>
                  B
                </Th>
                {forms && (
                  <th className="hidden py-3 pl-2 pr-4 font-semibold @2xl:table-cell">
                    <abbr title="Poslednjih 5 mečeva, od starijeg ka novijem" className="no-underline">
                      Forma
                    </abbr>
                  </th>
                )}
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => {
                const zone: Zone | null = playoffIds
                  ? playoffIds.has(r.player.id)
                    ? "direct"
                    : null
                  : advanceZone(r.position, group);
                const highlighted = r.player.id === highlightPlayerId;
                return (
                  <tr
                    key={r.player.id}
                    aria-current={highlighted || undefined}
                    className={`border-t border-black/5 ${highlighted ? "bg-prize-gold/20 font-semibold" : "even:bg-black/[0.02]"}`}
                  >
                    <td
                      className={`border-l-4 py-2.5 pl-3 pr-2 font-bold ${zone ? ZONE_BORDER[zone] : "border-l-transparent"}`}
                    >
                      <span className="inline-flex items-center gap-1">
                        {r.position}
                        {r.tieBreak && (
                          // FR-5.1: kojim kriterijumom BR-03 je igrač odvojen od izjednačenih.
                          <abbr
                            title={TIE_BREAK_LABEL[r.tieBreak]}
                            aria-label={TIE_BREAK_LABEL[r.tieBreak]}
                            tabIndex={0}
                            className="inline-flex size-4 cursor-help items-center justify-center rounded-full bg-black/[0.08] text-[10px] font-bold text-black/60 no-underline"
                          >
                            i
                          </abbr>
                        )}
                      </span>
                    </td>
                    <td className="px-2 py-2">
                      <PlayerLink player={r.player} className="group flex items-center gap-2.5">
                        <PlayerAvatar
                          name={r.player.name}
                          src={playerThumb(r.player)}
                          size={28}
                          className="hidden @md:inline-flex"
                        />
                        {/* U uskoj tabeli (profil, telefon) ime ide ispod nadimka, umesto da se lomi usred. */}
                        <span className="min-w-0">
                          <span className="font-semibold underline-offset-2 group-hover:underline">
                            {r.player.nickname}
                          </span>{" "}
                          <span className="block text-[13px] font-normal text-black/50 @md:inline @md:text-[15px]">
                            {r.player.name}
                          </span>
                        </span>
                      </PlayerLink>
                    </td>
                    <Td>{r.played}</Td>
                    <Td>{r.wins}</Td>
                    <Td>{r.losses}</Td>
                    <Td className="hidden @lg:table-cell">{r.pointsFor}</Td>
                    <Td className="hidden @lg:table-cell">{r.pointsAgainst}</Td>
                    <Td>{signed(r.pointDifference)}</Td>
                    <Td className={`font-extrabold ${forms ? "pr-4 @2xl:pr-2" : "pr-4"}`}>{r.points}</Td>
                    {forms && (
                      <td className="hidden py-2 pl-2 pr-4 @2xl:table-cell">
                        <FormChips items={forms.get(r.player.id) ?? []} />
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Th({ children, title, className = "" }: { children: React.ReactNode; title: string; className?: string }) {
  return (
    <th className={`px-2 py-3 text-center font-semibold ${className}`}>
      <abbr title={title} className="no-underline">
        {children}
      </abbr>
    </th>
  );
}

function Td({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return <td className={`px-2 py-2.5 text-center ${className}`}>{children}</td>;
}
