package ar.edu.utn.frc.tup.piii.services.game.presence.impl;



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
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.GamePresenceNotificationListener;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GamePresenceNotificationListenerTest {

    private final GameRealtimeEventService gameRealtimeEventService = mock(GameRealtimeEventService.class);
    private final GamePresenceNotificationListener listener = new GamePresenceNotificationListener(gameRealtimeEventService);

    @Test
    void shouldDispatchConnectedPresenceUpdate() {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant lastSeenAt = Instant.parse("2026-05-30T12:00:00Z");

        listener.onPresenceChanged(new GameParticipantPresenceChangedEvent(gameId, 7, userId, true, lastSeenAt));

        verify(gameRealtimeEventService).dispatchPublic(
                gameId,
                GameEventType.PARTICIPANT_PRESENCE_CHANGED,
                7,
                Map.of(
                        "userId", userId.toString(),
                        "connected", true,
                        "lastSeenAt", lastSeenAt.toString()));
    }

    @Test
    void shouldDispatchDisconnectedPresenceUpdateWithNullLastSeenAt() {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        listener.onPresenceChanged(new GameParticipantPresenceChangedEvent(gameId, 3, userId, false, null));

        verify(gameRealtimeEventService).dispatchPublic(
                eq(gameId),
                eq(GameEventType.PARTICIPANT_PRESENCE_CHANGED),
                eq(3),
                argThat(disconnectedPresencePayload(userId)));
    }

    private org.mockito.ArgumentMatcher<Map<String, Object>> disconnectedPresencePayload(UUID userId) {
        return new org.mockito.ArgumentMatcher<Map<String, Object>>() {
            @Override
            public boolean matches(Map<String, Object> payload) {
                return userId.toString().equals(payload.get("userId"))
                        && Boolean.FALSE.equals(payload.get("connected"))
                        && payload.containsKey("lastSeenAt")
                        && payload.get("lastSeenAt") == null;
            }
        };
    }
}
