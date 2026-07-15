alter table game_action_logs add column actor_user_id uuid;
alter table game_action_logs add column action_type varchar(60);
alter table game_action_logs add column payload json;
alter table game_action_logs add column result json;
alter table game_action_logs add column version integer;
alter table game_action_logs add column client_action_id uuid;

alter table game_action_logs add constraint fk_game_action_logs_actor
    foreign key (actor_user_id) references users (id);

alter table game_action_logs alter column game_id set not null;
alter table game_action_logs alter column action_type set not null;
alter table game_action_logs alter column payload set not null;
alter table game_action_logs alter column version set not null;

alter table game_action_logs add constraint ux_game_action_version unique (game_id, version);

create index ix_game_action_logs_game_created
on game_action_logs (game_id, created_at);

create unique index ux_game_actor_client_action
on game_action_logs (game_id, actor_user_id, client_action_id);

alter table game_state_snapshots rename column state_version to version;
alter table game_state_snapshots add column state_json json;
alter table game_state_snapshots add column checksum varchar(128);

alter table game_state_snapshots alter column game_id set not null;
alter table game_state_snapshots alter column version set not null;
alter table game_state_snapshots alter column state_json set not null;
alter table game_state_snapshots alter column checksum set not null;

alter table game_state_snapshots add constraint ux_game_snapshot_version unique (game_id, version);

create index ix_game_state_snapshots_latest
on game_state_snapshots (game_id, version desc);

alter table game_events alter column game_id set not null;
alter table game_events alter column event_type type varchar(60);
alter table game_events alter column event_type set not null;
alter table game_events add column payload json;
alter table game_events add column version integer;
alter table game_events add column visible_to_user_id uuid;

alter table game_events alter column payload set not null;
alter table game_events alter column version set not null;

create index ix_game_events_game_created
on game_events (game_id, created_at);

create index ix_game_events_type
on game_events (event_type);
