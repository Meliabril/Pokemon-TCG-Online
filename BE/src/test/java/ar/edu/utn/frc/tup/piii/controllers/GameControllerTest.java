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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerTest extends PostgreSqlDockerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void cleanup() {
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
        jdbcTemplate.update("delete from attack_costs");
        jdbcTemplate.update("delete from attacks");
        jdbcTemplate.update("delete from card_weaknesses");
        jdbcTemplate.update("delete from card_resistances");
        jdbcTemplate.update("delete from cards");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldReturnGameWithParticipants() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userOneId = UUID.randomUUID();
        UUID userTwoId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-06T12:00:00Z");

        insertUser(userOneId, "userone", now);
        insertUser(userTwoId, "usertwo", now);
        insertDeck(deckOneId, userOneId, now);
        insertDeck(deckTwoId, userTwoId, now);
        insertGame(gameId, "ACTIVE", "MAIN", 4, 2, userOneId, now);
        insertParticipant(UUID.randomUUID(), gameId, userTwoId, deckTwoId, 2, false, now);
        insertParticipant(UUID.randomUUID(), gameId, userOneId, deckOneId, 1, true, now);

        mockMvc.perform(get("/api/games/{gameId}", gameId)
                        .header("Authorization", bearerToken(userOneId, "userone")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentPhase").value("MAIN"))
                .andExpect(jsonPath("$.activePlayerId").value(userOneId.toString()))
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.participants[0].playerOrder").value(1))
                .andExpect(jsonPath("$.participants[0].userId").value(userOneId.toString()))
                .andExpect(jsonPath("$.participants[1].playerOrder").value(2))
                .andExpect(jsonPath("$.participants[1].userId").value(userTwoId.toString()));
    }

    @Test
    void shouldReturn404WhenGameDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-06T12:00:00Z");
        insertUser(userId, "viewer404", now);

        mockMvc.perform(get("/api/games/{gameId}", UUID.randomUUID())
                        .header("Authorization", bearerToken(userId, "viewer404")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldReturnForbiddenWhenAuthenticatedUserIsNotParticipant() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID participantUserId = UUID.randomUUID();
        UUID outsiderUserId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-06T12:00:00Z");

        insertUser(participantUserId, "participant", now);
        insertUser(outsiderUserId, "outsider", now);
        insertDeck(deckId, participantUserId, now);
        insertGame(gameId, "ACTIVE", "MAIN", 1, 0, participantUserId, now);
        insertParticipant(UUID.randomUUID(), gameId, participantUserId, deckId, 1, true, now);

        mockMvc.perform(get("/api/games/{gameId}", gameId)
                        .header("Authorization", bearerToken(outsiderUserId, "outsider")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN_ACTION"));
    }

    @Test
    void shouldExecuteActionForAuthenticatedParticipantUsingPersistedState() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-06T12:00:00Z");

        insertUser(userId, "actionuser", now);
        insertUser(opponentUserId, "actionopponent", now);
        insertDeck(deckId, userId, now);
        insertDeck(opponentDeckId, opponentUserId, now);
        insertGame(gameId, "ACTIVE", "MAIN", 4, 2, userId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, 1, true, now);
        insertParticipant(UUID.randomUUID(), gameId, opponentUserId, opponentDeckId, 2, true, now);

        String requestBody = """
                {
                  "gameId":"%s",
                  "clientActionId":"%s",
                  "actionType":"END_TURN",
                  "expectedStateVersion":2,
                  "payload":{}
                }
                """.formatted(gameId, UUID.randomUUID());

        mockMvc.perform(post("/api/games/{gameId}/actions", gameId)
                        .header("Authorization", bearerToken(userId, "actionuser"))
                        .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newStateVersion").value(3))
                .andExpect(jsonPath("$.data.emittedEventsCount").value(3));
    }

    @Test
    void shouldReturnConflictWhenActionUsesStaleStateVersion() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-06T12:00:00Z");

        insertUser(userId, "staleuser", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, "ACTIVE", "MAIN", 4, 2, userId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, 1, true, now);

        String requestBody = """
                {
                  "gameId":"%s",
                  "clientActionId":"%s",
                  "actionType":"END_TURN",
                  "expectedStateVersion":1,
                  "payload":{}
                }
                """.formatted(gameId, UUID.randomUUID());

        mockMvc.perform(post("/api/games/{gameId}/actions", gameId)
                        .header("Authorization", bearerToken(userId, "staleuser"))
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONCURRENT_GAME_STATE"));
    }

    @Test
    void shouldAdvanceStateVersionAcrossAcceptedActions() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-06T12:00:00Z");

        insertUser(userId, "sequenceuser", now);
        insertUser(opponentUserId, "sequenceopponent", now);
        insertDeck(deckId, userId, now);
        insertDeck(opponentDeckId, opponentUserId, now);
        insertGame(gameId, "ACTIVE", "MAIN", 4, 2, userId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, 1, true, now);
        insertParticipant(UUID.randomUUID(), gameId, opponentUserId, opponentDeckId, 2, true, now);
        UUID opponentDrawCardId = UUID.randomUUID();
        insertCard(opponentDrawCardId, now);
        insertDeckCardInstance(UUID.randomUUID(), gameId, opponentUserId, opponentDrawCardId, 1, now);

        String firstRequest = """
                {
                  "gameId":"%s",
                  "clientActionId":"%s",
                  "actionType":"END_TURN",
                  "expectedStateVersion":2,
                  "payload":{"step":1}
                }
                """.formatted(gameId, UUID.randomUUID());

        String secondRequest = """
                {
                  "gameId":"%s",
                  "clientActionId":"%s",
                  "actionType":"DRAW_CARD",
                  "expectedStateVersion":3,
                  "payload":{"step":2}
                }
                """.formatted(gameId, UUID.randomUUID());

        mockMvc.perform(post("/api/games/{gameId}/actions", gameId)
                        .header("Authorization", bearerToken(userId, "sequenceuser"))
                        .contentType(APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStateVersion").value(3));

        mockMvc.perform(post("/api/games/{gameId}/actions", gameId)
                        .header("Authorization", bearerToken(opponentUserId, "sequenceopponent"))
                        .contentType(APPLICATION_JSON)
                        .content(secondRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStateVersion").value(4));
    }

    @Test
    void shouldRejectPlayingBasicPokemonWhenBenchIsFull() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-06T12:00:00Z");

        insertUser(userId, "benchuser", now);
        insertDeck(deckId, userId, now);
        insertGame(gameId, "ACTIVE", "MAIN", 4, 3, userId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, 1, true, now);

        for (int position = 1; position <= 5; position++) {
            UUID cardId = UUID.randomUUID();
            insertCard(cardId, now);
            insertBenchCardInstance(UUID.randomUUID(), gameId, userId, cardId, position, now);
        }

        String requestBody = """
                {
                  "gameId":"%s",
                  "clientActionId":"%s",
                  "actionType":"PLAY_BASIC_POKEMON",
                  "expectedStateVersion":3,
                  "payload":{}
                }
                """.formatted(gameId, UUID.randomUUID());

        mockMvc.perform(post("/api/games/{gameId}/actions", gameId)
                        .header("Authorization", bearerToken(userId, "benchuser"))
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_GAME_ACTION"));
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

    private void insertGame(UUID gameId, String status, String phase, int turnNumber, int stateVersion, UUID activePlayerId, Instant now) {
        jdbcTemplate.update(
                "insert into games (id, status, current_phase, turn_number, state_version, active_player_id, pause_reason, started_at, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                gameId,
                status,
                phase,
                turnNumber,
                stateVersion,
                activePlayerId,
                null,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertParticipant(
            UUID participantId,
            UUID gameId,
            UUID userId,
            UUID deckId,
            int playerOrder,
            boolean connected,
            Instant now) {
        jdbcTemplate.update(
                "insert into game_participants (id, game_id, user_id, deck_id, player_order, is_connected, last_seen_at, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                participantId,
                gameId,
                userId,
                deckId,
                playerOrder,
                connected,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertCard(UUID cardId, Instant now) {
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

    private void insertBenchCardInstance(UUID instanceId, UUID gameId, UUID ownerUserId, UUID cardId, int zonePosition, Instant now) {
        jdbcTemplate.update(
                "insert into game_card_instances (id, game_id, owner_user_id, card_id, zone, zone_position, is_face_down, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                instanceId,
                gameId,
                ownerUserId,
                cardId,
                "BENCH",
                zonePosition,
                false,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertDeckCardInstance(UUID instanceId, UUID gameId, UUID ownerUserId, UUID cardId, int zonePosition, Instant now) {
        jdbcTemplate.update(
                "insert into game_card_instances (id, game_id, owner_user_id, card_id, zone, zone_position, is_face_down, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                instanceId,
                gameId,
                ownerUserId,
                cardId,
                "DECK",
                zonePosition,
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
