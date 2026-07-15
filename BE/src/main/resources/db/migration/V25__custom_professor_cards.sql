alter table cards drop constraint if exists ck_cards_set_xy1;

alter table cards add column if not exists source varchar(40);
update cards set source = 'POKEMONTCG_IO' where source is null;
alter table cards alter column source set default 'POKEMONTCG_IO';
alter table cards alter column source set not null;

alter table cards drop constraint if exists ck_cards_playable_set;
alter table cards add constraint ck_cards_playable_set check (set_code in ('xy1', 'custom-professors'));
create index if not exists ix_cards_source on cards (source);

insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, subtype, evolves_from, hp, pokemon_type, retreat_cost, image_small_url, image_large_url, raw_json, source, updated_at)
select '00000000-0000-0000-0000-000000025001', 'custom-professors-pr-001', 'custom-professors', 'Profesores TPI', 'PR-001', 'AngularQuin', 'POKEMON', 'BASIC_POKEMON', 'Basic', null, 80, 'Lightning', 1, 'https://kommodo.ai/i/F3atm1eW9N3XsI4D7iJq', 'https://kommodo.ai/i/F3atm1eW9N3XsI4D7iJq', '{"source":"CUSTOM_PROFESSORS","rarity":"Uncommon","description":"Pokémon eléctrico de mente veloz, capaz de transformar ideas complejas en explicaciones claras antes de que sus rivales puedan reaccionar.","notes":"Inspirado en Joaquín. Representa FrontEnd, agilidad mental, explicación clara, lentes, interfaces digitales, circuitos luminosos y energía constante.","theme":"Rapidez técnica, electricidad, código, interfaces, claridad y proactividad."}', 'CUSTOM_PROFESSORS', current_timestamp
where not exists (select 1 from cards where external_id = 'custom-professors-pr-001');

insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, subtype, evolves_from, hp, pokemon_type, retreat_cost, image_small_url, image_large_url, raw_json, source, updated_at)
select '00000000-0000-0000-0000-000000025002', 'custom-professors-pr-002', 'custom-professors', 'Profesores TPI', 'PR-002', 'RaMaven', 'POKEMON', 'BASIC_POKEMON', 'Basic', null, 110, 'Metal', 2, '', '', '{"source":"CUSTOM_PROFESSORS","rarity":"Rare","description":"Pokémon metálico de enorme tamaño y corazón tranquilo. No suele llamar la atención, pero aparece siempre que alguien necesita sostén, paciencia o una respuesta sólida.","notes":"Inspirado en Ramiro Romera. Representa BackEnd, servidores, bases de datos, engranajes, estructura interna, timidez, nobleza y ayuda constante.","theme":"Gigante amable, defensa, estabilidad, soporte silencioso y solidez backend."}', 'CUSTOM_PROFESSORS', current_timestamp
where not exists (select 1 from cards where external_id = 'custom-professors-pr-002');

insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, subtype, evolves_from, hp, pokemon_type, retreat_cost, image_small_url, image_large_url, raw_json, source, updated_at)
select '00000000-0000-0000-0000-000000025003', 'custom-professors-pr-003', 'custom-professors', 'Profesores TPI', 'PR-003', 'BelgraNode', 'POKEMON', 'BASIC_POKEMON', 'Basic', null, 90, 'Darkness', 1, '', '', '{"source":"CUSTOM_PROFESSORS","rarity":"Rare","description":"Pokémon corsario de pocas palabras y mirada cansada. Parece relajado, pero cada intervención llega con precisión oscura y deja una marca difícil de ignorar.","notes":"Inspirado en Fabio Mercado. Representa FrontEnd, humor sarcástico, estética pirata, oscuridad elegante, celeste, negro, blanco y actitud distendida pero peligrosa.","theme":"Pirata sombrío, sarcasmo, ofensiva precisa, celeste cordobés, interfaces y comentarios filosos."}', 'CUSTOM_PROFESSORS', current_timestamp
where not exists (select 1 from cards where external_id = 'custom-professors-pr-003');

insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, subtype, evolves_from, hp, pokemon_type, retreat_cost, image_small_url, image_large_url, raw_json, source, updated_at)
select '00000000-0000-0000-0000-000000025004', 'custom-professors-pr-004', 'custom-professors', 'Profesores TPI', 'PR-004', 'SantorMento', 'POKEMON', 'BASIC_POKEMON', 'Basic', null, 130, 'Fire', 3, '', '', '{"source":"CUSTOM_PROFESSORS","rarity":"Rare Holo","description":"Pokémon ígneo de presencia dominante. Su cuerpo pesado arde como una armadura volcánica y su mirada parece desafiar a cualquiera que entre al aula sin estar preparado.","notes":"Inspirado en Exequiel Santoro. Representa coordinación, autoridad, provocación teatral, humor filoso, fuego, resistencia, rojo, cuernos, brasas y presencia de guardián infernal.","theme":"Tanque de fuego, presión, provocación, diablo de aula, autoridad y resistencia."}', 'CUSTOM_PROFESSORS', current_timestamp
where not exists (select 1 from cards where external_id = 'custom-professors-pr-004');

insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, subtype, evolves_from, hp, pokemon_type, retreat_cost, image_small_url, image_large_url, raw_json, source, updated_at)
select '00000000-0000-0000-0000-000000025005', 'custom-professors-pr-005', 'custom-professors', 'Profesores TPI', 'PR-005', 'HernullPointer', 'POKEMON', 'BASIC_POKEMON', 'Basic', null, 120, 'Psychic', 2, '', '', '{"source":"CUSTOM_PROFESSORS","rarity":"Rare Holo","description":"Pokémon psíquico de conocimiento profundo. Sus orbes mentales analizan cada movimiento y revelan de inmediato cuando un rival no estudió lo suficiente.","notes":"Inspirado en Hernán Jesús Morais. Representa BackEnd, arquitectura, disciplina, exigencia, conocimiento avanzado, mente analítica, libros flotantes, código y estructuras geométricas.","theme":"Sabiduría técnica, poder mental, arquitectura backend, exigencia, precisión y enseñanza profunda."}', 'CUSTOM_PROFESSORS', current_timestamp
where not exists (select 1 from cards where external_id = 'custom-professors-pr-005');

insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025101', c.id, 'RelámpagoLM', '20', 20, null, 0 from cards c
where c.external_id = 'custom-professors-pr-001' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025101');
insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025102', c.id, 'TypeScriptazo', '50', 50, null, 1 from cards c
where c.external_id = 'custom-professors-pr-001' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025102');
insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025201', c.id, 'Scirocco', '20', 20, null, 0 from cards c
where c.external_id = 'custom-professors-pr-002' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025201');
insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025202', c.id, 'Scaffolding Destructor', '70', 70, null, 1 from cards c
where c.external_id = 'custom-professors-pr-002' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025202');
insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025301', c.id, 'Roba Tokens', '30', 30, null, 0 from cards c
where c.external_id = 'custom-professors-pr-003' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025301');
insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025302', c.id, 'Deploy Pirata', '80', 80, null, 1 from cards c
where c.external_id = 'custom-professors-pr-003' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025302');
insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025401', c.id, 'Cuestionario Incendiario', '30', 30, null, 0 from cards c
where c.external_id = 'custom-professors-pr-004' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025401');
insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025402', c.id, 'Dos del Cornudo', '90', 90, null, 1 from cards c
where c.external_id = 'custom-professors-pr-004' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025402');
insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025501', c.id, 'Ruleta Denigrante', '30', 30, null, 0 from cards c
where c.external_id = 'custom-professors-pr-005' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025501');
insert into attacks (id, card_id, name, damage_text, base_damage, effect_text, attack_order)
select '00000000-0000-0000-0000-000000025502', c.id, 'Singleton Supremo', '90', 90, null, 1 from cards c
where c.external_id = 'custom-professors-pr-005' and not exists (select 1 from attacks where id = '00000000-0000-0000-0000-000000025502');

insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026001', '00000000-0000-0000-0000-000000025101', 'Lightning', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026001');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026002', '00000000-0000-0000-0000-000000025102', 'Lightning', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026002');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026003', '00000000-0000-0000-0000-000000025102', 'Colorless', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026003');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026004', '00000000-0000-0000-0000-000000025201', 'Metal', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026004');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026005', '00000000-0000-0000-0000-000000025202', 'Metal', 2 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026005');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026006', '00000000-0000-0000-0000-000000025202', 'Colorless', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026006');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026007', '00000000-0000-0000-0000-000000025301', 'Darkness', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026007');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026008', '00000000-0000-0000-0000-000000025302', 'Darkness', 2 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026008');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026009', '00000000-0000-0000-0000-000000025302', 'Colorless', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026009');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026010', '00000000-0000-0000-0000-000000025401', 'Fire', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026010');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026011', '00000000-0000-0000-0000-000000025401', 'Colorless', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026011');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026012', '00000000-0000-0000-0000-000000025402', 'Fire', 2 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026012');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026013', '00000000-0000-0000-0000-000000025402', 'Colorless', 2 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026013');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026014', '00000000-0000-0000-0000-000000025501', 'Psychic', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026014');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026015', '00000000-0000-0000-0000-000000025502', 'Psychic', 2 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026015');
insert into attack_costs (id, attack_id, energy_type, quantity) select '00000000-0000-0000-0000-000000026016', '00000000-0000-0000-0000-000000025502', 'Colorless', 1 where not exists (select 1 from attack_costs where id = '00000000-0000-0000-0000-000000026016');

insert into card_weaknesses (id, card_id, energy_type, multiplier) select '00000000-0000-0000-0000-000000027001', c.id, 'Fighting', 'x2' from cards c where c.external_id = 'custom-professors-pr-001' and not exists (select 1 from card_weaknesses where id = '00000000-0000-0000-0000-000000027001');
insert into card_weaknesses (id, card_id, energy_type, multiplier) select '00000000-0000-0000-0000-000000027002', c.id, 'Fire', 'x2' from cards c where c.external_id = 'custom-professors-pr-002' and not exists (select 1 from card_weaknesses where id = '00000000-0000-0000-0000-000000027002');
insert into card_weaknesses (id, card_id, energy_type, multiplier) select '00000000-0000-0000-0000-000000027003', c.id, 'Fighting', 'x2' from cards c where c.external_id = 'custom-professors-pr-003' and not exists (select 1 from card_weaknesses where id = '00000000-0000-0000-0000-000000027003');
insert into card_weaknesses (id, card_id, energy_type, multiplier) select '00000000-0000-0000-0000-000000027004', c.id, 'Water', 'x2' from cards c where c.external_id = 'custom-professors-pr-004' and not exists (select 1 from card_weaknesses where id = '00000000-0000-0000-0000-000000027004');
insert into card_weaknesses (id, card_id, energy_type, multiplier) select '00000000-0000-0000-0000-000000027005', c.id, 'Darkness', 'x2' from cards c where c.external_id = 'custom-professors-pr-005' and not exists (select 1 from card_weaknesses where id = '00000000-0000-0000-0000-000000027005');

insert into card_resistances (id, card_id, energy_type, resistance_value) select '00000000-0000-0000-0000-000000028001', c.id, 'Metal', '-20' from cards c where c.external_id = 'custom-professors-pr-001' and not exists (select 1 from card_resistances where id = '00000000-0000-0000-0000-000000028001');
insert into card_resistances (id, card_id, energy_type, resistance_value) select '00000000-0000-0000-0000-000000028002', c.id, 'Psychic', '-20' from cards c where c.external_id = 'custom-professors-pr-002' and not exists (select 1 from card_resistances where id = '00000000-0000-0000-0000-000000028002');
insert into card_resistances (id, card_id, energy_type, resistance_value) select '00000000-0000-0000-0000-000000028003', c.id, 'Psychic', '-20' from cards c where c.external_id = 'custom-professors-pr-003' and not exists (select 1 from card_resistances where id = '00000000-0000-0000-0000-000000028003');
insert into card_resistances (id, card_id, energy_type, resistance_value) select '00000000-0000-0000-0000-000000028004', c.id, 'Metal', '-20' from cards c where c.external_id = 'custom-professors-pr-004' and not exists (select 1 from card_resistances where id = '00000000-0000-0000-0000-000000028004');
insert into card_resistances (id, card_id, energy_type, resistance_value) select '00000000-0000-0000-0000-000000028005', c.id, 'Fighting', '-20' from cards c where c.external_id = 'custom-professors-pr-005' and not exists (select 1 from card_resistances where id = '00000000-0000-0000-0000-000000028005');
