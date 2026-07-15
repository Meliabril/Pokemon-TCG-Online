package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
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
class PokemonAttachedCardRepositoryTest extends PostgreSqlDockerTestBase {

    @Autowired
    private PokemonAttachedCardRepository pokemonAttachedCardRepository;

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
        jdbcTemplate.update("delete from pokemon_attached_cards");
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
    void shouldPersistAttachedCardTypeAndReturnCardsForPokemon() {
        BoardFixture fixture = insertBoardFixture();

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setPokemonInPlay(fixture.pokemonInPlay());
        attachedCard.setGameCardInstance(fixture.attachedCardInstance());
        attachedCard.setAttachedCardType(AttachedCardType.BASIC_ENERGY);

        PokemonAttachedCard saved = pokemonAttachedCardRepository.saveAndFlush(attachedCard);
        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardRepository
                .findByPokemonInPlay_IdOrderByCreatedAtAsc(fixture.pokemonInPlay().getId());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(attachedCards)
                .hasSize(1)
                .extracting(PokemonAttachedCard::getAttachedCardType)
                .containsExactly(AttachedCardType.BASIC_ENERGY);
    }

    private BoardFixture insertBoardFixture() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        insertUser(userId);
        UUID pokemonCardId = UUID.randomUUID();
        UUID energyCardId = UUID.randomUUID();
        insertCard(pokemonCardId, "POKEMON", "BASIC_POKEMON");
        insertCard(energyCardId, "ENERGY", "BASIC_ENERGY");

        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(1);
        game.setStateVersion(0);
        game = gameRepository.saveAndFlush(game);

        GameCardInstance pokemonCard = cardInstance(game, userId, pokemonCardId, CardZone.ACTIVE, 0);
        GameCardInstance energyCard = cardInstance(game, userId, energyCardId, CardZone.HAND, 1);

        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setGame(game);
        pokemonInPlay.setOwnerUserId(userId);
        pokemonInPlay.setActiveCardInstance(pokemonCard);
        pokemonInPlay.setSlotPosition(0);
        pokemonInPlay.setEnteredPlayTurn(1);
        pokemonInPlay = pokemonInPlayRepository.saveAndFlush(pokemonInPlay);

        return new BoardFixture(pokemonInPlay, energyCard);
    }

    private GameCardInstance cardInstance(Game game, UUID userId, UUID cardId, CardZone zone, int zonePosition) {
        GameCardInstance instance = new GameCardInstance();
        instance.setGame(game);
        instance.setOwnerUserId(userId);
        instance.setCardId(cardId);
        instance.setZone(zone);
        instance.setZonePosition(zonePosition);
        return gameCardInstanceRepository.saveAndFlush(instance);
    }

    private void insertUser(UUID userId) {
        Instant now = Instant.parse("2026-05-24T18:20:00Z");
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

    private void insertCard(UUID cardId, String supertype, String category) {
        Instant now = Instant.parse("2026-05-24T18:20:00Z");
        jdbcTemplate.update(
                "insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, raw_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                cardId,
                cardId.toString(),
                "xy1",
                "XY",
                "1",
                "Card " + cardId,
                supertype,
                category,
                "{}",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private record BoardFixture(PokemonInPlay pokemonInPlay, GameCardInstance attachedCardInstance) {
    }
}
