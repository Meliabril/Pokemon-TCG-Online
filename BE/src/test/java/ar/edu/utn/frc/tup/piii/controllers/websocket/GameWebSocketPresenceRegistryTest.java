package ar.edu.utn.frc.tup.piii.controllers.websocket;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameWebSocketPresenceRegistryTest {

    @Test
    void shouldReturnTrueOnlyWhenExplicitLeaveRemovesTheLastTrackedSessionForThatGame() {
        GameWebSocketPresenceRegistry registry = new GameWebSocketPresenceRegistry();
        String sessionId = "session-1";
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        registry.register(sessionId, gameId, userId);

        assertThat(registry.unregister(sessionId, gameId, userId)).isTrue();
        assertThat(registry.unregisterSession(sessionId)).isEmpty();
    }

    @Test
    void shouldKeepOtherTrackedGamesWhenExplicitLeaveRemovesOnlyOneGameFromTheSession() {
        GameWebSocketPresenceRegistry registry = new GameWebSocketPresenceRegistry();
        String sessionId = "session-1";
        UUID firstGameId = UUID.randomUUID();
        UUID secondGameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        registry.register(sessionId, firstGameId, userId);
        registry.register(sessionId, secondGameId, userId);

        assertThat(registry.unregister(sessionId, firstGameId, userId)).isTrue();
        assertThat(registry.unregisterSession(sessionId))
                .containsExactly(new GameWebSocketPresenceRegistry.GamePresenceKey(secondGameId, userId));
    }
}
