package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardResistance;
import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import ar.edu.utn.frc.tup.piii.entities.GameEvent;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameEventRepository;
import ar.edu.utn.frc.tup.piii.repositories.PokemonInPlayRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

abstract class CustomProfessorAttackIntegrationTestSupport {

    private static final int ACTIVE_MAIN_STATE_VERSION = 1;
    private static final int ACTIVE_MAIN_TURN_NUMBER = 3;

    @Autowired
    protected GameService gameService;

    @Autowired
    protected CardRepository cardRepository;

    @Autowired
    protected GameRepository gameRepository;

    @Autowired
    protected PokemonInPlayRepository pokemonInPlayRepository;

    @Autowired
    protected GameEventRepository gameEventRepository;

    @Autowired
    protected GameSnapshotService gameSnapshotService;

    @Autowired
    protected GameStateQueryService gameStateQueryService;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupGameFixtures() {
        jdbcTemplate.update("delete from special_conditions");
        jdbcTemplate.update("delete from pokemon_attached_cards");
        jdbcTemplate.update("delete from pokemon_evolution_stack");
        jdbcTemplate.update("delete from pokemon_in_play");
        jdbcTemplate.update("delete from game_events");
        jdbcTemplate.update("delete from game_state_snapshots");
        jdbcTemplate.update("delete from game_action_logs");
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from deck_cards");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from attack_costs where attack_id in (select id from attacks where card_id in (select id from cards where external_id like 'test-%' or external_id like 'xy1-test-%'))");
        jdbcTemplate.update("delete from attacks where card_id in (select id from cards where external_id like 'test-%' or external_id like 'xy1-test-%')");
        jdbcTemplate.update("delete from card_weaknesses where card_id in (select id from cards where external_id like 'test-%' or external_id like 'xy1-test-%')");
        jdbcTemplate.update("delete from card_resistances where card_id in (select id from cards where external_id like 'test-%' or external_id like 'xy1-test-%')");
        jdbcTemplate.update("delete from cards where external_id like 'test-%' or external_id like 'xy1-test-%'");
        jdbcTemplate.update("delete from users where username like 'attacktest%'");
    }

    protected Card customProfessorCard(String externalId) {
        Card card = cardRepository.findByExternalId(externalId).orElseThrow();
        return cardRepository.findWithDetailsById(card.getId()).orElseThrow();
    }

    protected Attack attackByName(Card card, String attackName) {
        for (Attack attack : card.getAttacks()) {
            if (attackName.equals(attack.getName())) {
                return attack;
            }
        }
        throw new AssertionError("Attack not found: " + attackName);
    }

    protected Card saveBasicEnergy(String type) {
        Card card = new Card();
        String normalizedType = type.toLowerCase();
        card.setExternalId("test-energy-" + normalizedType + "-" + UUID.randomUUID());
        card.setSetCode(Card.XY1_SET_CODE);
        card.setSetName("XY");
        card.setSource(Card.SOURCE_POKEMONTCG_IO);
        card.setNumber("E-" + normalizedType);
        card.setName(type + " Energy");
        card.setSupertype(CardSupertype.ENERGY);
        card.setCategory(CardCategory.BASIC_ENERGY);
        card.setSubtype("Basic");
        card.setPokemonType(type);
        card.setRawJson("{}");
        return cardRepository.saveAndFlush(card);
    }

    protected Card saveTestPokemon(String externalId, String name, String type, int hp) {
        Card card = testPokemon(externalId, name, type, hp);
        return cardRepository.saveAndFlush(card);
    }

    protected Card saveTestPokemonWithWeakness(
            String externalId,
            String name,
            String type,
            int hp,
            String weaknessType,
            String weaknessValue) {
        Card card = testPokemon(externalId, name, type, hp);
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType(weaknessType);
        weakness.setMultiplier(weaknessValue);
        card.addWeakness(weakness);
        return cardRepository.saveAndFlush(card);
    }

    protected Card saveTestPokemonWithResistance(
            String externalId,
            String name,
            String type,
            int hp,
            String resistanceType,
            String resistanceValue) {
        Card card = testPokemon(externalId, name, type, hp);
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType(resistanceType);
        resistance.setValue(resistanceValue);
        card.addResistance(resistance);
        return cardRepository.saveAndFlush(card);
    }

    private Card testPokemon(String externalId, String name, String type, int hp) {
        Card card = new Card();
        card.setExternalId(externalId);
        card.setSetCode(Card.XY1_SET_CODE);
        card.setSetName("XY");
        card.setSource(Card.SOURCE_POKEMONTCG_IO);
        card.setNumber("T-1");
        card.setName(name);
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.BASIC_POKEMON);
        card.setSubtype("Basic");
        card.setHp(hp);
        card.setPokemonType(type);
        card.setRetreatCost(1);
        card.setRawJson("{}");
        return card;
    }

    protected Attack saveAttack(Card card, String name, int baseDamage, String energyType, int quantity) {
        Attack attack = new Attack();
        attack.setName(name);
        attack.setDamageText(String.valueOf(baseDamage));
        attack.setBaseDamage(baseDamage);
        attack.setEffectText(null);
        attack.setAttackOrder(card.getAttacks().size());
        AttackCost cost = new AttackCost();
        cost.setEnergyType(energyType);
        cost.setQuantity(quantity);
        attack.addCost(cost);
        card.addAttack(attack);
        Card savedCard = cardRepository.saveAndFlush(card);
        return attackByName(cardRepository.findWithDetailsById(savedCard.getId()).orElseThrow(), name);
    }

    protected AttackGameFixture insertAttackGame(Card attackerCard, Attack attack, List<Card> energyCards, Card defenderCard) {
        Instant now = Instant.parse("2026-06-25T12:00:00Z");
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID attackerDeckId = UUID.randomUUID();
        UUID defenderDeckId = UUID.randomUUID();

        insertUser(attackerUserId, "attacktestattacker" + attackerUserId.toString().replace("-", ""), now);
        insertUser(defenderUserId, "attacktestdefender" + defenderUserId.toString().replace("-", ""), now);
        insertDeck(attackerDeckId, attackerUserId, now);
        insertDeck(defenderDeckId, defenderUserId, now);
        insertActiveGame(gameId, attackerUserId, defenderUserId, now);
        insertParticipant(UUID.randomUUID(), gameId, attackerUserId, attackerDeckId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, defenderUserId, defenderDeckId, 2, now);

        UUID attackerActiveInstanceId = UUID.randomUUID();
        UUID defenderActiveInstanceId = UUID.randomUUID();
        insertCardInstance(attackerActiveInstanceId, gameId, attackerUserId, attackerCard.getId(), CardZone.ACTIVE, 0, false, now);
        insertCardInstance(defenderActiveInstanceId, gameId, defenderUserId, defenderCard.getId(), CardZone.ACTIVE, 0, false, now);

        UUID attackerPokemonInPlayId = UUID.randomUUID();
        UUID defenderPokemonInPlayId = UUID.randomUUID();
        insertPokemonInPlay(attackerPokemonInPlayId, gameId, attackerUserId, attackerActiveInstanceId, 0, 1, 0, now);
        insertPokemonInPlay(defenderPokemonInPlayId, gameId, defenderUserId, defenderActiveInstanceId, 0, 1, 0, now);
        insertEvolutionStack(UUID.randomUUID(), attackerPokemonInPlayId, attackerActiveInstanceId, 0, now);
        insertEvolutionStack(UUID.randomUUID(), defenderPokemonInPlayId, defenderActiveInstanceId, 0, now);

        int zonePosition = 1;
        for (Card energyCard : energyCards) {
            UUID energyInstanceId = UUID.randomUUID();
            insertCardInstance(energyInstanceId, gameId, attackerUserId, energyCard.getId(), CardZone.ATTACHED, zonePosition, false, now);
            insertAttachedCard(UUID.randomUUID(), attackerPokemonInPlayId, energyInstanceId, AttachedCardType.BASIC_ENERGY.name(), now);
            zonePosition++;
        }

        return new AttackGameFixture(
                gameId,
                attackerUserId,
                defenderUserId,
                defenderPokemonInPlayId,
                attack.getId(),
                ACTIVE_MAIN_STATE_VERSION);
    }

    protected GameActionResponseDto declareAttack(AttackGameFixture fixture) {
        return gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString())));
    }

    protected void assertDamageAndEvents(AttackGameFixture fixture, int expectedDamageCounters) {
        PokemonInPlay defenderPokemon = pokemonInPlayRepository.findById(fixture.defenderPokemonInPlayId()).orElseThrow();
        assertThat(defenderPokemon.getDamageCounters()).isEqualTo(expectedDamageCounters);
        assertThat(gameEventRepository.findByGame_IdOrderByCreatedAtAsc(fixture.gameId()))
                .extracting(GameEvent::getEventType)
                .contains(GameEventType.ATTACK_DECLARED, GameEventType.DAMAGE_APPLIED);
    }

    protected void assertAttackVisibleAndAffordableBeforeAction(AttackGameFixture fixture) {
        GameStateDto state = gameStateQueryService.buildVisibleState(
                gameRepository.findById(fixture.gameId()).orElseThrow(),
                fixture.attackerUserId());
        assertThat(state.players().get(fixture.attackerUserId()).affordableAttackIds())
                .contains(fixture.attackId());
        assertThat(state.board().view().players())
                .filteredOn(player -> fixture.attackerUserId().equals(player.playerId()))
                .singleElement()
                .satisfies(player -> assertThat(player.activePokemon().attacks())
                .anySatisfy(attack -> {
                    assertThat(attack.attackId()).isEqualTo(fixture.attackId());
                    assertThat(attack.enabled()).isTrue();
                    assertThat(attack.costs()).isNotEmpty();
                }));
    }

    protected List<Card> energies(String... types) {
        List<Card> cards = new ArrayList<>();
        for (String type : types) {
            cards.add(saveBasicEnergy(type));
        }
        return cards;
    }

    private void insertUser(UUID userId, String username, Instant now) {
        jdbcTemplate.update(
                "insert into users (id, email, username, password_hash, role, status, email_verified, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                username + "@example.com",
                username,
                "hash",
                "USER",
                "ACTIVE",
                false,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertDeck(UUID deckId, UUID ownerUserId, Instant now) {
        jdbcTemplate.update(
                "insert into decks (id, owner_user_id, name, format, is_valid, is_active, validation_errors, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                deckId,
                ownerUserId,
                "Deck " + deckId,
                "XY1_UNLIMITED",
                true,
                true,
                null,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertActiveGame(UUID gameId, UUID activePlayerId, UUID playerWhoWentFirstId, Instant now) {
        jdbcTemplate.update(
                "insert into games (id, status, current_phase, turn_number, state_version, active_player_id, player_who_went_first_id, started_at, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                gameId,
                GameStatus.ACTIVE.name(),
                TurnPhase.MAIN.name(),
                ACTIVE_MAIN_TURN_NUMBER,
                ACTIVE_MAIN_STATE_VERSION,
                activePlayerId,
                playerWhoWentFirstId,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertParticipant(UUID participantId, UUID gameId, UUID userId, UUID deckId, int playerOrder, Instant now) {
        jdbcTemplate.update(
                "insert into game_participants (id, game_id, user_id, deck_id, player_order, is_connected, last_seen_at, consecutive_timeouts, created_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                participantId,
                gameId,
                userId,
                deckId,
                playerOrder,
                true,
                Timestamp.from(now),
                0,
                Timestamp.from(now));
    }

    private void insertCardInstance(
            UUID instanceId,
            UUID gameId,
            UUID ownerUserId,
            UUID cardId,
            CardZone zone,
            int zonePosition,
            boolean faceDown,
            Instant now) {
        jdbcTemplate.update(
                "insert into game_card_instances (id, game_id, owner_user_id, card_id, zone, zone_position, is_face_down, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                instanceId,
                gameId,
                ownerUserId,
                cardId,
                zone.name(),
                zonePosition,
                faceDown,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertPokemonInPlay(
            UUID pokemonInPlayId,
            UUID gameId,
            UUID ownerUserId,
            UUID activeCardInstanceId,
            int slotPosition,
            int enteredPlayTurn,
            int damageCounters,
            Instant now) {
        jdbcTemplate.update(
                "insert into pokemon_in_play (id, game_id, owner_user_id, active_card_instance_id, slot_position, damage_counters, entered_play_turn, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                pokemonInPlayId,
                gameId,
                ownerUserId,
                activeCardInstanceId,
                slotPosition,
                damageCounters,
                enteredPlayTurn,
                Timestamp.from(now));
    }

    private void insertEvolutionStack(UUID stackId, UUID pokemonInPlayId, UUID gameCardInstanceId, int stackOrder, Instant now) {
        jdbcTemplate.update(
                "insert into pokemon_evolution_stack (id, pokemon_in_play_id, game_card_instance_id, stack_order, created_at_turn, created_at) values (?, ?, ?, ?, ?, ?)",
                stackId,
                pokemonInPlayId,
                gameCardInstanceId,
                stackOrder,
                1,
                Timestamp.from(now));
    }

    private void insertAttachedCard(UUID attachedCardId, UUID pokemonInPlayId, UUID gameCardInstanceId, String attachedCardType, Instant now) {
        jdbcTemplate.update(
                "insert into pokemon_attached_cards (id, pokemon_in_play_id, game_card_instance_id, attached_card_type, created_at) values (?, ?, ?, ?, ?)",
                attachedCardId,
                pokemonInPlayId,
                gameCardInstanceId,
                attachedCardType,
                Timestamp.from(now));
    }

    protected record AttackGameFixture(
            UUID gameId,
            UUID attackerUserId,
            UUID defenderUserId,
            UUID defenderPokemonInPlayId,
            UUID attackId,
            int expectedStateVersion) {
    }
}
