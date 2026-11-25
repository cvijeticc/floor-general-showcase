-- Seed podaci ISKLJUCIVO za integracione testove. Namerno mali i deterministicki skup —
-- svaki red ovde je iskoriscen u nekom testu, nema "tralala" podataka.
-- Admin nalog se NE seeduje ovde: pravi ga AdminSeeder (ApplicationRunner) na prvom pokretanju,
-- pa se u testovima prijavljujemo podrazumevanim admin/admin123 iz application.yml.

insert into leagues (name) values
    ('Floor General 1v1 Liga'),
    ('Floor General 1v1 Liga - Rekreativna divizija');

insert into courts (name, address) values
    ('SC Šumice', 'Ustanička 125, Beograd'),
    ('SRC Olimp', 'Bulevar Mihajla Pupina 4, Novi Beograd'),
    ('Hala Pinki', 'Trg Nikole Pašića 1, Zemun');
