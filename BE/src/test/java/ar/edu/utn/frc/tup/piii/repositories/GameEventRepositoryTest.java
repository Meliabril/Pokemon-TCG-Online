package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GameEventRepositoryTest {

    @Autowired
    private GameEventRepository gameEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistPublicAndPrivateEventsWithExpectedFields() {
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        insertGame(gameId);

        GameEvent publicEvent = event(gameId, GameEventType.TURN_STARTED, Map.of("turn", 1), 1, null,
                Instant.parse("2026-05-20T10:00:00Z"));
        GameEvent privateEvent = event(gameId, GameEventType.CARD_DRAWN, Map.of("cardId", "abc"), 2, viewerUserId,
                Instant.parse("2026-05-20T10:01:00Z"));

        gameEventRepository.save(publicEvent);
        gameEventRepository.save(privateEvent);

        List<GameEvent> persistedEvents = gameEventRepository.findByGame_IdOrderByCreatedAtAsc(gameId);

        assertThat(persistedEvents).hasSize(2);
        assertThat(persistedEvents.get(0).getVisibleToUserId()).isNull();
        assertThat(persistedEvents.get(0).getVersion()).isEqualTo(1);
        assertThat(persistedEvents.get(0).getPayload()).isEqualTo(Map.of("turn", 1));
        assertThat(persistedEvents.get(1).getVisibleToUserId()).isEqualTo(viewerUserId);
        assertThat(persistedEvents.get(1).getVersion()).isEqualTo(2);
        assertThat(persistedEvents.get(1).getPayload()).isEqualTo(Map.of("cardId", "abc"));
    }

    @Test
    void shouldReturnOnlyEventsVisibleToViewer() {
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID otherGameId = UUID.randomUUID();
        insertGame(gameId);
        insertGame(otherGameId);

        gameEventRepository.save(event(gameId, GameEventType.TURN_STARTED, Map.of(), 1, null,
                Instant.parse("2026-05-20T10:00:00Z")));
        gameEventRepository.save(event(gameId, GameEventType.CARD_DRAWN, Map.of("card", 1), 2, viewerUserId,
                Instant.parse("2026-05-20T10:01:00Z")));
        gameEventRepository.save(event(gameId, GameEventType.CARD_DRAWN, Map.of("card", 2), 3, otherUserId,
                Instant.parse("2026-05-20T10:02:00Z")));
        gameEventRepository.save(event(otherGameId, GameEventType.GAME_STARTED, Map.of(), 1, null,
                Instant.parse("2026-05-20T10:03:00Z")));

        List<GameEvent> visibleEvents = gameEventRepository.findVisibleByGameIdAndViewerUserId(gameId, viewerUserId);

        assertThat(visibleEvents).hasSize(2);
        assertThat(visibleEvents)
                .extracting(GameEvent::getVersion)
                .containsExactly(1, 2);
    }

    private GameEvent event(
            UUID gameId,
            GameEventType eventType,
            Map<String, Object> payload,
            int version,
            UUID visibleToUserId,
            Instant createdAt) {
        GameEvent gameEvent = new GameEvent();
        Game game = entityManager.getReference(Game.class, gameId);
        gameEvent.setGame(game);
        gameEvent.setEventType(eventType);
        gameEvent.setPayload(payload);
        gameEvent.setVersion(version);
        gameEvent.setVisibleToUserId(visibleToUserId);
        gameEvent.setCreatedAt(createdAt);
        return gameEvent;
    }

    private void insertGame(UUID gameId) {
        jdbcTemplate.update(
                "insert into games (id, status, turn_number, state_version, created_at, updated_at) values (?, ?, ?, ?, current_timestamp, current_timestamp)",
                gameId,
                "ACTIVE",
                0,
                0);
    }
}
