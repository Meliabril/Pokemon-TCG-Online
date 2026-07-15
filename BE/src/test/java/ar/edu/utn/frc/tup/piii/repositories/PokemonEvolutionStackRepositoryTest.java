package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
class PokemonEvolutionStackRepositoryTest extends PostgreSqlDockerTestBase {

    @Autowired
    private PokemonEvolutionStackRepository pokemonEvolutionStackRepository;

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
    void shouldReturnEvolutionStackOrderedByStackOrder() {
        BoardFixture fixture = insertBoardFixture();

        pokemonEvolutionStackRepository.saveAndFlush(stack(fixture.pokemonInPlay(), fixture.evolvedCard(), 1));
        pokemonEvolutionStackRepository.saveAndFlush(stack(fixture.pokemonInPlay(), fixture.baseCard(), 0));

        List<PokemonEvolutionStack> stack = pokemonEvolutionStackRepository
                .findByPokemonInPlay_IdOrderByStackOrderAsc(fixture.pokemonInPlay().getId());

        assertThat(stack)
                .extracting(PokemonEvolutionStack::getStackOrder)
                .containsExactly(0, 1);
    }

    @Test
    void shouldRejectDuplicateStackOrderWithinSamePokemon() {
        BoardFixture fixture = insertBoardFixture();

        pokemonEvolutionStackRepository.saveAndFlush(stack(fixture.pokemonInPlay(), fixture.baseCard(), 0));

        assertThatThrownBy(() -> pokemonEvolutionStackRepository.saveAndFlush(
                stack(fixture.pokemonInPlay(), fixture.evolvedCard(), 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateCardInstanceInEvolutionStack() {
        BoardFixture fixture = insertBoardFixture();

        pokemonEvolutionStackRepository.saveAndFlush(stack(fixture.pokemonInPlay(), fixture.baseCard(), 0));

        assertThatThrownBy(saveDuplicateCardInstance(fixture))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNegativeCreatedAtTurn() {
        BoardFixture fixture = insertBoardFixture();
        PokemonEvolutionStack stack = stack(fixture.pokemonInPlay(), fixture.baseCard(), 0);
        stack.setCreatedAtTurn(-1);

        assertThatThrownBy(saveStack(stack))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PokemonEvolutionStack stack(PokemonInPlay pokemonInPlay, GameCardInstance cardInstance, int stackOrder) {
        PokemonEvolutionStack stack = new PokemonEvolutionStack();
        stack.setPokemonInPlay(pokemonInPlay);
        stack.setGameCardInstance(cardInstance);
        stack.setStackOrder(stackOrder);
        stack.setCreatedAtTurn(1);
        return stack;
    }

    private ThrowingCallable saveDuplicateCardInstance(BoardFixture fixture) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                pokemonEvolutionStackRepository.saveAndFlush(stack(fixture.pokemonInPlay(), fixture.baseCard(), 1));
            }
        };
    }

    private ThrowingCallable saveStack(PokemonEvolutionStack stack) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                pokemonEvolutionStackRepository.saveAndFlush(stack);
            }
        };
    }

    private BoardFixture insertBoardFixture() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        insertUser(userId);
        UUID baseCardId = UUID.randomUUID();
        UUID evolvedCardId = UUID.randomUUID();
        insertCard(baseCardId);
        insertCard(evolvedCardId);

        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(1);
        game.setStateVersion(0);
        game = gameRepository.saveAndFlush(game);

        GameCardInstance baseCard = cardInstance(game, userId, baseCardId, 1);
        GameCardInstance evolvedCard = cardInstance(game, userId, evolvedCardId, 2);

        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setGame(game);
        pokemonInPlay.setOwnerUserId(userId);
        pokemonInPlay.setActiveCardInstance(baseCard);
        pokemonInPlay.setSlotPosition(0);
        pokemonInPlay.setEnteredPlayTurn(1);
        pokemonInPlay = pokemonInPlayRepository.saveAndFlush(pokemonInPlay);

        return new BoardFixture(pokemonInPlay, baseCard, evolvedCard);
    }

    private GameCardInstance cardInstance(Game game, UUID userId, UUID cardId, int zonePosition) {
        GameCardInstance instance = new GameCardInstance();
        instance.setGame(game);
        instance.setOwnerUserId(userId);
        instance.setCardId(cardId);
        instance.setZone(CardZone.HAND);
        instance.setZonePosition(zonePosition);
        return gameCardInstanceRepository.saveAndFlush(instance);
    }

    private void insertUser(UUID userId) {
        Instant now = Instant.parse("2026-05-24T18:15:00Z");
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
        Instant now = Instant.parse("2026-05-24T18:15:00Z");
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

    private record BoardFixture(PokemonInPlay pokemonInPlay, GameCardInstance baseCard, GameCardInstance evolvedCard) {
    }
}
