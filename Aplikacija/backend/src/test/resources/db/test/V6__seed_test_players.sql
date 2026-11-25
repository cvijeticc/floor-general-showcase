-- Seed ISKLJUCIVO za integracione testove UC-ADM-04 i dalje (profil "test").
-- V6 je slobodna verzija u nizu db/migration (V1, V3, V5) + db/test (V2) — mora biti POSLE V5,
-- jer V5 uvodi obavezan nadimak i ostala polja profila igraca.
-- Mali i deterministicki skup: svaki red je iskoriscen u nekom testu.

insert into seasons (league_id, name, start_date, end_date, player_count, status,
                     points_for_win, points_for_loss, points_for_forfeit,
                     forfeit_winner_score, forfeit_loser_score, target_score)
select l.league_id, 'Test sezona', date '2026-09-19', date '2026-12-20', 4, 'ACTIVE',
       2, 1, 0, 11, 0, 11
from leagues l
where l.name = 'Floor General 1v1 Liga';

insert into stages (season_id, name, type, order_no, status,
                    group_count, group_size, round_count, direct_advance, additional_advance, bracket_size)
select s.season_id, 'Grupna faza', 'GROUP', 1, 'ACTIVE', 1, 4, 2, 2, 0, null
from seasons s
where s.name = 'Test sezona';

insert into groups (stage_id, name)
select st.stage_id, 'Grupa A'
from stages st
         join seasons s on s.season_id = st.season_id
where s.name = 'Test sezona';

-- Marko Markovic: odigrao mec -> brisanje mora vratiti 409.
-- Petar Petrovic: odigrao mec, protivnik.
-- Jovan Jovanovic: u grupi, bez meca -> brisanje mora vratiti 409 zbog prijave u grupu.
-- Nikola Nikolic: nije ni u grupi ni u mecu, status ODUSTAO -> koristi se za filter i za brisanje.
insert into players (first_name, last_name, nickname, birth_year, phone,
                     height_cm, weight_kg, payment_note, note, status) values
    ('Marko', 'Marković', 'Mare', 1995, '+381601111111', 190, 85, '6500din', null, 'ACTIVE'),
    ('Petar', 'Petrović', 'Pera', 1998, '+381602222222', 185, 80, 'Free', null, 'ACTIVE'),
    ('Jovan', 'Jovanović', 'Joca', 2000, null, null, null, null, 'Ne moze subotom', 'ACTIVE'),
    ('Nikola', 'Nikolić', 'Nidza', 1992, null, null, null, null, null, 'WITHDRAWN');

insert into group_entries (group_id, player_id)
select g.group_id, p.player_id
from groups g
         join stages st on st.stage_id = g.stage_id
         join seasons s on s.season_id = st.season_id
         join players p on p.last_name in ('Marković', 'Petrović', 'Jovanović')
where s.name = 'Test sezona'
  and g.name = 'Grupa A';

insert into matches (stage_id, group_id, round_number, player1_id, player2_id,
                     score_player1, score_player2, winner_id, status, scheduled_start, note)
select st.stage_id,
       g.group_id,
       1,
       p1.player_id,
       p2.player_id,
       11,
       7,
       p1.player_id,
       'FINISHED',
       timestamp '2026-09-19 19:00:00',
       null
from stages st
         join seasons s on s.season_id = st.season_id
         join groups g on g.stage_id = st.stage_id and g.name = 'Grupa A'
         join players p1 on p1.last_name = 'Marković'
         join players p2 on p2.last_name = 'Petrović'
where s.name = 'Test sezona';
