alter table games add column turn_started_at timestamp;
alter table game_participants add column consecutive_timeouts integer not null default 0;
