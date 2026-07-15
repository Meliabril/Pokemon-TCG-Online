alter table pokemon_in_play add column damage_protection_threshold integer;
alter table pokemon_in_play add column abilities_disabled_until_turn integer;
alter table pokemon_in_play add column next_attack_bonus_turn integer;
alter table pokemon_in_play add column next_attack_bonus_order integer;
alter table pokemon_in_play add column next_attack_bonus_amount integer;
alter table pokemon_in_play add column blocked_attack_turn integer;
alter table pokemon_in_play add column blocked_attack_order integer;
