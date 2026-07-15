package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionLogService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GameHistoryControllerTest extends PostgreSqlDockerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private GameActionLogService gameActionLogService;

    @Autowired
    private GameSnapshotService gameSnapshotService;

    @Autowired
    private GameRealtimeEventService gameRealtimeEventService;

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
    void shouldReturnHistoryOrderedByVersion() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T20:00:00Z");

        insertUser(userId, "historyuser", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, 1, now);

        gameActionLogService.recordAcceptedAction(
                gameId,
                userId,
                ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType.END_TURN,
                Map.of("step", 2),
                Map.of(),
                2,
                UUID.randomUUID());

        gameActionLogService.recordAcceptedAction(
                gameId,
                userId,
                ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType.DRAW_CARD,
                Map.of("step", 1),
                Map.of("drawn", 1),
                1,
                UUID.randomUUID());

        mockMvc.perform(get("/api/games/{gameId}/history", gameId)
                        .header("Authorization", bearerToken(userId, "historyuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[0].actionType").value("DRAW_CARD"))
                .andExpect(jsonPath("$[1].version").value(2))
                .andExpect(jsonPath("$[1].actionType").value("END_TURN"));
    }

    @Test
    void shouldReturnLatestSnapshot() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T20:00:00Z");

        insertUser(userId, "snapshotuser", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, 1, now);

        gameSnapshotService.saveSnapshot(
                gameId,
                1,
                GameStateTestFactory.state(
                        gameId,
                        ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE,
                        ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase.DRAW,
                        1,
                        1,
                        null,
                        List.of(ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType.DRAW_CARD),
                        now.minusSeconds(10)),
                userId);

        gameSnapshotService.saveSnapshot(
                gameId,
                2,
                GameStateTestFactory.state(
                        gameId,
                        ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE,
                        ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase.MAIN,
                        2,
                        2,
                        null,
                        List.of(ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType.END_TURN),
                        now),
                userId);

        mockMvc.perform(get("/api/games/{gameId}/snapshot/latest", gameId)
                        .header("Authorization", bearerToken(userId, "snapshotuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateVersion").value(2))
                .andExpect(jsonPath("$.turn.currentPhase").value("MAIN"));
    }

    @Test
    void shouldReturn404WhenSnapshotDoesNotExist() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T20:00:00Z");

        insertUser(userId, "nosnapshotuser", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, 1, now);

        mockMvc.perform(get("/api/games/{gameId}/snapshot/latest", gameId)
                        .header("Authorization", bearerToken(userId, "nosnapshotuser")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldReturnVisibleChatHistoryOrderedByCreation() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID otherDeckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T20:00:00Z");

        insertUser(userId, "chatuser", now);
        insertUser(otherUserId, "chatopponent", now);
        insertDeck(deckId, userId, now);
        insertDeck(otherDeckId, otherUserId, now);
        insertGame(gameId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, otherUserId, otherDeckId, 2, now);

        gameRealtimeEventService.dispatchPublic(
                gameId,
                ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType.CHAT_MESSAGE,
                0,
                Map.of(
                        "messageId", UUID.randomUUID(),
                        "senderUserId", userId,
                        "senderUsername", "chatuser",
                        "context", "ROOM",
                        "content", "hola",
                        "sentAt", now));
        gameRealtimeEventService.dispatchPublic(
                gameId,
                ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType.CHAT_MESSAGE,
                0,
                Map.of(
                        "messageId", UUID.randomUUID(),
                        "senderUserId", otherUserId,
                        "senderUsername", "chatopponent",
                        "context", "GAME",
                        "content", "vamos",
                        "sentAt", now.plusSeconds(5)));

        mockMvc.perform(get("/api/games/{gameId}/chat", gameId)
                        .header("Authorization", bearerToken(userId, "chatuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senderUsername").value("chatuser"))
                .andExpect(jsonPath("$[0].context").value("ROOM"))
                .andExpect(jsonPath("$[1].senderUsername").value("chatopponent"))
                .andExpect(jsonPath("$[1].context").value("GAME"));
    }

    @Test
    void shouldReturnForbiddenWhenAuthenticatedUserIsNotParticipant() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID participantUserId = UUID.randomUUID();
        UUID outsiderUserId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T20:00:00Z");

        insertUser(participantUserId, "historyparticipant", now);
        insertUser(outsiderUserId, "historyoutsider", now);
        insertDeck(deckId, participantUserId, now);
        insertGame(gameId, now);
        insertParticipant(UUID.randomUUID(), gameId, participantUserId, deckId, 1, now);

        mockMvc.perform(get("/api/games/{gameId}/history", gameId)
                        .header("Authorization", bearerToken(outsiderUserId, "historyoutsider")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN_ACTION"));
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

    private void insertGame(UUID gameId, Instant now) {
        jdbcTemplate.update(
                "insert into games (id, status, current_phase, turn_number, state_version, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?)",
                gameId,
                "ACTIVE",
                "DRAW",
                1,
                0,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertParticipant(UUID participantId, UUID gameId, UUID userId, UUID deckId, int playerOrder, Instant now) {
        jdbcTemplate.update(
                "insert into game_participants (id, game_id, user_id, deck_id, player_order, is_connected, last_seen_at, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                participantId,
                gameId,
                userId,
                deckId,
                playerOrder,
                true,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private String bearerToken(UUID userId, String username) {
        User user = new User();
        user.setId(userId);
        user.setEmail(username + "@example.com");
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}
