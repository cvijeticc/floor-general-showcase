-- Bazna sema Floor General MVP-a. Odgovara JPA entitetima u rs.floorgeneral.domain.
-- Vazi za sva okruzenja (dev, test, prod) — seed podaci idu u posebne migracije (db/dev, db/test).

create table leagues (
    league_id bigserial primary key,
    name varchar(100) not null
);

create table app_users (
    app_user_id bigserial primary key,
    username varchar(60) not null unique,
    password_hash varchar(100) not null,
    email varchar(120),
    role varchar(20) not null
);

create table courts (
    court_id bigserial primary key,
    name varchar(60) not null,
    address varchar(160)
);

create table seasons (
    season_id bigserial primary key,
    league_id bigint not null references leagues (league_id),
    name varchar(100) not null,
    start_date date,
    end_date date,
    player_count integer not null,
    status varchar(20) not null,
    points_for_win integer not null,
    points_for_loss integer not null,
    points_for_forfeit integer not null,
    forfeit_winner_score integer not null,
    forfeit_loser_score integer not null,
    target_score integer not null
);

create index idx_seasons_league on seasons (league_id);

create table groups (
    group_id bigserial primary key,
    season_id bigint not null references seasons (season_id),
    name varchar(40) not null,
    constraint uk_groups_season_name unique (season_id, name)
);

create table players (
    player_id bigserial primary key,
    first_name varchar(60) not null,
    last_name varchar(60) not null,
    date_of_birth date,
    phone varchar(30),
    group_id bigint references groups (group_id),
    status varchar(20) not null
);

create index idx_players_group on players (group_id);

create table matches (
    match_id bigserial primary key,
    group_id bigint not null references groups (group_id),
    round_number integer not null,
    player1_id bigint not null references players (player_id),
    player2_id bigint not null references players (player_id),
    score_player1 integer,
    score_player2 integer,
    winner_id bigint references players (player_id),
    status varchar(20) not null,
    court_id bigint references courts (court_id),
    scheduled_start timestamp,
    note varchar(500)
);

create index idx_matches_group_round on matches (group_id, round_number);
create index idx_matches_player1 on matches (player1_id);
create index idx_matches_player2 on matches (player2_id);
create index idx_matches_scheduled_start on matches (scheduled_start);
create index idx_matches_court_start on matches (court_id, scheduled_start);

create table standings (
    standing_id bigserial primary key,
    group_id bigint not null references groups (group_id),
    player_id bigint not null references players (player_id),
    played integer not null,
    wins integer not null,
    losses integer not null,
    points_for integer not null,
    points_against integer not null,
    point_difference integer not null,
    points integer not null,
    position_in_group integer not null,
    constraint uk_standings_group_player unique (group_id, player_id)
);

create index idx_standings_group_position on standings (group_id, position_in_group);
