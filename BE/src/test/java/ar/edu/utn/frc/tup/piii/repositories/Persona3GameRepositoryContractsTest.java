package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameActionLog;
import ar.edu.utn.frc.tup.piii.entities.GameEvent;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.GameStateSnapshot;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class Persona3GameRepositoryContractsTest {

    @Autowired
    private GameAccessRepository gameRepository;

    @Autowired
    private MatchmakingGameAccessRepository matchmakingGameRepository;

    @Autowired
    private GameParticipantAccessRepository gameParticipantRepository;

    @Autowired
    private GameSnapshotStore gameSnapshotStore;

    @Autowired
    private GameActionLogReadRepository gameActionLogReadRepository;

    @Autowired
    private GameEventAccessRepository gameEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindGameDetailByIdWithParticipantsLoaded() {
        UUID gameId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-21T10:00:00Z");
        insertGame(gameId);
        insertUserAndDeck(firstUserId, now);
        insertUserAndDeck(secondUserId, now.plusSeconds(1));
        persistParticipant(gameId, firstUserId, 2, now.plusSeconds(2));
        persistParticipant(gameId, secondUserId, 1, now.plusSeconds(3));
        entityManager.flush();
        entityManager.clear();

        Game game = gameRepository.findDetailById(gameId).orElseThrow();

        assertThat(game.getParticipants())
                .extracting(GameParticipant::getPlayerOrder)
                .containsExactly(1, 2);
    }

    @Test
    void shouldReturnParticipantsOrderedByPlayerOrder() {
        UUID gameId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-21T10:05:00Z");
        insertGame(gameId);
        insertUserAndDeck(firstUserId, now);
        insertUserAndDeck(secondUserId, now.plusSeconds(1));
        persistParticipant(gameId, firstUserId, 2, now.plusSeconds(2));
        persistParticipant(gameId, secondUserId, 1, now.plusSeconds(3));

        List<GameParticipant> participants = gameParticipantRepository.findOrderedByGameId(gameId);

        assertThat(participants)
                .extracting(GameParticipant::getPlayerOrder)
                .containsExactly(1, 2);
    }

    @Test
    void shouldFindLatestParticipantGameWithoutPaginatingOverParticipants() {
        UUID olderGameId = UUID.randomUUID();
        UUID latestGameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant olderTime = Instant.parse("2026-05-21T10:06:00Z");
        Instant latestTime = Instant.parse("2026-05-21T10:07:00Z");
        insertGame(olderGameId, GameStatus.ACTIVE, olderTime);
        insertGame(latestGameId, GameStatus.ACTIVE, latestTime);
        insertUserAndDeck(userId, olderTime);
        persistParticipant(olderGameId, userId, 1, olderTime);
        persistParticipant(latestGameId, userId, 1, latestTime);
        entityManager.flush();
        entityManager.clear();

        Game latestGame = matchmakingGameRepository
                .findLatestByParticipantUserIdAndStatusIn(userId, List.of(GameStatus.ACTIVE))
                .orElseThrow();

        assertThat(latestGame.getId()).isEqualTo(latestGameId);
        assertThat(latestGame.getParticipants())
                .extracting(GameParticipant::getUserId)
                .containsExactly(userId);
    }

    @Test
    void shouldReturnLatestSnapshotByVersion() {
        UUID gameId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-21T10:10:00Z");
        insertGame(gameId);
        persistSnapshot(gameId, 1, now, Map.of("stateVersion", 1));
        persistSnapshot(gameId, 2, now.plusSeconds(10), Map.of("stateVersion", 2));

        GameStateSnapshot snapshot = gameSnapshotStore.findLatestByGameId(gameId).orElseThrow();

        assertThat(snapshot.getVersion()).isEqualTo(2);
        assertThat(snapshot.getStateJson()).containsEntry("stateVersion", 2);
    }

    @Test
    void shouldReturnVisibleEventsOrderedByVersionAndCreatedAt() {
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-21T10:15:00Z");
        insertGame(gameId);
        persistEvent(gameId, GameEventType.GAME_STARTED, 1, null, now, Map.of("step", 1));
        persistEvent(gameId, GameEventType.STATE_SYNC, 2, viewerUserId, now.plusSeconds(2), Map.of("step", 2));
        persistEvent(gameId, GameEventType.STATE_SYNC, 3, otherUserId, now.plusSeconds(1), Map.of("step", 3));

        List<GameEvent> events = gameEventRepository.findVisibleEvents(gameId, viewerUserId);

        assertThat(events)
                .extracting(GameEvent::getVersion)
                .containsExactly(1, 2);
    }

    @Test
    void shouldReturnActionLogHistoryOrderedByVersion() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-21T10:20:00Z");
        insertGame(gameId);
        insertUser(actorUserId, now);
        persistActionLog(gameId, actorUserId, 2, now.plusSeconds(10));
        persistActionLog(gameId, actorUserId, 1, now.plusSeconds(20));

        List<GameActionLog> history = gameActionLogReadRepository.findHistory(gameId);

        assertThat(history)
                .extracting(GameActionLog::getVersion)
                .containsExactly(1, 2);
    }

    private void insertGame(UUID gameId) {
        jdbcTemplate.update(
                "insert into games (id, status, turn_number, state_version, created_at, updated_at) values (?, ?, ?, ?, current_timestamp, current_timestamp)",
                gameId,
                "ACTIVE",
                1,
                0);
    }

    private void insertGame(UUID gameId, GameStatus status, Instant createdAt) {
        jdbcTemplate.update(
                "insert into games (id, status, turn_number, state_version, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                gameId,
                status.name(),
                1,
                0,
                Timestamp.from(createdAt),
                Timestamp.from(createdAt));
    }

    private void insertUserAndDeck(UUID userId, Instant now) {
        insertUser(userId, now);
        jdbcTemplate.update(
                "insert into decks (id, owner_user_id, name, format, is_valid, is_active, validation_errors, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                userId,
                "Deck " + userId,
                "XY1_UNLIMITED",
                true,
                true,
                null,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertUser(UUID userId, Instant now) {
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
    }

    private void persistParticipant(UUID gameId, UUID userId, int playerOrder, Instant createdAt) {
        GameParticipant participant = new GameParticipant();
        participant.setGame(entityManager.getReference(Game.class, gameId));
        participant.setUserId(userId);
        participant.setDeckId(userId);
        participant.setPlayerOrder(playerOrder);
        participant.setConnected(true);
        participant.setCreatedAt(createdAt);
        entityManager.persist(participant);
    }

    private void persistSnapshot(UUID gameId, int version, Instant createdAt, Map<String, Object> stateJson) {
        GameStateSnapshot snapshot = new GameStateSnapshot();
        snapshot.setGame(entityManager.getReference(Game.class, gameId));
        snapshot.setVersion(version);
        snapshot.setStateJson(stateJson);
        snapshot.setChecksum("a".repeat(64));
        snapshot.setCreatedAt(createdAt);
        entityManager.persist(snapshot);
    }

    private void persistEvent(
            UUID gameId,
            GameEventType eventType,
            int version,
            UUID visibleToUserId,
            Instant createdAt,
            Map<String, Object> payload) {
        GameEvent event = new GameEvent();
        event.setGame(entityManager.getReference(Game.class, gameId));
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setVersion(version);
        event.setVisibleToUserId(visibleToUserId);
        event.setCreatedAt(createdAt);
        entityManager.persist(event);
    }

    private void persistActionLog(UUID gameId, UUID actorUserId, int version, Instant createdAt) {
        GameActionLog log = new GameActionLog();
        log.setGame(entityManager.getReference(Game.class, gameId));
        log.setActorUserId(actorUserId);
        log.setActionType(GameActionType.DRAW_CARD);
        log.setPayload(Map.of("version", version));
        log.setResult(Map.of("ok", true));
        log.setVersion(version);
        log.setClientActionId(UUID.randomUUID());
        log.setCreatedAt(createdAt);
        entityManager.persist(log);
    }
}
