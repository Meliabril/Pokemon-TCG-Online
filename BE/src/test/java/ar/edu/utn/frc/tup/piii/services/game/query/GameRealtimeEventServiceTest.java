package ar.edu.utn.frc.tup.piii.services.game.query;




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
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateSyncDto;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.GameRealtimeEventServiceImpl;
import ar.edu.utn.frc.tup.piii.services.websocket.GameEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameRealtimeEventServiceTest {

    @Mock
    private GameEventService gameEventService;

    @Mock
    private GameEventPublisher gameEventPublisher;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldPersistPublicEventAndPublishItAfterCommit() {
        GameRealtimeEventService service = new GameRealtimeEventServiceImpl(gameEventService, gameEventPublisher, objectMapper);
        UUID gameId = UUID.randomUUID();
        GameEventDto persistedEvent = event(gameId, GameEventType.GAME_PAUSED, false, Map.of("reason", "manual pause"));
        when(gameEventService.recordPublicEvent(gameId, GameEventType.GAME_PAUSED, 3, Map.of("reason", "manual pause")))
                .thenReturn(persistedEvent);
        TransactionSynchronizationManager.initSynchronization();

        service.dispatchPublic(gameId, GameEventType.GAME_PAUSED, 3, Map.of("reason", "manual pause"));

        InOrder inOrder = inOrder(gameEventService, gameEventPublisher);
        inOrder.verify(gameEventService).recordPublicEvent(gameId, GameEventType.GAME_PAUSED, 3, Map.of("reason", "manual pause"));
        verifyNoInteractions(gameEventPublisher);

        TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();

        verify(gameEventPublisher).publishPublic(persistedEvent);
    }

    @Test
    void shouldPersistPrivateEventAndPublishImmediatelyWhenNoTransactionIsActive() {
        GameRealtimeEventService service = new GameRealtimeEventServiceImpl(gameEventService, gameEventPublisher, objectMapper);
        UUID gameId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        GameEventDto persistedEvent = event(gameId, GameEventType.STATE_SYNC, true, Map.of("stateVersion", 7));
        when(gameEventService.recordPrivateEvent(gameId, GameEventType.STATE_SYNC, 7, Map.of("stateVersion", 7), recipientUserId))
                .thenReturn(persistedEvent);

        service.dispatchPrivate(gameId, GameEventType.STATE_SYNC, 7, Map.of("stateVersion", 7), recipientUserId);

        InOrder inOrder = inOrder(gameEventService, gameEventPublisher);
        inOrder.verify(gameEventService).recordPrivateEvent(gameId, GameEventType.STATE_SYNC, 7, Map.of("stateVersion", 7), recipientUserId);
        inOrder.verify(gameEventPublisher).publishPrivate(persistedEvent, recipientUserId);
    }

    @Test
    void shouldPersistNestedStatePayloadAndPublishExplicitStateSyncContract() {
        GameRealtimeEventService service = new GameRealtimeEventServiceImpl(gameEventService, gameEventPublisher, objectMapper);
        UUID gameId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE,
                ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase.MAIN,
                4,
                4,
                recipientUserId,
                List.of(recipientUserId, UUID.randomUUID()),
                List.of(ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType.END_TURN),
                Instant.parse("2026-05-24T10:00:00Z"));
        GameEventDto persistedEvent = event(gameId, GameEventType.STATE_SYNC, true, Map.of("state", Map.of("stateVersion", 4)));
        when(gameEventService.recordPrivateEvent(org.mockito.ArgumentMatchers.eq(gameId), org.mockito.ArgumentMatchers.eq(GameEventType.STATE_SYNC), org.mockito.ArgumentMatchers.eq(4), org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.eq(recipientUserId)))
                .thenReturn(persistedEvent);

        service.dispatchStateSync(state, recipientUserId);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventService).recordPrivateEvent(org.mockito.ArgumentMatchers.eq(gameId), org.mockito.ArgumentMatchers.eq(GameEventType.STATE_SYNC), org.mockito.ArgumentMatchers.eq(4), payloadCaptor.capture(), org.mockito.ArgumentMatchers.eq(recipientUserId));
        ArgumentCaptor<GameStateSyncDto> contractCaptor = ArgumentCaptor.forClass(GameStateSyncDto.class);
        verify(gameEventPublisher).publishPrivateStateSync(contractCaptor.capture(), org.mockito.ArgumentMatchers.eq(recipientUserId));
        assertThat(payloadCaptor.getValue()).containsKey("state");
        Map<String, Object> statePayload = (Map<String, Object>) payloadCaptor.getValue().get("state");
        Map<String, Object> turnPayload = (Map<String, Object>) statePayload.get("turn");
        assertThat(statePayload).containsEntry("stateVersion", 4);
        assertThat(turnPayload).containsEntry("playerWhoWentFirstId", null);
        assertThat(contractCaptor.getValue().gameId()).isEqualTo(gameId);
        assertThat(contractCaptor.getValue().eventType()).isEqualTo(GameEventType.STATE_SYNC);
        assertThat(contractCaptor.getValue().state()).isEqualTo(state);
        assertThat(contractCaptor.getValue().state().turn().playerWhoWentFirstId()).isNull();
    }

    @Test
    void shouldDispatchPrivateEventsUsingViewerUserIdEmbeddedInPayload() {
        GameRealtimeEventService service = new GameRealtimeEventServiceImpl(gameEventService, gameEventPublisher, objectMapper);
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        GameEventDto persistedEvent = event(gameId, GameEventType.CARD_DRAWN, true, Map.of("cardId", "abc"));
        when(gameEventService.recordPrivateEvent(gameId, GameEventType.CARD_DRAWN, 7, Map.of("cardId", "abc"), viewerUserId))
                .thenReturn(persistedEvent);

        service.dispatch(new GameEventDto(
                UUID.randomUUID(),
                gameId,
                GameEventType.CARD_DRAWN,
                7,
                true,
                Instant.parse("2026-05-24T00:00:00Z"),
                Map.of("cardId", "abc", "viewerUserId", viewerUserId.toString())));

        verify(gameEventService).recordPrivateEvent(gameId, GameEventType.CARD_DRAWN, 7, Map.of("cardId", "abc"), viewerUserId);
        verify(gameEventPublisher).publishPrivate(persistedEvent, viewerUserId);
    }

    private GameEventDto event(UUID gameId, GameEventType type, boolean privateEvent, Map<String, Object> payload) {
        return new GameEventDto(
                UUID.randomUUID(),
                gameId,
                type,
                7,
                privateEvent,
                Instant.parse("2026-05-24T00:00:00Z"),
                payload);
    }
}
