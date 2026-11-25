-- Seed za lokalni razvoj (profil "dev"). Verzija je V4 jer je V3 zauzeta baznom migracijom —
-- Flyway spaja lokacije db/migration i db/dev u jedan niz verzija.
-- Sezona 2026/2027 sa parametrima formata iz CLAUDE.md §6.6: 10x10, dvokruzno, 6+4 -> 64.

insert into seasons (league_id, name, start_date, end_date, player_count, status,
                     points_for_win, points_for_loss, points_for_forfeit,
                     forfeit_winner_score, forfeit_loser_score, target_score)
select l.league_id, '2026/2027', date '2026-09-19', date '2026-12-20', 100, 'ACTIVE',
       2, 1, 0, 11, 0, 11
from leagues l
where l.name = 'Floor General 1v1 Liga';

insert into stages (season_id, name, type, order_no, status,
                    group_count, group_size, round_count, direct_advance, additional_advance, bracket_size)
select s.season_id, 'Grupna faza', 'GROUP', 1, 'ACTIVE', 10, 10, 2, 6, 4, null
from seasons s
where s.name = '2026/2027';

insert into stages (season_id, name, type, order_no, status,
                    group_count, group_size, round_count, direct_advance, additional_advance, bracket_size)
select s.season_id, 'Playoff', 'KNOCKOUT', 2, 'DRAFT', null, null, null, null, null, 64
from seasons s
where s.name = '2026/2027';

-- 10 grupa grupne faze (grupe su vec izvucene van sistema — odluka D-06, admin ih samo popunjava).
insert into groups (stage_id, name)
select st.stage_id, 'Grupa ' || g.oznaka
from stages st
         join seasons s on s.season_id = st.season_id
         cross join (values ('A'), ('B'), ('V'), ('G'), ('D'), ('Đ'), ('E'), ('Ž'), ('Z'), ('I')) as g(oznaka)
where s.name = '2026/2027'
  and st.order_no = 1;
