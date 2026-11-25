-- Rok 3 (10.11.2026.) — playoff: kola playoff faze sa formatom i serije (BR-08, BR-09).
-- V17 jer su V11–V14 zauzete u db/dev, a V14–V15 u db/prod (Flyway spaja lokacije u jedan niz).
--
-- Model:
--   Stage (KNOCKOUT) 1─* StageRound 1─* Series 1─* Match
-- Serija je par u drvetu („Tie" iz CLAUDE.md §6.5): dva igraca, pozicija u kolu i pobednik.
-- Mecevi serije su obicni redovi u `matches` (grupa je null, kao sto je V3 predvideo za playoff),
-- pa ih profil igraca, forma i H2H vide bez ikakve izmene.

-- Kolo playoff faze nosi broj potrebnih pobeda i format meca (BR-08: „broj potrebnih pobeda je
-- po kolu konfigurabilan"). Prazan format = format sezone (target 11, max 12, 8 minuta — D-18);
-- Final Four igra do 16 poena ili 15 minuta, pa ima svoje vrednosti.
create table stage_rounds (
    stage_round_id   bigserial    primary key,
    stage_id         bigint       not null references stages (stage_id),
    round_no         integer      not null,
    name             varchar(60)  not null,
    wins_required    integer      not null default 1,
    target_score     integer,
    max_score        integer,
    duration_minutes integer,
    constraint uk_stage_rounds_stage_round unique (stage_id, round_no),
    constraint ck_stage_rounds_wins check (wins_required between 1 and 4),
    constraint ck_stage_rounds_round check (round_no >= 1)
);

-- Pozicija u kolu prati drvo: pobednik pozicije p ide u sledece kolo na poziciju (p+1)/2, kao prvi
-- igrac ako je p neparno, kao drugi ako je parno (engine/BracketMath). Mec za 3. mesto je u kolu
-- finala sa third_place = true — tu stizu porazeni iz polufinala (BR-09).
-- Pobednik se ne izvodi u letu nego se cuva: serija moze biti resena i bez meceva (nedolazak,
-- walkover = true), a „Ponisti" vraca red tacno onakav kakav je bio.
create table series (
    series_id        bigserial    primary key,
    stage_round_id   bigint       not null references stage_rounds (stage_round_id),
    position         integer      not null,
    third_place      boolean      not null default false,
    player1_id       bigint       references players (player_id),
    player2_id       bigint       references players (player_id),
    -- Oznaka nosioca sa kostura lige, npr. „1. Gr. 1" — tekst, jer je kostur odluka lige (BR-05).
    seed1            varchar(40),
    seed2            varchar(40),
    winner_id        bigint       references players (player_id),
    walkover         boolean      not null default false,
    court_id         bigint       references courts (court_id),
    scheduled_start  timestamp,
    note             varchar(500),
    constraint uk_series_round_position unique (stage_round_id, position, third_place),
    constraint ck_series_players check (player1_id is null or player2_id is null or player1_id <> player2_id)
);

create index idx_series_round on series (stage_round_id);
create index idx_series_player1 on series (player1_id);
create index idx_series_player2 on series (player2_id);

-- Mec serije: redni broj meca u seriji (1, 2, 3). Grupni mecevi ostaju bez serije.
alter table matches add column series_id bigint references series (series_id);
alter table matches add column game_number integer;

create index idx_matches_series on matches (series_id);

-- FR-5.1: kojim kriterijumom BR-03 je igrac razdvojen od izjednacenih (tooltip u tabeli).
-- Null = razdvojen bodovima, nije bilo izjednacenja.
alter table standings add column tie_break varchar(40);
