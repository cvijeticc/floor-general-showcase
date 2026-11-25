-- V5 — profil igraca po pravoj bazi lige (CLAUDE.md §6.6a, odluka D-16).
-- Izvor: .work/Baza podataka/Prijavljeni.xls (baza prijavljenih koju liga vodi od pocetka)
-- + Floor_General_League_Grupe.csv (finalnih 100 igraca po grupama).
-- Radi se pre unosa pravih podataka — tabela players je i dalje prazna u svim okruzenjima.

-- Liga ljude zove po nadimku: CSV kolona je bukvalno „Ime i nadimak", a na javnom sajtu
-- (Rok 2) nadimak je ono sto posetilac prepoznaje. Zato je obavezan, kao ime i prezime.
alter table players add column nickname varchar(60);
update players set nickname = first_name where nickname is null;
alter table players alter column nickname set not null;

-- XLS nema pun datum rodjenja nego samo GODISTE, pa date_of_birth ostaje trajno prazna kolona.
-- Kad (i ako) stigne nalog igraca sa punim datumom, date_of_birth se vraca kao NOVA kolona.
alter table players add column birth_year integer;
alter table players drop column date_of_birth;

-- XLS kolone VISINA / TEZINA. Cuvaju se kao brojevi, ne kao „196cm" — javni profil igraca
-- ih prikazuje, a kasnije se po njima moze i sortirati/filtrirati.
alter table players add column height_cm integer;
alter table players add column weight_kg integer;

-- XLS kolona UPLATA („6500din", „Free", „1500/3000din"). Obican tekst prenet iz postojece
-- evidencije — NIJE modul placanja (on je Faza 3, van MVP-a, CLAUDE.md §3.4).
alter table players add column payment_note varchar(40);

-- XLS kolona NAPOMENA — uglavnom raspolozivost („Ne moze subotom zbog posla“, „Samo posle 15h“).
-- Direktno se koristi pri zakazivanju termina.
alter table players add column note varchar(500);

-- Pretraga po nadimku je jednako cesta kao po prezimenu (UC-ADM-04, UC-FE-05).
create index idx_players_nickname on players (nickname);
create index idx_players_last_name on players (last_name);
