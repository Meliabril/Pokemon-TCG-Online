alter table games add column active_player_id uuid;
alter table games add column winner_player_id uuid;
alter table games add column pause_reason varchar(120);
alter table games add column started_at timestamp;
alter table games add column paused_at timestamp;
alter table games add column finished_at timestamp;

alter table games add constraint fk_games_active_player
    foreign key (active_player_id) references users (id);

alter table games add constraint fk_games_winner_player
    foreign key (winner_player_id) references users (id);

alter table games alter column status set not null;
alter table games alter column turn_number set not null;
alter table games alter column state_version set not null;

alter table games add constraint ck_games_turn_non_negative
    check (turn_number >= 0);

create index ix_games_status on games (status);
create index ix_games_active_player on games (active_player_id);
create index ix_games_winner_player on games (winner_player_id);

alter table game_participants add column player_order integer;
alter table game_participants add column is_connected boolean default false;
alter table game_participants add column last_seen_at timestamp;

alter table game_participants alter column game_id set not null;
alter table game_participants alter column user_id set not null;
alter table game_participants alter column deck_id set not null;
alter table game_participants alter column player_order set not null;
alter table game_participants alter column is_connected set not null;

alter table game_participants add constraint ck_player_order
    check (player_order in (1, 2));

create unique index ux_game_participant_user on game_participants (game_id, user_id);
create unique index ux_game_participant_order on game_participants (game_id, player_order);
create index ix_game_participants_user on game_participants (user_id);
