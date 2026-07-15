package ar.edu.utn.frc.tup.piii.services.matchmaking;

import ar.edu.utn.frc.tup.piii.configs.LocalTestingDeckSeeder;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidDeckException;
import ar.edu.utn.frc.tup.piii.repositories.GameParticipantRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameMatchBootstrapService;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameSnapshotServiceImpl;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest
class MatchmakingBootstrapIntegrationTest extends PostgreSqlDockerTestBase {

    private static final Instant FIXED_TIME = Instant.parse("2026-05-17T19:00:00Z");

    @Autowired
    private GameMatchBootstrapService gameMatchBootstrapService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameParticipantRepository gameParticipantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private GameSnapshotServiceImpl gameSnapshotService;

    @MockBean
    private LocalTestingDeckSeeder localTestingDeckSeeder;

    @AfterEach
    void cleanup() {
        reset(gameSnapshotService);
        jdbcTemplate.update("delete from matchmaking_queue_entries");
        jdbcTemplate.update("delete from game_state_snapshots");
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from game_state_snapshots");
        jdbcTemplate.update("delete from game_action_logs");
        jdbcTemplate.update("delete from game_events");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from deck_cards");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldPersistGameBootstrapForDecksAtValidationBoundaries() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID userDeckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        UUID basicCardId = insertCard("Bulbasaur", "1", CardCategory.BASIC_POKEMON, CardSupertype.POKEMON);
        UUID trainerCardId = insertCard("Professor Sycamore", "2", CardCategory.SUPPORTER_TRAINER, CardSupertype.TRAINER);
        UUID energyCardId = insertCard("Grass Energy", "3", CardCategory.BASIC_ENERGY, CardSupertype.ENERGY);
        insertUser(userId);
        insertUser(opponentUserId);
        insertDeck(userDeckId, userId, true);
        insertDeck(opponentDeckId, opponentUserId, true);
        insertDeckComposition(userDeckId, basicCardId, 4, trainerCardId, 4, energyCardId, 52);
        insertDeckComposition(opponentDeckId, basicCardId, 4, trainerCardId, 4, energyCardId, 52);

        UUID gameId = gameMatchBootstrapService.createMatch(userId, userDeckId, opponentUserId, opponentDeckId);

        Integer deckColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'matchmaking_queue_entries' and column_name = 'deck_id'",
                Integer.class);
        Integer snapshotCount = jdbcTemplate.queryForObject(
                "select count(*) from game_state_snapshots where game_id = ?",
                Integer.class,
                gameId);
        Game savedGame = gameRepository.findById(gameId).orElseThrow();
        List<GameParticipant> participants = gameParticipantRepository.findByGame_IdOrderByPlayerOrderAsc(gameId);

        assertThat(deckColumnCount).isEqualTo(1);
        assertThat(snapshotCount).isEqualTo(1);
        assertThat(savedGame.getId()).isEqualTo(gameId);
        assertThat(participants)
                .hasSize(2)
                .extracting(GameParticipant::getUserId, GameParticipant::getDeckId, GameParticipant::getPlayerOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(userId, userDeckId, 1),
                        org.assertj.core.groups.Tuple.tuple(opponentUserId, opponentDeckId, 2));
    }

    @Test
    void shouldRejectMatchCreationBeforePersistenceWhenDeckExceedsCopyLimitAtValidationTime() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID userDeckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        UUID basicCardId = insertCard("Charmander", "10", CardCategory.BASIC_POKEMON, CardSupertype.POKEMON);
        UUID trainerCardId = insertCard("Tierno", "11", CardCategory.ITEM_TRAINER, CardSupertype.TRAINER);
        UUID energyCardId = insertCard("Fire Energy", "12", CardCategory.BASIC_ENERGY, CardSupertype.ENERGY);
        insertUser(userId);
        insertUser(opponentUserId);
        insertDeck(userDeckId, userId, true);
        insertDeck(opponentDeckId, opponentUserId, true);
        insertDeckComposition(userDeckId, basicCardId, 4, trainerCardId, 5, energyCardId, 51);
        insertDeckComposition(opponentDeckId, basicCardId, 4, trainerCardId, 4, energyCardId, 52);

        assertThatThrownBy(() -> gameMatchBootstrapService.createMatch(userId, userDeckId, opponentUserId, opponentDeckId))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("not valid");

        assertThat(gameRepository.count()).isZero();
        assertThat(gameParticipantRepository.count()).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from game_state_snapshots", Long.class)).isZero();
    }

    @Test
    void shouldRollbackPersistedGameBootstrapWhenSnapshotPersistenceFails() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID userDeckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        UUID basicCardId = insertCard("Squirtle", "20", CardCategory.BASIC_POKEMON, CardSupertype.POKEMON);
        UUID trainerCardId = insertCard("Potion", "21", CardCategory.ITEM_TRAINER, CardSupertype.TRAINER);
        UUID energyCardId = insertCard("Water Energy", "22", CardCategory.BASIC_ENERGY, CardSupertype.ENERGY);
        insertUser(userId);
        insertUser(opponentUserId);
        insertDeck(userDeckId, userId, true);
        insertDeck(opponentDeckId, opponentUserId, true);
        insertDeckComposition(userDeckId, basicCardId, 4, trainerCardId, 4, energyCardId, 52);
        insertDeckComposition(opponentDeckId, basicCardId, 4, trainerCardId, 4, energyCardId, 52);

        doAnswer(invocation -> {
            throw new IllegalStateException("Simulated snapshot persistence failure");
        }).when(gameSnapshotService).saveSnapshot(any(UUID.class), anyInt(), any(GameStateDto.class), isNull());

        assertThatThrownBy(() -> gameMatchBootstrapService.createMatch(userId, userDeckId, opponentUserId, opponentDeckId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("snapshot persistence failure");

        assertThat(gameRepository.count()).isZero();
        assertThat(gameParticipantRepository.count()).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from game_state_snapshots", Long.class)).isZero();
    }

    private void insertUser(UUID userId) {
        jdbcTemplate.update(
                "insert into users (id, email, username, password_hash, role, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                userId + "@example.com",
                "user" + userId.toString().substring(0, 8),
                "hash",
                "USER",
                "ACTIVE",
                Timestamp.from(FIXED_TIME),
                Timestamp.from(FIXED_TIME));
    }

    private void insertDeck(UUID deckId, UUID ownerUserId, boolean valid) {
        jdbcTemplate.update(
                "insert into decks (id, owner_user_id, name, format, is_valid, is_active, validation_errors, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                deckId,
                ownerUserId,
                "Deck " + deckId,
                "XY1_UNLIMITED",
                valid,
                true,
                "[]",
                Timestamp.from(FIXED_TIME),
                Timestamp.from(FIXED_TIME));
    }

    private UUID insertCard(String name, String number, CardCategory category, CardSupertype supertype) {
        UUID cardId = UUID.randomUUID();
        String subtype = category == CardCategory.BASIC_POKEMON ? "Basic" : null;
        jdbcTemplate.update(
                "insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, subtype, raw_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                cardId,
                "test-" + cardId,
                Card.XY1_SET_CODE,
                "XY",
                number,
                name,
                supertype.name(),
                category.name(),
                subtype,
                "{}",
                Timestamp.from(FIXED_TIME),
                Timestamp.from(FIXED_TIME));
        return cardId;
    }

    private void insertDeckComposition(
            UUID deckId,
            UUID basicCardId,
            int basicQuantity,
            UUID trainerCardId,
            int trainerQuantity,
            UUID energyCardId,
            int energyQuantity) {
        insertDeckCard(deckId, basicCardId, basicQuantity);
        insertDeckCard(deckId, trainerCardId, trainerQuantity);
        insertDeckCard(deckId, energyCardId, energyQuantity);
    }

    private void insertDeckCard(UUID deckId, UUID cardId, int quantity) {
        jdbcTemplate.update(
                "insert into deck_cards (id, deck_id, card_id, quantity) values (?, ?, ?, ?)",
                UUID.randomUUID(),
                deckId,
                cardId,
                quantity);
    }
}
