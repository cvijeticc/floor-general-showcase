-- V3 — Faze (Stage) i prijave u grupu (GroupEntry). Specifikacija: CLAUDE.md §6.6, odluka D-10.
-- Radi se dok je baza prazna (postoje samo sifarnici liga i terena), zato se kolone prekrajaju
-- slobodno — nijedan red se ne migrira. Posle ulaska 100 igraca i 60 meceva ovo vise nije moguce.

-- Faza nosi parametre formata (spec poglavlje 4.4). Ni jedan od brojeva 10 / 10 / 2 / 6 / 4 / 64
-- ne sme postojati u kodu — svi su redovi ove tabele.
create table stages (
    stage_id bigserial primary key,
    season_id bigint not null references seasons (season_id),
    name varchar(100) not null,
    type varchar(20) not null,
    order_no integer not null,
    status varchar(20) not null,
    group_count integer,
    group_size integer,
    round_count integer,
    direct_advance integer,
    additional_advance integer,
    bracket_size integer,
    constraint uk_stages_season_order unique (season_id, order_no)
);

create index idx_stages_season on stages (season_id);

-- Grupa pripada Fazi, ne Sezoni. Do Sezone se stize preko Faze.
alter table groups drop constraint uk_groups_season_name;
alter table groups drop column season_id;
alter table groups add column stage_id bigint not null references stages (stage_id);
alter table groups add constraint uk_groups_stage_name unique (stage_id, name);

-- Igrac je sifarnik lige, nezavisan od faze; pripadnost grupi ide kroz group_entries.
-- Bez ovoga igrac moze biti u tacno jednoj grupi ikada, pa sezona sa dve grupne faze nije moguca.
create table group_entries (
    group_entry_id bigserial primary key,
    group_id bigint not null references groups (group_id),
    player_id bigint not null references players (player_id),
    constraint uk_group_entries_group_player unique (group_id, player_id)
);

create index idx_group_entries_player on group_entries (player_id);

drop index idx_players_group;
alter table players drop column group_id;

-- Playoff mec nema grupu (spec poglavlje 7), ali svaki mec pripada tacno jednoj fazi.
alter table matches alter column group_id drop not null;
alter table matches add column stage_id bigint not null references stages (stage_id);

create index idx_matches_stage_round on matches (stage_id, round_number);
