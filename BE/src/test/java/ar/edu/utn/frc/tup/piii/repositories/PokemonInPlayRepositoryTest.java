package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PokemonInPlayRepositoryTest extends PostgreSqlDockerTestBase {

    @Autowired
    private PokemonInPlayRepository pokemonInPlayRepository;

    @Autowired
    private GameCardInstanceRepository gameCardInstanceRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("delete from special_conditions");
        jdbcTemplate.update("delete from pokemon_attached_cards");
        jdbcTemplate.update("delete from pokemon_evolution_stack");
        jdbcTemplate.update("delete from pokemon_in_play");
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from deck_cards");
        jdbcTemplate.update("delete from attack_costs");
        jdbcTemplate.update("delete from attacks");
        jdbcTemplate.update("delete from card_weaknesses");
        jdbcTemplate.update("delete from card_resistances");
        jdbcTemplate.update("delete from cards");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldPersistActiveSlotUsingPositionZero() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        GameCardInstance activeCard = insertCardInstance(gameId, userId, 1);

        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setGame(activeCard.getGame());
        pokemonInPlay.setOwnerUserId(userId);
        pokemonInPlay.setActiveCardInstance(activeCard);
        pokemonInPlay.setSlotPosition(0);
        pokemonInPlay.setEnteredPlayTurn(1);

        PokemonInPlay saved = pokemonInPlayRepository.saveAndFlush(pokemonInPlay);

        assertThat(saved.getDamageCounters()).isZero();
        assertThat(saved.getSlotPosition()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldReturnBoardSlotsOrderedByPosition() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        GameCardInstance secondCard = insertCardInstance(gameId, userId, 2);
        GameCardInstance firstCard = insertCardInstance(gameId, userId, 1);

        pokemonInPlayRepository.saveAndFlush(slot(firstCard.getGame(), userId, firstCard, 1, 2));
        pokemonInPlayRepository.saveAndFlush(slot(secondCard.getGame(), userId, secondCard, 2, 2));

        List<PokemonInPlay> slots = pokemonInPlayRepository
                .findByGame_IdAndOwnerUserIdOrderBySlotPositionAsc(gameId, userId);

        assertThat(slots)
                .extracting(PokemonInPlay::getSlotPosition)
                .containsExactly(1, 2);
    }

    @Test
    void shouldRejectDuplicateOwnerSlotPerGame() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        GameCardInstance firstCard = insertCardInstance(gameId, userId, 1);
        GameCardInstance secondCard = insertCardInstance(gameId, userId, 2);

        pokemonInPlayRepository.saveAndFlush(slot(secondCard.getGame(), userId, secondCard, 0, 1));

        assertThatThrownBy(() -> pokemonInPlayRepository.saveAndFlush(slot(secondCard.getGame(), userId, secondCard, 0, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldExposeSwappedSlotPositionsAfterSwapActiveWithBench() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        GameCardInstance activeCard = insertCardInstance(gameId, userId, 1);
        GameCardInstance benchCard = insertCardInstance(gameId, userId, 2);

        PokemonInPlay activePokemon = pokemonInPlayRepository.saveAndFlush(
                slot(activeCard.getGame(), userId, activeCard, 0, 1));
        PokemonInPlay benchPokemon = pokemonInPlayRepository.saveAndFlush(
                slot(benchCard.getGame(), userId, benchCard, 1, 1));

        List<PokemonInPlay> beforeSwap = pokemonInPlayRepository
                .findByGame_IdOrderByOwnerUserIdAscSlotPositionAsc(gameId);
        assertThat(beforeSwap)
                .extracting(PokemonInPlay::getId)
                .containsExactly(activePokemon.getId(), benchPokemon.getId());
        assertThat(beforeSwap)
                .extracting(PokemonInPlay::getSlotPosition)
                .containsExactly(0, 1);

        int updated = pokemonInPlayRepository.swapActiveWithBench(
                activePokemon.getId(), benchPokemon.getId(), 1);
        assertThat(updated).isEqualTo(2);

        List<PokemonInPlay> afterSwap = pokemonInPlayRepository
                .findByGame_IdOrderByOwnerUserIdAscSlotPositionAsc(gameId);
        assertThat(afterSwap)
                .extracting(PokemonInPlay::getId)
                .containsExactly(benchPokemon.getId(), activePokemon.getId());
        assertThat(afterSwap)
                .extracting(PokemonInPlay::getSlotPosition)
                .containsExactly(0, 1);
    }

    private PokemonInPlay slot(Game game, UUID ownerUserId, GameCardInstance activeCardInstance, int slotPosition, int enteredPlayTurn) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setGame(game);
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setActiveCardInstance(activeCardInstance);
        pokemonInPlay.setSlotPosition(slotPosition);
        pokemonInPlay.setEnteredPlayTurn(enteredPlayTurn);
        return pokemonInPlay;
    }

    private GameCardInstance insertCardInstance(UUID gameId, UUID userId, int zonePosition) {
        insertUser(userId);
        UUID cardId = UUID.randomUUID();
        insertCard(cardId);
        Game game = findOrInsertGame(gameId);

        GameCardInstance instance = new GameCardInstance();
        instance.setGame(game);
        instance.setOwnerUserId(userId);
        instance.setCardId(cardId);
        instance.setZone(ar.edu.utn.frc.tup.piii.dtos.enums.CardZone.BENCH);
        instance.setZonePosition(zonePosition);
        return gameCardInstanceRepository.saveAndFlush(instance);
    }

    private Game findOrInsertGame(UUID gameId) {
        return gameRepository.findById(gameId).orElseGet(() -> {
            Game game = new Game();
            game.setId(gameId);
            game.setStatus(GameStatus.ACTIVE);
            game.setTurnNumber(1);
            game.setStateVersion(0);
            return gameRepository.saveAndFlush(game);
        });
    }

    private void insertUser(UUID userId) {
        if (jdbcTemplate.queryForObject("select count(*) from users where id = ?", Integer.class, userId) > 0) {
            return;
        }
        Instant now = Instant.parse("2026-05-24T18:00:00Z");
        jdbcTemplate.update(
                "insert into users (id, email, username, password_hash, role, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                userId + "@example.com",
                "user" + userId.toString().substring(0, 8),
                "hash",
                "USER",
                "ACTIVE",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertCard(UUID cardId) {
        Instant now = Instant.parse("2026-05-24T18:00:00Z");
        jdbcTemplate.update(
                "insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, raw_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                cardId,
                cardId.toString(),
                "xy1",
                "XY",
                "1",
                "Card " + cardId,
                "POKEMON",
                "BASIC_POKEMON",
                "{}",
                Timestamp.from(now),
                Timestamp.from(now));
    }
}
