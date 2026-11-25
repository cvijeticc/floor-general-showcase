-- UC-ADM-23 — „Ponisti" (undo): istorija admin izmena, sa prethodnim stanjem svakog dirnutog reda.
--
-- Zasto tabela, a ne stek u pregledacu: admin uvece unese ceo dan rezultata, osvezi stranu i ode.
-- Ako se sutradan ispostavi da je neko slucajno obrisan ili je rezultat unet na pogresan mec,
-- ponistavanje mora da radi i posle osvezavanja strane i posle odjave.
--
-- Oblik kolone changes (JSON, pise ga UndoService):
--   { "rows": [ { "op": "CHANGED|CREATED", "table": "players", "pkColumn": "player_id",
--                 "pkValue": "42", "values": {kolona: vrednost}, "types": {kolona: tip} } ],
--     "recomputeGroupIds": [3] }
-- CHANGED nosi stanje PRE izmene (vraca se upsert-om), CREATED nosi nov red (ponistava se brisanjem).

create table undo_entries (
    undo_entry_id bigserial primary key,
    action varchar(40) not null,
    description varchar(300) not null,
    performed_by varchar(60),
    performed_at timestamp not null,
    changes text not null,
    undone boolean not null default false,
    undone_at timestamp
);

-- Ponistavanje uvek trazi poslednju neponistenu izmenu (LIFO stek).
create index idx_undo_entries_open on undo_entries (undone, undo_entry_id desc);
