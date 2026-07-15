package ar.edu.utn.frc.tup.piii.services.matchmaking;

import ar.edu.utn.frc.tup.piii.dtos.websocket.MatchFoundDto;
import ar.edu.utn.frc.tup.piii.services.matchmaking.impl.StompMatchFoundNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StompMatchFoundNotifierTest {

    private SimpMessagingTemplate messagingTemplate;
    private StompMatchFoundNotifier notifier;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        notifier = new StompMatchFoundNotifier(messagingTemplate);
    }

    @Test
    void shouldNotifyBothMatchedUsersInPrivateQueue() {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        Instant matchedAt = Instant.parse("2026-05-24T11:00:00Z");

        notifier.notifyMatchFound(firstUserId, secondUserId, gameId, matchedAt);

        verify(messagingTemplate).convertAndSendToUser(
                eq(firstUserId.toString()),
                eq("/queue/matchmaking"),
                eq(new MatchFoundDto(secondUserId, gameId, matchedAt)));
        verify(messagingTemplate).convertAndSendToUser(
                eq(secondUserId.toString()),
                eq("/queue/matchmaking"),
                eq(new MatchFoundDto(firstUserId, gameId, matchedAt)));
    }

    @Test
    void shouldAlwaysPublishExactlyTwoNotificationsPerMatchEvent() {
        notifier.notifyMatchFound(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-05-24T11:05:00Z"));

        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/matchmaking"), any(MatchFoundDto.class));
    }
}
