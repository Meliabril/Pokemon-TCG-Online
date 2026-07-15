package ar.edu.utn.frc.tup.piii.services.websocket;


import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateSyncDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketGameEventPublisherTest {

    private SimpMessagingTemplate messagingTemplate;
    private WebSocketGameEventPublisher publisher;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        publisher = new WebSocketGameEventPublisher(messagingTemplate);
    }

    @Test
    void shouldPublishPublicEventToGameTopic() {
        GameEventDto event = sampleEvent();

        publisher.publishPublic(event);

        verify(messagingTemplate).convertAndSend("/topic/games/" + event.gameId(), event);
    }

    @Test
    void shouldPublishPrivateEventToUserQueue() {
        GameEventDto event = sampleEvent();
        UUID recipientUserId = UUID.randomUUID();

        publisher.publishPrivate(event, recipientUserId);

        verify(messagingTemplate).convertAndSendToUser(
                recipientUserId.toString(),
                "/queue/games/" + event.gameId(),
                event);
    }

    @Test
    void shouldPublishExplicitStateSyncContractToUserQueue() {
        UUID recipientUserId = UUID.randomUUID();
        GameStateDto state = GameStateTestFactory.state(
                UUID.randomUUID(),
                ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE,
                ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase.MAIN,
                2,
                2,
                recipientUserId,
                List.of(recipientUserId),
                List.of(ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType.END_TURN),
                Instant.parse("2026-05-24T10:00:00Z"));
        GameStateSyncDto contract = new GameStateSyncDto(state.gameId(), GameEventType.STATE_SYNC, state.stateVersion(), state);

        publisher.publishPrivateStateSync(contract, recipientUserId);

        verify(messagingTemplate).convertAndSendToUser(
                recipientUserId.toString(),
                "/queue/games/" + state.gameId(),
                contract);
    }

    private GameEventDto sampleEvent() {
        return new GameEventDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GameEventType.STATE_SYNC,
                1,
                false,
                Instant.parse("2026-05-20T10:00:00Z"),
                Map.of("stateVersion", 1));
    }
}
