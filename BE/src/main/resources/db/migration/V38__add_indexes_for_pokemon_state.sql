-- Migration V38: Add indexes for foreign keys in special_conditions and pokemon_attached_cards tables to improve latency in game state build queries.

CREATE INDEX IF NOT EXISTS ix_pokemon_attached_cards_pokemon_in_play_id
ON pokemon_attached_cards (pokemon_in_play_id);

CREATE INDEX IF NOT EXISTS ix_special_conditions_pokemon_in_play_id
ON special_conditions (pokemon_in_play_id);
