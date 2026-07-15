package ar.edu.utn.frc.tup.piii.services.matchmaking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MatchFoundNotificationListenerTest {

    private MatchFoundNotifier matchFoundNotifier;
    private MatchFoundNotificationListener listener;

    @BeforeEach
    void setUp() {
        matchFoundNotifier = mock(MatchFoundNotifier.class);
        listener = new MatchFoundNotificationListener(matchFoundNotifier);
    }

    @Test
    void shouldDelegateMatchFoundNotificationThroughPort() {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        Instant matchedAt = Instant.parse("2026-05-24T11:00:00Z");

        listener.onMatchFound(new MatchFoundApplicationEvent(firstUserId, secondUserId, gameId, matchedAt));

        verify(matchFoundNotifier).notifyMatchFound(
                eq(firstUserId),
                eq(secondUserId),
                eq(gameId),
                eq(matchedAt));
    }
}
