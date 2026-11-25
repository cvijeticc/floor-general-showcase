-- Liga igra na DVA terena — „Teren A" i „Teren B" (potvrdio narucilac 22.09.2026.).
-- Cetiri terena iz V2 su bila izmisljena za lokalni rad; ovde se zamenjuju pravim stanjem,
-- da padajuca lista na ekranu „Mecevi" nudi tacno ono sto postoji.
--
-- Brisu se samo tereni na koje nije vezan nijedan mec — ako je neki vec koriscen, ostaje.

delete from courts
where name in ('SC Šumice', 'SRC Olimp', 'Košarkaški teren Tašmajdan', 'Hala Pinki')
  and not exists (select 1 from matches m where m.court_id = courts.court_id);

insert into courts (name, address)
select 'Teren A', null
where not exists (select 1 from courts where name = 'Teren A');

insert into courts (name, address)
select 'Teren B', null
where not exists (select 1 from courts where name = 'Teren B');
