package ar.edu.utn.frc.tup.piii.services.game.query.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateSyncDto;
import ar.edu.utn.frc.tup.piii.services.game.query.GameEventService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.websocket.GameEventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameRealtimeEventServiceImpl implements GameRealtimeEventService {

    private final GameEventService gameEventService;
    private final GameEventPublisher gameEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public void dispatchPublic(UUID gameId, GameEventType type, int version, Map<String, Object> payload) {
        GameEventDto persistedEvent = gameEventService.recordPublicEvent(gameId, type, version, payload);
        publishAfterCommit(new Runnable() {
            @Override
            public void run() {
                gameEventPublisher.publishPublic(persistedEvent);
            }
        });
    }

    @Override
    public void dispatchPrivate(UUID gameId, GameEventType type, int version, Map<String, Object> payload, UUID viewerUserId) {
        GameEventDto persistedEvent = gameEventService.recordPrivateEvent(gameId, type, version, payload, viewerUserId);
        publishAfterCommit(new Runnable() {
            @Override
            public void run() {
                gameEventPublisher.publishPrivate(persistedEvent, viewerUserId);
            }
        });
    }

    @Override
    public void dispatchStateSync(GameStateDto state, UUID viewerUserId) {
        Map<String, Object> persistedPayload = Map.of("state", objectMapper.convertValue(state, new TypeReference<Map<String, Object>>() {
        }));
        gameEventService.recordPrivateEvent(
                state.gameId(),
                GameEventType.STATE_SYNC,
                state.stateVersion(),
                persistedPayload,
                viewerUserId);
        GameStateSyncDto contract = GameStateSyncDto.from(state);
        publishAfterCommit(new Runnable() {
            @Override
            public void run() {
                gameEventPublisher.publishPrivateStateSync(contract, viewerUserId);
            }
        });
    }

    @Override
    public void dispatch(GameEventDto event) {
        if (event.privateEvent()) {
            UUID viewerUserId = viewerUserId(event.payload());
            if (event.eventType() == GameEventType.STATE_SYNC) {
                Object statePayload = event.payload().get("state");
                GameStateDto state = objectMapper.convertValue(statePayload, GameStateDto.class);
                dispatchStateSync(state, viewerUserId);
                return;
            }

            dispatchPrivate(event.gameId(), event.eventType(), event.stateVersion(), payloadWithoutViewer(event.payload()), viewerUserId);
            return;
        }

        dispatchPublic(event.gameId(), event.eventType(), event.stateVersion(), event.payload());
    }

    private UUID viewerUserId(Map<String, Object> payload) {
        Object viewerUserId = payload.get("viewerUserId");
        if (viewerUserId instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(viewerUserId));
    }

    private Map<String, Object> payloadWithoutViewer(Map<String, Object> payload) {
        java.util.LinkedHashMap<String, Object> copiedPayload = new java.util.LinkedHashMap<>(payload);
        copiedPayload.remove("viewerUserId");
        return Map.copyOf(copiedPayload);
    }

    private void publishAfterCommit(Runnable publishAction) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }

        publishAction.run();
    }
}
