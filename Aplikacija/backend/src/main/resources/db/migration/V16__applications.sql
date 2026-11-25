-- D-20 (05.10.2026.): prijave u ligu sa javnog sajta. Do sada ih je primao Framer server;
-- posle prelaska sajta na nas Next.js stizu ovde, admin ih vidi u panelu i izvozi u Excel.
-- Polja prate Framer formu na floorgeneral.rs/kontakt (i kolone iz Prijavljeni.xls, §6.7).
-- V16 jer je V15 zauzeta u db/prod (Flyway spaja lokacije u jedan niz verzija).

create table applications (
    application_id bigserial    primary key,
    full_name      varchar(120) not null,
    nickname       varchar(60)  not null,
    height_cm      integer,
    weight_kg      integer,
    birth_year     integer,
    phone          varchar(30)  not null,
    email          varchar(120) not null,
    skill_level    varchar(20)  not null,
    note           varchar(500),
    about          varchar(500),
    status         varchar(20)  not null default 'NEW',
    -- Igrac napravljen iz prijave; prijava ostaje kao trag i kad se igrac obrise.
    player_id      bigint       references players (player_id) on delete set null,
    created_at     timestamp    not null default now()
);

create index idx_applications_status on applications (status, application_id desc);
