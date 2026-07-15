package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SpecialConditionRepositoryTest extends PostgreSqlDockerTestBase {

    @Autowired
    private SpecialConditionRepository specialConditionRepository;

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
    void shouldPersistAndQueryConditionsForPokemon() {
        PokemonInPlay pokemonInPlay = insertBoardFixture();

        specialConditionRepository.saveAndFlush(condition(pokemonInPlay, SpecialConditionType.BURNED));
        specialConditionRepository.saveAndFlush(condition(pokemonInPlay, SpecialConditionType.POISONED));

        List<SpecialCondition> conditions = specialConditionRepository
                .findByPokemonInPlay_IdOrderByCreatedAtAsc(pokemonInPlay.getId());

        assertThat(conditions)
                .extracting(SpecialCondition::getConditionType)
                .containsExactly(SpecialConditionType.BURNED, SpecialConditionType.POISONED);
    }

    @Test
    void shouldDeleteConditionsByPokemonInPlayId() {
        PokemonInPlay pokemonInPlay = insertBoardFixture();
        specialConditionRepository.saveAndFlush(condition(pokemonInPlay, SpecialConditionType.ASLEEP));
        specialConditionRepository.saveAndFlush(condition(pokemonInPlay, SpecialConditionType.CONFUSED));

        specialConditionRepository.deleteByPokemonInPlay_Id(pokemonInPlay.getId());

        assertThat(specialConditionRepository.findByPokemonInPlay_IdOrderByCreatedAtAsc(pokemonInPlay.getId()))
                .isEmpty();
    }

    private SpecialCondition condition(PokemonInPlay pokemonInPlay, SpecialConditionType type) {
        SpecialCondition condition = new SpecialCondition();
        condition.setPokemonInPlay(pokemonInPlay);
        condition.setConditionType(type);
        condition.setAppliedTurn(1);
        return condition;
    }

    private PokemonInPlay insertBoardFixture() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        insertUser(userId);
        insertCard(cardId);

        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(1);
        game.setStateVersion(0);
        game = gameRepository.saveAndFlush(game);

        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setGame(game);
        cardInstance.setOwnerUserId(userId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(CardZone.ACTIVE);
        cardInstance.setZonePosition(0);
        cardInstance = gameCardInstanceRepository.saveAndFlush(cardInstance);

        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setGame(game);
        pokemonInPlay.setOwnerUserId(userId);
        pokemonInPlay.setActiveCardInstance(cardInstance);
        pokemonInPlay.setSlotPosition(0);
        pokemonInPlay.setEnteredPlayTurn(1);
        return pokemonInPlayRepository.saveAndFlush(pokemonInPlay);
    }

    private void insertUser(UUID userId) {
        Instant now = Instant.parse("2026-05-24T18:25:00Z");
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
        Instant now = Instant.parse("2026-05-24T18:25:00Z");
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
