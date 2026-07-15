package ar.edu.utn.frc.tup.piii;




import ar.edu.utn.frc.tup.piii.services.game.attack.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.board.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.attack.*;
import ar.edu.utn.frc.tup.piii.services.game.board.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.*;
import ar.edu.utn.frc.tup.piii.services.game.query.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.*;
import ar.edu.utn.frc.tup.piii.services.game.state.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.*;
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.controllers.websocket.GameWebSocketController;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateSyncDto;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.security.StompPrincipal;
import ar.edu.utn.frc.tup.piii.services.game.query.GameEventService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.websocket.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
class GameStateSyncIntegrationTest extends PostgreSqlDockerTestBase {

    @Autowired
    private GameWebSocketController gameWebSocketController;

    @Autowired
    private GameSnapshotService gameSnapshotService;

    @Autowired
    private GameEventService gameEventService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private GameEventPublisher gameEventPublisher;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("delete from game_events");
        jdbcTemplate.update("delete from game_state_snapshots");
        jdbcTemplate.update("delete from game_action_logs");
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from deck_cards");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldPersistAndPublishPrivateStateSyncUsingLatestSnapshot() {
        UUID gameId = createGame(GameStatus.ACTIVE, TurnPhase.MAIN, 5);
        UUID viewerUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-24T12:00:00Z");
        insertUser(viewerUserId, "viewer", now);
        insertUser(opponentUserId, "opponent", now);
        UUID viewerDeckId = insertDeck(viewerUserId, now);
        UUID opponentDeckId = insertDeck(opponentUserId, now);
        insertParticipant(gameId, viewerUserId, viewerDeckId, 1, now);
        insertParticipant(gameId, opponentUserId, opponentDeckId, 2, now);

        GameStateDto snapshot = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                5,
                5,
                viewerUserId,
                List.of(viewerUserId, opponentUserId),
                List.of(GameActionType.END_TURN),
                now);
        gameSnapshotService.saveSnapshot(gameId, 5, snapshot, viewerUserId);

        gameWebSocketController.syncState(gameId, new StompPrincipal(viewerUserId));

        List<GameEventDto> viewerEvents = gameEventService.getVisibleEvents(gameId, viewerUserId);
        List<GameEventDto> opponentEvents = gameEventService.getVisibleEvents(gameId, opponentUserId);

        assertThat(viewerEvents).hasSize(1);
        assertThat(viewerEvents.getFirst().eventType()).isEqualTo(GameEventType.STATE_SYNC);
        assertThat(viewerEvents.getFirst().privateEvent()).isTrue();
        assertThat(viewerEvents.getFirst().payload()).containsKey("state");
        java.util.Map<String, Object> statePayload = (java.util.Map<String, Object>) viewerEvents.getFirst().payload().get("state");
        java.util.Map<String, Object> turnPayload = (java.util.Map<String, Object>) statePayload.get("turn");
        assertThat(statePayload)
                .containsEntry("gameId", gameId.toString())
                .containsEntry("stateVersion", 5);
        assertThat(turnPayload).containsEntry("playerWhoWentFirstId", null);
        assertThat(opponentEvents).isEmpty();
        ArgumentCaptor<GameStateSyncDto> publishedEventCaptor = ArgumentCaptor.forClass(GameStateSyncDto.class);
        verify(gameEventPublisher).publishPrivateStateSync(publishedEventCaptor.capture(), eq(viewerUserId));
        assertThat(publishedEventCaptor.getValue().gameId()).isEqualTo(gameId);
        assertThat(publishedEventCaptor.getValue().eventType()).isEqualTo(GameEventType.STATE_SYNC);
        assertThat(publishedEventCaptor.getValue().state().stateVersion()).isEqualTo(5);
        assertThat(publishedEventCaptor.getValue().state().turn().playerWhoWentFirstId()).isNull();
    }

    @Test
    void shouldFallbackToCurrentVisibleStateWhenSnapshotDoesNotExist() {
        UUID gameId = createGame(GameStatus.ACTIVE, TurnPhase.DRAW, 3);
        UUID viewerUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-24T12:05:00Z");
        insertUser(viewerUserId, "viewer2", now);
        insertUser(opponentUserId, "opponent2", now);
        UUID viewerDeckId = insertDeck(viewerUserId, now);
        UUID opponentDeckId = insertDeck(opponentUserId, now);
        insertParticipant(gameId, viewerUserId, viewerDeckId, 1, now);
        insertParticipant(gameId, opponentUserId, opponentDeckId, 2, now);

        gameWebSocketController.syncState(gameId, new StompPrincipal(viewerUserId));

        List<GameEventDto> viewerEvents = gameEventService.getVisibleEvents(gameId, viewerUserId);

        assertThat(viewerEvents).hasSize(1);
        assertThat(viewerEvents.getFirst().eventType()).isEqualTo(GameEventType.STATE_SYNC);
        assertThat(viewerEvents.getFirst().payload()).containsKey("state");
        java.util.Map<String, Object> statePayload = (java.util.Map<String, Object>) viewerEvents.getFirst().payload().get("state");
        java.util.Map<String, Object> turnPayload = (java.util.Map<String, Object>) statePayload.get("turn");
        assertThat(statePayload)
                .containsEntry("stateVersion", 3)
                .containsEntry("status", GameStatus.ACTIVE.name());
        assertThat(turnPayload)
                .containsEntry("currentPhase", TurnPhase.DRAW.name())
                .containsKey("playerWhoWentFirstId");
    }

    private UUID createGame(GameStatus status, TurnPhase phase, int stateVersion) {
        ar.edu.utn.frc.tup.piii.entities.Game game = new ar.edu.utn.frc.tup.piii.entities.Game();
        game.setStatus(status);
        game.setCurrentPhase(phase);
        game.setTurnNumber(stateVersion);
        game.setStateVersion(stateVersion);
        return gameRepository.saveAndFlush(game).getId();
    }

    private void insertUser(UUID userId, String username, Instant now) {
        jdbcTemplate.update(
                "insert into users (id, email, username, password_hash, role, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                username + "@example.com",
                username,
                "hash",
                "USER",
                "ACTIVE",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private UUID insertDeck(UUID ownerUserId, Instant now) {
        UUID deckId = UUID.randomUUID();
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
        return deckId;
    }

    private void insertParticipant(UUID gameId, UUID userId, UUID deckId, int playerOrder, Instant now) {
        jdbcTemplate.update(
                "insert into game_participants (id, game_id, user_id, deck_id, player_order, is_connected, last_seen_at, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                gameId,
                userId,
                deckId,
                playerOrder,
                true,
                Timestamp.from(now),
                Timestamp.from(now));
    }
}
