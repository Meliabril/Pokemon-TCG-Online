ALTER TABLE matchmaking_queue_entries
ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE matchmaking_queue_entries
ADD CONSTRAINT ux_matchmaking_queue_entries_user UNIQUE (user_id);
