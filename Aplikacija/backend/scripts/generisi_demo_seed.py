#!/usr/bin/env python3
"""
Izmišljena liga za lokalni razvoj (profil "dev"). Pravi dva Flyway seed-a:

  src/main/resources/db/dev/V7__seed_dev_demo_igraci_i_grupe.sql
      100 izmišljenih igrača raspoređenih u 10 grupa
  src/main/resources/db/dev/V18__seed_dev_demo_rezultati_i_playoff.sql
      900 mečeva grupne faze (dvokružno, 18 kola) i playoff drvo od 64 (Top 64 i Top 32
      odigrani, Top 16 zakazan)

Sva imena, nadimci i brojke su izmišljeni. Seed slučajnih brojeva je fiksan, pa skripta uvek
daje isti izlaz. Plasman u grupama se računa istim pravilima kao engine/TieBreak (BR-03), da
oznake nosilaca na kosturu („1. Gr. 3") odgovaraju tabelama koje aplikacija izračuna pri
pokretanju (StandingsBootstrap).

Pokretanje (iz Aplikacija/backend):  python3 scripts/generisi_demo_seed.py
"""
from __future__ import annotations

import datetime as dt
import math
import random
from dataclasses import dataclass, field
from pathlib import Path

RNG = random.Random(20261124)
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/db/dev"
SEASON = "2026/2027"
GROUPS = 10
GROUP_SIZE = 10

# Bodovanje sezone 2026/2027: pobeda 2, poraz 1, i službeni poraz 1 (odluka D-22).
POINTS_WIN, POINTS_LOSS, POINTS_FORFEIT = 2, 1, 1
FORFEIT_WINNER_SCORE, FORFEIT_LOSER_SCORE = 11, 0

FIRST_M = [
    "Aleksa", "Aleksandar", "Andrej", "Andrija", "Bogdan", "Bojan", "Branko", "Damjan", "Danilo",
    "Darko", "David", "Dejan", "Dimitrije", "Dragan", "Dušan", "Đorđe", "Filip", "Goran", "Igor",
    "Ivan", "Jovan", "Kosta", "Lazar", "Luka", "Marko", "Matej", "Mihajlo", "Milan", "Miloš",
    "Mladen", "Momčilo", "Nemanja", "Nenad", "Nikola", "Ognjen", "Pavle", "Petar", "Predrag",
    "Relja", "Sava", "Siniša", "Slobodan", "Srđan", "Stefan", "Strahinja", "Teodor", "Uroš",
    "Vasilije", "Veljko", "Viktor", "Vladimir", "Vuk", "Vukašin", "Zoran", "Žarko", "Željko",
]
FIRST_F = ["Ana", "Jelena", "Jovana", "Katarina", "Marija", "Milica", "Sara", "Teodora"]
LAST = [
    "Adamović", "Antić", "Arsić", "Babić", "Bajić", "Bjelić", "Bošković", "Božić", "Branković",
    "Bulatović", "Čavić", "Ćirić", "Ćosić", "Ćurčić", "Cvijović", "Dašić", "Davidović", "Dedić",
    "Dimić", "Dimitrijević", "Đekić", "Đokić", "Đukić", "Đurić", "Erić", "Filipović", "Gajić",
    "Gavrilović", "Gligorić", "Glišić", "Hadžić", "Isaković", "Ivić", "Jakovljević", "Jevtić",
    "Jurić", "Knežević", "Kostadinović", "Kovačević", "Lalić", "Lazić", "Ljubić", "Lukić",
    "Majstorović", "Marić", "Miletić", "Milić", "Milutinović", "Mirković", "Mišić", "Mitić",
    "Nedeljković", "Nenadić", "Nešić", "Novaković", "Ostojić", "Panić", "Pantić", "Pavićević",
    "Pejić", "Perić", "Petković", "Popović", "Prodanović", "Radić", "Radojević", "Sekulić", "Simić",
    "Spasić", "Stevanović", "Stojković", "Šćepanović", "Trifunović", "Urošević", "Vasić",
    "Vasiljević", "Veselinović", "Vlajić", "Vranješ", "Vukić", "Vuković", "Zdravković", "Zec",
    "Zorić", "Živković", "Belić", "Bogićević", "Dinić", "Đuričić", "Jeftić", "Kojić", "Lekić",
    "Mijailović", "Nikšić", "Pajić", "Rašić", "Ružić", "Savković", "Stamenković", "Tešić",
    "Topalović", "Vojinović", "Zarić", "Živanović", "Krsmanović", "Lazović", "Maričić", "Rajić",
    "Tomašević", "Velimirović", "Zlatković", "Ćeranić",
]
NICKS = [
    "Raketa", "Zeka", "Mrki", "Dule", "Žile", "Kiza", "Bata", "Čupa", "Garo", "Gile", "Gaša",
    "Lepi", "Medo", "Rile", "Roki", "Sale", "Tile", "Toza", "Vule", "Zoki", "Žarac", "Boca", "Brka",
    "Fićo", "Grof", "Jarac", "Kum", "Mačak", "Mišo", "Neša", "Ogi", "Pirke", "Rođa", "Sova",
    "Šilja", "Tanki", "Vrabac", "Zmaj", "Žuća", "Ćuka", "Bubi", "Doca", "Fantom", "Gazda", "Hulk",
    "Jež", "Kapiten", "Kobra", "Lav", "Ljuba", "Majstor", "Munja", "Nindža", "Orao", "Profa", "Ris",
    "Sokol", "Šampion", "Tigar", "Torpedo", "Vuja", "Bomba", "Dugi", "Kec", "Strela", "Šaraf",
    "Točak", "Vetar", "Žuti", "Crni", "Flash", "Splash", "Hook", "Iceman", "Dribler", "Trojka",
    "Blok", "Smeč", "Kengur", "Pantera", "Soko", "Grom", "Bager", "Šuter", "Jastreb", "Leptir",
    "Komarac", "Mrav", "Brzi", "Kamen", "Oblak", "Šerif", "Vitez", "Žaba", "Duh", "Kralj", "Bik",
]
NICKS_F = ["Dada", "Seka", "Lasta", "Lija", "Buba", "Senka"]
NOTES = [
    "Ne može subotom pre 14h",
    "Može samo nedeljom",
    "Radi vikendom do 13h",
    "Ne može 31.10. i 01.11.",
    "Kasni ako je termin pre 13h",
]


@dataclass
class Player:
    pid: int  # player_id u svežoj dev bazi: redosled upisa (bigserial od 1)
    first: str
    last: str
    nickname: str
    female: bool
    height: int
    weight: int
    birth_year: int
    payment: str | None
    note: str | None
    strength: float
    group: int = 0

    @property
    def full(self) -> str:
        return f"{self.first} {self.last}"


@dataclass
class Game:
    p1: int
    p2: int
    s1: int
    s2: int
    status: str = "FINISHED"
    note: str | None = None
    round_no: int = 0
    court: str | None = None
    start: dt.datetime | None = None


@dataclass
class Totals:
    pid: int
    played: int = 0
    wins: int = 0
    losses: int = 0
    pf: int = 0
    pa: int = 0
    points: int = 0

    @property
    def diff(self) -> int:
        return self.pf - self.pa


@dataclass
class Series:
    round_no: int
    position: int
    third_place: bool = False
    p1: int | None = None
    p2: int | None = None
    seed1: str | None = None
    seed2: str | None = None
    winner: int | None = None
    walkover: bool = False
    court: str | None = None
    start: dt.datetime | None = None
    note: str | None = None
    games: list[Game] = field(default_factory=list)


def clamp(v: float, lo: int, hi: int) -> int:
    return max(lo, min(hi, int(round(v))))


def make_players() -> list[Player]:
    lasts = RNG.sample(LAST, 92)
    lasts += RNG.sample(lasts, 8)  # neka prezimena se ponavljaju, kao u pravoj ligi
    RNG.shuffle(lasts)
    nicks = RNG.sample(NICKS, len(NICKS))
    nicks_f = RNG.sample(NICKS_F, len(NICKS_F))
    women = set(RNG.sample(range(100), 4))
    used, players = set(), []
    for i, last in enumerate(lasts):
        female = i in women
        while True:
            first = RNG.choice(FIRST_F if female else FIRST_M)
            if f"{first} {last}" not in used:
                break
        used.add(f"{first} {last}")
        if female:
            nickname = nicks_f.pop()
        elif RNG.random() < 0.15 and lasts.count(last) == 1:
            nickname = last  # liga često zove igrača po prezimenu
        else:
            nickname = nicks.pop()
        if female:
            height, weight = clamp(RNG.gauss(173, 6), 160, 188), clamp(RNG.gauss(64, 6), 52, 82)
        else:
            height = clamp(RNG.gauss(187, 7.5), 170, 208)
            weight = clamp(height - 100 + RNG.gauss(-2, 8), 63, 118)
        years = list(range(1986, 2009))
        birth = RNG.choices(years, weights=[1 if y < 1996 else 3 if y < 2001 else 5 for y in years])[0]
        payment = RNG.choices(["6500din", "Free", "3250din", None], weights=[85, 5, 5, 5])[0]
        note = RNG.choice(NOTES) if RNG.random() < 0.08 else None
        players.append(Player(i + 1, first, last, nickname, female, height, weight, birth,
                              payment, note, RNG.gauss(0, 1)))
    return players


def play(a: Player, b: Player, target: int = 11, max_score: int = 12) -> tuple[int, int]:
    """Rezultat jednog meča iz ugla igrača a. Meč traje 8 minuta; ko dostigne target završava
    ranije, a ko sa target-1 pogodi šut spolja (2 poena) završava sa max_score (D-18)."""
    a_wins = RNG.random() < 1 / (1 + math.exp(-1.3 * (a.strength - b.strength)))
    gap = abs(a.strength - b.strength)
    r = RNG.random()
    if r < 0.64:
        win = target
    elif r < 0.82:
        win = max_score
    else:
        win = RNG.randint(max(4, target - 6), target - 1)  # isteklo vreme
    if win >= target:
        top = target - 1
        mode = max(1.0, top * 0.75 - 2.5 * gap * top / 10)
        lose = min(top, win - 1, int(round(RNG.triangular(0, top, mode))))
    else:
        lose = RNG.randint(max(0, win - 5), win - 1)
    return (win, lose) if a_wins else (lose, win)


def round_robin(n: int) -> list[list[tuple[int, int]]]:
    """Kružni sistem (circle method): n-1 kola, svako sa n/2 parova."""
    arr = list(range(n))
    rounds = []
    for r in range(n - 1):
        pairs = []
        for i in range(n // 2):
            x, y = arr[i], arr[n - 1 - i]
            pairs.append((y, x) if (i == 0 and r % 2) else (x, y))
        rounds.append(pairs)
        arr = [arr[0], arr[-1]] + arr[1:-1]
    return rounds


ROUND_DAYS = [dt.date(2026, 9, 19) + dt.timedelta(weeks=w, days=d) for w in range(8) for d in (0, 1)]


def round_start(round_no: int) -> dt.datetime:
    """16 vikend dana za 18 kola: kola 17 i 18 igraju se posle podne poslednjeg vikenda."""
    if round_no <= 16:
        return dt.datetime.combine(ROUND_DAYS[round_no - 1], dt.time(12, 0))
    return dt.datetime.combine(ROUND_DAYS[round_no - 3], dt.time(17, 0))


def group_stage(players: list[Player]) -> dict[int, list[Game]]:
    order = players[:]
    RNG.shuffle(order)
    groups: dict[int, list[Player]] = {}
    for g in range(1, GROUPS + 1):
        groups[g] = order[(g - 1) * GROUP_SIZE:g * GROUP_SIZE]
        for p in groups[g]:
            p.group = g
    legs = round_robin(GROUP_SIZE)
    schedule = legs + [[(y, x) for x, y in leg] for leg in legs]  # drugi krug: zamenjeni domaćin i gost
    games: dict[int, list[Game]] = {g: [] for g in groups}
    for round_no, pairs in enumerate(schedule, 1):
        start = round_start(round_no)
        for slot, (i, j) in enumerate(pairs):
            for g in groups:
                a, b = groups[g][i], groups[g][j]
                idx = slot * GROUPS + (g - 1)
                court = "Teren A" if idx % 2 == 0 else "Teren B"
                when = start + dt.timedelta(minutes=10 * (idx // 2))
                r = RNG.random()
                if r < 0.05:  # nije se pojavio: službeni rezultat
                    absent_a = RNG.random() < 0.5
                    s1, s2 = (FORFEIT_LOSER_SCORE, FORFEIT_WINNER_SCORE) if absent_a else (FORFEIT_WINNER_SCORE, FORFEIT_LOSER_SCORE)
                    game = Game(a.pid, b.pid, s1, s2, "WALKOVER", "Službeni rezultat — nije se pojavio", round_no, None, when)
                elif r < 0.062:  # predaja tokom meča
                    a_quits = a.strength < b.strength
                    s1, s2 = (FORFEIT_LOSER_SCORE, FORFEIT_WINNER_SCORE) if a_quits else (FORFEIT_WINNER_SCORE, FORFEIT_LOSER_SCORE)
                    game = Game(a.pid, b.pid, s1, s2, "FORFEIT", "Predao meč", round_no, court, when)
                else:
                    s1, s2 = play(a, b)
                    game = Game(a.pid, b.pid, s1, s2, "FINISHED", None, round_no, court, when)
                games[g].append(game)
    # Jedan obostrani službeni rezultat (D-23): nijedan se nije pojavio, 0:0, bez pobednika.
    g = RNG.randint(1, GROUPS)
    victim = RNG.choice([x for x in games[g] if x.round_no == 13])
    victim.s1 = victim.s2 = FORFEIT_LOSER_SCORE
    victim.status, victim.note, victim.court = "DOUBLE_FORFEIT", "Obojica kažnjena — službeni rezultat", None
    return games


# ---------------------------------------------------------------- BR-03, isto kao engine/TieBreak
def standings(pids: list[int], games: list[Game]) -> tuple[list[int], dict[int, Totals]]:
    acc = {p: Totals(p) for p in pids}
    for m in games:
        a, b = acc[m.p1], acc[m.p2]
        a.played += 1; a.pf += m.s1; a.pa += m.s2
        b.played += 1; b.pf += m.s2; b.pa += m.s1
        if m.status == "DOUBLE_FORFEIT":
            for t in (a, b):
                t.losses += 1
                t.points += POINTS_FORFEIT
            continue
        w, l = (a, b) if m.s1 > m.s2 else (b, a)
        w.wins += 1
        w.points += POINTS_WIN
        l.losses += 1
        l.points += POINTS_FORFEIT if m.status in ("WALKOVER", "FORFEIT") else POINTS_LOSS
    by_points: dict[int, list[Totals]] = {}
    for p in pids:
        by_points.setdefault(acc[p].points, []).append(acc[p])
    order: list[int] = []
    for pts in sorted(by_points, reverse=True):
        tied = by_points[pts]
        order += [tied[0].pid] if len(tied) == 1 else resolve(tied, games)
    return order, acc


def mini_table(tied: list[Totals], games: list[Game]) -> dict[int, dict[str, int]]:
    ids = {t.pid for t in tied}
    mini = {t.pid: {"wins": 0, "points": 0, "pf": 0, "pa": 0} for t in tied}
    for m in games:
        if m.p1 not in ids or m.p2 not in ids:
            continue
        a, b = mini[m.p1], mini[m.p2]
        a["pf"] += m.s1; a["pa"] += m.s2
        b["pf"] += m.s2; b["pa"] += m.s1
        if m.status == "DOUBLE_FORFEIT":
            a["points"] += POINTS_FORFEIT
            b["points"] += POINTS_FORFEIT
            continue
        w, l = (a, b) if m.s1 > m.s2 else (b, a)
        w["wins"] += 1
        w["points"] += POINTS_WIN
        l["points"] += POINTS_FORFEIT if m.status in ("WALKOVER", "FORFEIT") else POINTS_LOSS
    return mini


def resolve(tied: list[Totals], games: list[Game]) -> list[int]:
    if len(tied) == 1:
        return [tied[0].pid]
    mini = mini_table(tied, games)
    if len(tied) == 2:
        a, b = tied
        for key in (lambda r: r["wins"], lambda r: r["pf"] - r["pa"]):
            va, vb = key(mini[a.pid]), key(mini[b.pid])
            if va != vb:
                return [a.pid, b.pid] if va > vb else [b.pid, a.pid]
        return whole_group(tied)
    for key in (lambda r: r["points"], lambda r: r["pf"] - r["pa"], lambda r: r["pf"]):
        parts: dict[int, list[Totals]] = {}
        for t in tied:
            parts.setdefault(key(mini[t.pid]), []).append(t)
        if len(parts) > 1:
            out: list[int] = []
            for v in sorted(parts, reverse=True):
                out += resolve(parts[v], games)
            return out
    return whole_group(tied)


def whole_group(tied: list[Totals]) -> list[int]:
    return [t.pid for t in sorted(tied, key=lambda t: (-t.diff, -t.pf, t.pid))]


# ---------------------------------------------------------------- playoff
def bracket_order(n: int) -> list[int]:
    """Redosled nosilaca u drvetu: 1 i 2 mogu da se sretnu tek u finalu."""
    order = [1]
    while len(order) < n:
        m = len(order) * 2
        order = [x for s in order for x in (s, m + 1 - s)]
    return order


def playoff(players: list[Player], games: dict[int, list[Game]]) -> list[Series]:
    by_pid = {p.pid: p for p in players}
    rows = []
    for g, glist in games.items():
        pids = [p.pid for p in players if p.group == g]
        order, acc = standings(pids, glist)
        for pos, pid in enumerate(order, 1):
            t = acc[pid]
            rows.append((pos, t.points, t.diff, t.pf, pid, g))
    direct = sorted((r for r in rows if r[0] <= 6), key=lambda r: (r[0], -r[1], -r[2], -r[3], r[4]))
    sevenths = sorted((r for r in rows if r[0] == 7), key=lambda r: (-r[1], -r[2], -r[3], r[4]))[:4]
    seeded = direct + sevenths  # nosioci 1…64
    line = [seeded[s - 1] for s in bracket_order(64)]
    pairs = [[line[2 * i], line[2 * i + 1]] for i in range(32)]
    # Dva igrača iz iste grupe se ne sreću u prvom kolu: slabiji nosilac menja mesto sa najbližim.
    for i, (a, b) in enumerate(pairs):
        if a[5] != b[5]:
            continue
        best = None
        for j, (c, d) in enumerate(pairs):
            if j != i and d[5] != a[5] and b[5] != c[5]:
                dist = abs(seeded.index(d) - seeded.index(b))
                if best is None or dist < best[0]:
                    best = (dist, j)
        j = best[1]
        pairs[i][1], pairs[j][1] = pairs[j][1], pairs[i][1]

    rounds = {1: 32, 2: 16, 3: 8, 4: 4, 5: 2, 6: 1}
    tree = {(r, pos): Series(r, pos) for r, cnt in rounds.items() for pos in range(1, cnt + 1)}
    third = Series(6, 1, third_place=True)
    for pos, (a, b) in enumerate(pairs, 1):
        s = tree[(1, pos)]
        s.p1, s.seed1 = a[4], f"{a[0]}. Gr. {a[5]}"
        s.p2, s.seed2 = b[4], f"{b[0]}. Gr. {b[5]}"

    walkovers = set(RNG.sample(range(1, 33), 3))
    days = {1: (dt.date(2026, 11, 14), dt.date(2026, 11, 15)), 2: (dt.date(2026, 11, 21), dt.date(2026, 11, 22)),
            3: (dt.date(2026, 11, 28), dt.date(2026, 11, 28))}
    for r in (1, 2, 3):
        count = rounds[r]
        for pos in range(1, count + 1):
            s = tree[(r, pos)]
            half = 0 if pos <= count // 2 else 1
            k = pos - 1 - half * (count // 2)
            s.court = "Teren A" if k % 2 == 0 else "Teren B"
            s.start = dt.datetime.combine(days[r][half], dt.time(13, 0)) + dt.timedelta(minutes=30 * (k // 2))
            if r == 3:
                continue  # Top 16 je zakazan, još se ne igra
            if r == 1 and pos in walkovers:
                s.winner, s.walkover, s.court, s.start = s.p1, True, None, None
                s.note = "Službeni rezultat — protivnik se nije pojavio"
            else:
                wins = {s.p1: 0, s.p2: 0}
                n = 0
                while max(wins.values()) < 2:  # do dve pobede (BR-08)
                    n += 1
                    s1, s2 = play(by_pid[s.p1], by_pid[s.p2])
                    s.games.append(Game(s.p1, s.p2, s1, s2, "FINISHED", None, r, s.court, s.start))
                    wins[s.p1 if s1 > s2 else s.p2] += 1
                s.winner = max(wins, key=wins.get)
            nxt = tree[(r + 1, (pos + 1) // 2)]  # pobednik pozicije p ide na (p+1)/2 (engine/BracketMath)
            if pos % 2 == 1:
                nxt.p1 = s.winner
            else:
                nxt.p2 = s.winner
    return [tree[k] for k in sorted(tree)] + [third]


# ---------------------------------------------------------------- SQL
def q(v) -> str:
    if v is None:
        return "null"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, int):
        return str(v)
    if isinstance(v, dt.datetime):
        return f"'{v:%Y-%m-%d %H:%M}'"
    return "'" + str(v).replace("'", "''") + "'"


HEADER = """-- GENERISANO skriptom scripts/generisi_demo_seed.py — ne menjati rucno, nego ponovo pokrenuti skriptu.
-- Izmisljena liga za lokalni razvoj (profil "dev"): imena, nadimci i rezultati ne pripadaju stvarnim osobama.
"""


def write_v7(players: list[Player]) -> str:
    out = [HEADER, """-- V7 (dev) — 100 igraca sezone 2026/2027 i njihov raspored u 10 grupa.
-- Grupe iz V4 se zovu slovima; preimenuju se u „Grupa 1"…„Grupa 10" umesto da se V4 menja.

update groups g
set name = 'Grupa ' || case g.name
                         when 'Grupa A' then '1'  when 'Grupa B' then '2'
                         when 'Grupa V' then '3'  when 'Grupa G' then '4'
                         when 'Grupa D' then '5'  when 'Grupa Đ' then '6'
                         when 'Grupa E' then '7'  when 'Grupa Ž' then '8'
                         when 'Grupa Z' then '9'  when 'Grupa I' then '10'
                       end
from stages st
         join seasons s on s.season_id = st.season_id
where g.stage_id = st.stage_id
  and s.name = '2026/2027'
  and st.order_no = 1
  and g.name in ('Grupa A','Grupa B','Grupa V','Grupa G','Grupa D',
                 'Grupa Đ','Grupa E','Grupa Ž','Grupa Z','Grupa I');

insert into players (first_name, last_name, nickname, phone, height_cm, weight_kg,
                     birth_year, payment_note, note, status) values
"""]
    rows = [f"    ({q(p.first)}, {q(p.last)}, {q(p.nickname)}, null, {p.height}, {p.weight}, {p.birth_year}, "
            f"{q(p.payment)}, {q(p.note)}, 'ACTIVE')" for p in players]
    out.append(",\n".join(rows) + ";\n")
    out.append("""
-- Raspored u grupe (UC-ADM-07). Igrac se trazi po imenu, prezimenu i nadimku — ID-jevi se ne pogadjaju.
insert into group_entries (group_id, player_id)
select g.group_id, p.player_id
from (values
""")
    by_group = sorted(players, key=lambda p: (p.group, p.pid))
    out.append(",\n".join(f"    ('Grupa {p.group}', {q(p.first)}, {q(p.last)}, {q(p.nickname)})" for p in by_group))
    out.append("""
) as raspored(grupa, ime, prezime, nadimak)
         join players p on p.first_name = raspored.ime
             and p.last_name = raspored.prezime
             and p.nickname = raspored.nadimak
         join groups g on g.name = raspored.grupa
         join stages st on st.stage_id = g.stage_id and st.order_no = 1
         join seasons s on s.season_id = st.season_id and s.name = '2026/2027';
""")
    return "".join(out)


def write_v18(players: list[Player], games: dict[int, list[Game]], bracket: list[Series]) -> str:
    name = {p.pid: p.full for p in players}
    total_games = sum(len(s.games) for s in bracket)
    resolved = sum(1 for s in bracket if s.winner and not s.walkover)
    walkover = sum(1 for s in bracket if s.walkover)
    scheduled = sum(1 for s in bracket if s.p1 and s.p2 and not s.winner)
    out = [HEADER, f"""-- V18 (dev) — cela grupna faza i playoff drvo.
--   1. 900 meceva grupne faze (10 grupa x 90, dvokruzno, 18 kola): odigrani, sluzbeni rezultati
--      (WALKOVER, FORFEIT) i jedan obostrani sluzbeni rezultat (DOUBLE_FORFEIT, odluka D-23);
--   2. grupna faza -> FINISHED, playoff -> ACTIVE; i sluzbeni poraz nosi 1 bod (odluka D-22);
--   3. kola playoff-a: do cetvrtfinala na dve pobede, Final Four jedna utakmica do 16 (najvise 17);
--   4. 64 serije: prvih 6 iz svake grupe i 4 najbolja sedmoplasirana; Top 64 i Top 32 odigrani,
--      Top 16 zakazan. Pobednik pozicije p je vec upisan na poziciju (p+1)/2 sledeceg kola.
-- Ukupno: {total_games} meceva serija; serija resena: {resolved}, resena sluzbeno: {walkover}, zakazana: {scheduled}.
-- Tabele grupa preracunava StandingsBootstrap pri pokretanju (BR-12.3).

-- ---------------------------------------------------------------- 1. mecevi grupne faze
create temp table demo_group_matches (
    group_name      varchar(40),
    round_number    integer,
    player1         varchar(130),
    player2         varchar(130),
    score1          integer,
    score2          integer,
    status          varchar(20),
    court           varchar(60),
    scheduled_start timestamp,
    note            varchar(500)
) on commit drop;

insert into demo_group_matches values
"""]
    rows = []
    for g in sorted(games):
        for m in sorted(games[g], key=lambda m: (m.round_no, m.start)):
            rows.append(f"    ('Grupa {g}', {m.round_no}, {q(name[m.p1])}, {q(name[m.p2])}, {m.s1}, {m.s2}, "
                        f"{q(m.status)}, {q(m.court)}, {q(m.start)}, {q(m.note)})")
    out.append(",\n".join(rows) + ";\n")
    out.append("""
insert into matches (stage_id, group_id, round_number, player1_id, player2_id, score_player1, score_player2,
                     winner_id, status, court_id, scheduled_start, note)
select g.stage_id, g.group_id, t.round_number, p1.player_id, p2.player_id, t.score1, t.score2,
       case when t.status = 'DOUBLE_FORFEIT' then null
            when t.score1 > t.score2 then p1.player_id
            else p2.player_id end,
       t.status, c.court_id, t.scheduled_start, t.note
from demo_group_matches t
         join groups g on g.name = t.group_name
         join stages st on st.stage_id = g.stage_id and st.type = 'GROUP'
         join seasons s on s.season_id = st.season_id and s.name = '2026/2027'
         join players p1 on p1.first_name || ' ' || p1.last_name = t.player1
         join players p2 on p2.first_name || ' ' || p2.last_name = t.player2
         left join courts c on c.name = t.court;

-- ---------------------------------------------------------------- 2. faze
update seasons set points_for_forfeit = 1 where name = '2026/2027';
update stages set status = 'FINISHED'
where type = 'GROUP' and season_id = (select season_id from seasons where name = '2026/2027');
update stages set status = 'ACTIVE'
where type = 'KNOCKOUT' and season_id = (select season_id from seasons where name = '2026/2027');

-- ---------------------------------------------------------------- 3. kola playoff-a
insert into stage_rounds (stage_id, round_no, name, wins_required, target_score, max_score, duration_minutes)
select st.stage_id, v.round_no, v.name, v.wins_required, v.target_score, v.max_score, v.duration_minutes
from stages st
         join seasons s on s.season_id = st.season_id
         cross join (values
             (1, 'Top 64', 2, null::int, null::int, null::int),
             (2, 'Top 32', 2, null::int, null::int, null::int),
             (3, 'Top 16', 2, null::int, null::int, null::int),
             (4, 'Četvrtfinale', 2, null::int, null::int, null::int),
             (5, 'Polufinale', 1, 16::int, 17::int, 15::int),
             (6, 'Finale', 1, 16::int, 17::int, 15::int)
         ) as v (round_no, name, wins_required, target_score, max_score, duration_minutes)
where s.name = '2026/2027' and st.type = 'KNOCKOUT';

-- ---------------------------------------------------------------- 4. serije i njihovi mecevi
create temp table demo_series (
    round_no        integer,
    third_place     boolean,
    position        integer,
    player1         varchar(130),
    seed1           varchar(40),
    player2         varchar(130),
    seed2           varchar(40),
    winner          varchar(130),
    walkover        boolean,
    court           varchar(60),
    scheduled_start timestamp,
    note            varchar(500)
) on commit drop;

insert into demo_series values
""")
    rows = [f"    ({s.round_no}, {q(s.third_place)}, {s.position}, {q(name.get(s.p1))}, {q(s.seed1)}, "
            f"{q(name.get(s.p2))}, {q(s.seed2)}, {q(name.get(s.winner))}, {q(s.walkover)}, {q(s.court)}, "
            f"{q(s.start)}, {q(s.note)})" for s in bracket]
    out.append(",\n".join(rows) + ";\n")
    out.append("""
insert into series (stage_round_id, position, third_place, player1_id, seed1, player2_id, seed2,
                    winner_id, walkover, court_id, scheduled_start, note)
select r.stage_round_id, t.position, t.third_place, p1.player_id, t.seed1, p2.player_id, t.seed2,
       pw.player_id, t.walkover, c.court_id, t.scheduled_start, t.note
from demo_series t
         join stage_rounds r on r.round_no = t.round_no
         join stages st on st.stage_id = r.stage_id and st.type = 'KNOCKOUT'
         join seasons s on s.season_id = st.season_id and s.name = '2026/2027'
         left join players p1 on p1.first_name || ' ' || p1.last_name = t.player1
         left join players p2 on p2.first_name || ' ' || p2.last_name = t.player2
         left join players pw on pw.first_name || ' ' || pw.last_name = t.winner
         left join courts c on c.name = t.court;

create temp table demo_games (
    round_no        integer,
    third_place     boolean,
    position        integer,
    game_number     integer,
    score1          integer,
    score2          integer,
    court           varchar(60),
    scheduled_start timestamp
) on commit drop;

insert into demo_games values
""")
    rows = [f"    ({s.round_no}, {q(s.third_place)}, {s.position}, {n}, {m.s1}, {m.s2}, {q(m.court)}, {q(m.start)})"
            for s in bracket for n, m in enumerate(s.games, 1)]
    out.append(",\n".join(rows) + ";\n")
    out.append(f"""
insert into matches (stage_id, group_id, series_id, game_number, round_number, player1_id, player2_id,
                     score_player1, score_player2, winner_id, status, court_id, scheduled_start)
select st.stage_id, null, x.series_id, g.game_number, g.round_no, x.player1_id, x.player2_id,
       g.score1, g.score2, case when g.score1 > g.score2 then x.player1_id else x.player2_id end,
       'FINISHED', c.court_id, g.scheduled_start
from demo_games g
         join stage_rounds r on r.round_no = g.round_no
         join stages st on st.stage_id = r.stage_id and st.type = 'KNOCKOUT'
         join seasons s on s.season_id = st.season_id and s.name = '2026/2027'
         join series x on x.stage_round_id = r.stage_round_id
             and x.position = g.position and x.third_place = g.third_place
         left join courts c on c.name = g.court
order by g.round_no, g.position, g.game_number;

do $$
declare
    group_matches integer;
    series_total  integer;
    series_games  integer;
begin
    select count(*) into group_matches
    from matches m join stages st on st.stage_id = m.stage_id join seasons s on s.season_id = st.season_id
    where s.name = '2026/2027' and st.type = 'GROUP';
    select count(*) into series_total
    from series x join stage_rounds r on r.stage_round_id = x.stage_round_id
             join stages st on st.stage_id = r.stage_id join seasons s on s.season_id = st.season_id
    where s.name = '2026/2027';
    select count(*) into series_games
    from matches m join stages st on st.stage_id = m.stage_id join seasons s on s.season_id = st.season_id
    where s.name = '2026/2027' and st.type = 'KNOCKOUT';
    if group_matches <> 900 or series_total <> 64 or series_games <> {total_games} then
        raise exception 'V18: ocekivano 900 meceva grupne faze, 64 serije i {total_games} meceva serija, a ima %, %, %',
            group_matches, series_total, series_games;
    end if;
end $$;
""")
    return "".join(out)


def main() -> None:
    players = make_players()
    games = group_stage(players)
    bracket = playoff(players, games)
    (OUT / "V7__seed_dev_demo_igraci_i_grupe.sql").write_text(write_v7(players), encoding="utf-8")
    (OUT / "V18__seed_dev_demo_rezultati_i_playoff.sql").write_text(write_v18(players, games, bracket), encoding="utf-8")
    print(f"napisano u {OUT}: {len(players)} igraca, {sum(len(g) for g in games.values())} meceva grupne faze, "
          f"{sum(len(s.games) for s in bracket)} meceva playoff-a")


if __name__ == "__main__":
    main()
