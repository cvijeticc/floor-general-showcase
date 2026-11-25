-- Pravi format meca, potvrdio narucilac 26.09.2026. (odluka D-18).
--
-- Mec NIJE "prvi do 11". Igra se OSAM MINUTA i pobedjuje onaj ko na kraju ima vise poena —
-- rezultat sme biti i 2:1. Jedanaest poena je granica posle koje mec moze da se zavrsi ranije,
-- ali posto sut spolja vredi dva poena, igrac koji vodi 10:7 i pogodi spolja zavrsava 12:7.
-- Zato je najveci moguci rezultat 12, a ne 11.
--
-- Do ove migracije je validacija trazila da pobednik ima TACNO target_score (11), pa je odbijala
-- i 12:7 i 8:10 — a oba postoje na zapisnicima iz prvih kola.

alter table seasons add column max_score integer not null default 12;
alter table seasons add column duration_minutes integer not null default 8;

comment on column seasons.target_score is
    'Granica posle koje mec moze da se zavrsi pre isteka vremena (BR-11). Nije obavezan rezultat.';
comment on column seasons.max_score is
    'Najveci moguci rezultat. Veci je od target_score jer sut spolja vredi dva poena (10 + 2 = 12).';
comment on column seasons.duration_minutes is
    'Trajanje meca u minutima; posle isteka pobedjuje onaj ko ima vise poena.';
