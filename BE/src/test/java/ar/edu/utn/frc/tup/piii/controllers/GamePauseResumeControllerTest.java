package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
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
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GamePauseResumeControllerTest extends PostgreSqlDockerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("delete from game_events");
        jdbcTemplate.update("delete from game_state_snapshots");
        jdbcTemplate.update("delete from game_action_logs");
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldPauseAndResumeGame() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T21:30:00Z");

        insertUser(userId, "pauseresume", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, "ACTIVE", "MAIN", 7, 3, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, now);

        mockMvc.perform(post("/api/games/{gameId}/pause", gameId)
                        .header("Authorization", bearerToken(userId, "pauseresume"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"network issue\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.stateVersion").value(4));

        mockMvc.perform(post("/api/games/{gameId}/resume", gameId)
                        .header("Authorization", bearerToken(userId, "pauseresume")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.turn.currentPhase").value("MAIN"))
                .andExpect(jsonPath("$.turn.turnNumber").value(7))
                .andExpect(jsonPath("$.stateVersion").value(5));
    }

    @Test
    void shouldRejectPauseWhenGameAlreadyPaused() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T21:30:00Z");

        insertUser(userId, "alreadypaused", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, "PAUSED", "MAIN", 3, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, now);

        mockMvc.perform(post("/api/games/{gameId}/pause", gameId)
                        .header("Authorization", bearerToken(userId, "alreadypaused"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"retry\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_GAME_ACTION"));
    }

    @Test
    void shouldValidatePauseReasonLength() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T21:30:00Z");
        String longReason = "x".repeat(121);

        insertUser(userId, "longreason", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, "ACTIVE", "MAIN", 1, 0, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, now);

        mockMvc.perform(post("/api/games/{gameId}/pause", gameId)
                        .header("Authorization", bearerToken(userId, "longreason"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"" + longReason + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectPauseWhenGameIsFinished() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T21:30:00Z");

        insertUser(userId, "finishedpause", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, "FINISHED", "MAIN", 9, 3, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, now);

        mockMvc.perform(post("/api/games/{gameId}/pause", gameId)
                        .header("Authorization", bearerToken(userId, "finishedpause"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"late request\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_GAME_ACTION"));
    }

    @Test
    void shouldRejectResumeWhenGameIsNotPaused() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T21:30:00Z");

        insertUser(userId, "notpaused", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, "ACTIVE", "DRAW", 1, 0, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, now);

        mockMvc.perform(post("/api/games/{gameId}/resume", gameId)
                        .header("Authorization", bearerToken(userId, "notpaused")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_GAME_ACTION"));
    }

    @Test
    void shouldReturn404WhenPausedGameHasNoSnapshotToResume() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T21:30:00Z");

        insertUser(userId, "nosnapshotresume", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, "PAUSED", "DRAW", 1, 0, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, now);

        mockMvc.perform(post("/api/games/{gameId}/resume", gameId)
                        .header("Authorization", bearerToken(userId, "nosnapshotresume")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldReturnForbiddenWhenAuthenticatedUserIsNotParticipant() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID participantUserId = UUID.randomUUID();
        UUID outsiderUserId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T21:30:00Z");

        insertUser(participantUserId, "pauseparticipant", now);
        insertUser(outsiderUserId, "pauseoutsider", now);
        insertDeck(deckId, participantUserId, now);
        insertGame(gameId, "ACTIVE", "MAIN", 7, 3, now);
        insertParticipant(UUID.randomUUID(), gameId, participantUserId, deckId, now);

        mockMvc.perform(post("/api/games/{gameId}/pause", gameId)
                        .header("Authorization", bearerToken(outsiderUserId, "pauseoutsider"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"network issue\"}"))
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

    private void insertParticipant(UUID participantId, UUID gameId, UUID userId, UUID deckId, Instant now) {
        jdbcTemplate.update(
                "insert into game_participants (id, game_id, user_id, deck_id, player_order, is_connected, last_seen_at, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                participantId,
                gameId,
                userId,
                deckId,
                1,
                true,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertGame(UUID gameId, String status, String phase, int turnNumber, int stateVersion, Instant now) {
        jdbcTemplate.update(
                "insert into games (id, status, current_phase, turn_number, state_version, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?)",
                gameId,
                status,
                phase,
                turnNumber,
                stateVersion,
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
