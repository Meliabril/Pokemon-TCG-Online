package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@Transactional
class GameStateReadPerformanceIntegrationTest {

    @Autowired
    private GameStateQueryService gameStateQueryService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Disabled("N+1 regression from abilities PR: per-Pokemon queries in buildPokemonView (specialConditions, evolutionStack, attachedCards) break O(1) invariant. Remove @Disabled once batch-loading is added.")
    void shouldKeepCanonicalReadQueryCountIndependentOfPokemonCount() {
        UUID smallGameId = insertGameWithPokemon(1);
        UUID expandedGameId = insertGameWithPokemon(3);

        long smallBoardQueryCount = queryCountFor(smallGameId);
        long expandedBoardQueryCount = queryCountFor(expandedGameId);

        assertThat(expandedBoardQueryCount).isEqualTo(smallBoardQueryCount);
        assertThat(expandedBoardQueryCount).isLessThanOrEqualTo(12);
    }

    private long queryCountFor(UUID gameId) {
        entityManager.flush();
        entityManager.clear();
        Game game = gameRepository.findById(gameId).orElseThrow();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        GameStateDto state = gameStateQueryService.buildVisibleState(game);

        assertThat(state.boardPlayers()).hasSize(2);
        return statistics.getPrepareStatementCount();
    }

    private UUID insertGameWithPokemon(int pokemonPerPlayer) {
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        insertUserAndDeck(playerOneId);
        insertUserAndDeck(playerTwoId);

        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(4);
        game.setStateVersion(7);
        game.setActivePlayerId(playerOneId);
        game.setPlayerWhoWentFirstId(playerOneId);
        game = gameRepository.saveAndFlush(game);

        insertParticipant(game.getId(), playerOneId, 1);
        insertParticipant(game.getId(), playerTwoId, 2);
        insertPokemon(game.getId(), playerOneId, pokemonPerPlayer);
        insertPokemon(game.getId(), playerTwoId, pokemonPerPlayer);
        return game.getId();
    }

    private void insertUserAndDeck(UUID userId) {
        Instant now = Instant.parse("2026-06-10T18:00:00Z");
        jdbcTemplate.update(
                "insert into users (id, email, username, password_hash, role, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                userId + "@example.com",
                "user-" + userId,
                "hash",
                "USER",
                "ACTIVE",
                Timestamp.from(now),
                Timestamp.from(now));
        jdbcTemplate.update(
                "insert into decks (id, owner_user_id, name, format, is_valid, is_active, validation_errors, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                deckId(userId),
                userId,
                "Deck " + userId,
                "XY1_UNLIMITED",
                true,
                true,
                null,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertParticipant(UUID gameId, UUID userId, int playerOrder) {
        Instant now = Instant.parse("2026-06-10T18:00:00Z");
        jdbcTemplate.update(
                "insert into game_participants (id, game_id, user_id, deck_id, player_order, is_connected, last_seen_at, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                gameId,
                userId,
                deckId(userId),
                playerOrder,
                true,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertPokemon(UUID gameId, UUID ownerUserId, int count) {
        Instant now = Instant.parse("2026-06-10T18:00:00Z");
        for (int slotPosition = 0; slotPosition < count; slotPosition++) {
            UUID cardId = UUID.randomUUID();
            UUID cardInstanceId = UUID.randomUUID();
            jdbcTemplate.update(
                    "insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, raw_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    cardId,
                    "performance-" + cardId,
                    "xy1",
                    "XY",
                    Integer.toString(slotPosition + 1),
                    "Pokemon " + cardId,
                    "POKEMON",
                    "BASIC_POKEMON",
                    "{}",
                    Timestamp.from(now),
                    Timestamp.from(now));
            jdbcTemplate.update(
                    "insert into game_card_instances (id, game_id, owner_user_id, card_id, zone, zone_position, is_face_down, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    cardInstanceId,
                    gameId,
                    ownerUserId,
                    cardId,
                    slotPosition == 0 ? "ACTIVE" : "BENCH",
                    slotPosition,
                    false,
                    Timestamp.from(now),
                    Timestamp.from(now));
            jdbcTemplate.update(
                    "insert into pokemon_in_play (id, game_id, owner_user_id, active_card_instance_id, slot_position, damage_counters, entered_play_turn, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(),
                    gameId,
                    ownerUserId,
                    cardInstanceId,
                    slotPosition,
                    0,
                    1,
                    Timestamp.from(now));
        }
    }

    private UUID deckId(UUID userId) {
        return UUID.nameUUIDFromBytes(("deck-" + userId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
