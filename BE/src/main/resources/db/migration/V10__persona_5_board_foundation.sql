alter table pokemon_in_play add column owner_user_id uuid;
alter table pokemon_in_play add column slot_position integer;
alter table pokemon_in_play add column damage_counters integer default 0;
alter table pokemon_in_play add column entered_play_turn integer;

alter table pokemon_in_play add constraint fk_pokemon_in_play_owner
    foreign key (owner_user_id) references users (id);

update pokemon_in_play set damage_counters = 0 where damage_counters is null;

alter table pokemon_in_play alter column owner_user_id set not null;
alter table pokemon_in_play alter column slot_position set not null;
alter table pokemon_in_play alter column damage_counters set not null;
alter table pokemon_in_play alter column entered_play_turn set not null;

alter table pokemon_in_play add constraint ck_pokemon_in_play_slot_position
    check (slot_position between 0 and 5);
alter table pokemon_in_play add constraint ck_pokemon_in_play_damage_counters
    check (damage_counters >= 0);

create unique index ux_pokemon_in_play_owner_slot
    on pokemon_in_play (game_id, owner_user_id, slot_position);

alter table pokemon_evolution_stack add column stack_order integer;
update pokemon_evolution_stack set stack_order = 0 where stack_order is null;
alter table pokemon_evolution_stack alter column stack_order set not null;
alter table pokemon_evolution_stack add constraint ck_pokemon_evolution_stack_order
    check (stack_order >= 0);
create unique index ux_pokemon_evolution_stack_order
    on pokemon_evolution_stack (pokemon_in_play_id, stack_order);

alter table pokemon_attached_cards add column attached_card_type varchar(40);
update pokemon_attached_cards set attached_card_type = 'BASIC_ENERGY' where attached_card_type is null;
alter table pokemon_attached_cards alter column attached_card_type set not null;

alter table special_conditions add column condition_type varchar(40);
update special_conditions set condition_type = 'ASLEEP' where condition_type is null;
alter table special_conditions alter column condition_type set not null;
