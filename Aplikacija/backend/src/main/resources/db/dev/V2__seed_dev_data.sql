-- Seed podaci za lokalni razvoj (profil "dev"). Nikad se ne primenjuje na test/prod bazu —
-- vidi spring.flyway.locations po profilu u application*.yml.
-- Admin nalog se NE seeduje ovde: pravi ga AdminSeeder (ApplicationRunner) na prvom pokretanju.

insert into leagues (name) values
    ('Floor General 1v1 Liga'),
    ('Floor General 1v1 Liga - Rekreativna divizija');

insert into courts (name, address) values
    ('SC Šumice', 'Ustanička 125, Beograd'),
    ('SRC Olimp', 'Bulevar Mihajla Pupina 4, Novi Beograd'),
    ('Košarkaški teren Tašmajdan', 'Ilije Garašanina 26, Beograd'),
    ('Hala Pinki', 'Trg Nikole Pašića 1, Zemun');
