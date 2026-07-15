alter table special_conditions add column applied_turn integer;
update special_conditions set applied_turn = 0 where applied_turn is null;
alter table special_conditions alter column applied_turn set not null;

create unique index ux_special_condition_unique_type
    on special_conditions (pokemon_in_play_id, condition_type);
