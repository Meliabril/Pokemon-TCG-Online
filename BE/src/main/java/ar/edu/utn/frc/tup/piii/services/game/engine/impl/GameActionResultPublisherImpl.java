package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.ActionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionLogger;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionResultPublisher;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateRestorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of the action-result persistence/publication tail, shared by
 * player-submitted actions ({@code GameServiceImpl}) and system-forced ones (turn timeout).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameActionResultPublisherImpl implements GameActionResultPublisher {

    private final GameStateQueryService gameStateQueryService;
    private final GameStateRestorer gameStateRestorer;
    private final GameSnapshotService gameSnapshotService;
    private final GameActionLogger gameActionLogger;
    private final GameRealtimeEventService gameRealtimeEventService;

    @Override
    public GameActionResponseDto publish(
            UUID gameId,
            Game game,
            GameStateDto currentState,
            GameActionExecutionResult executionResult,
            UUID actorUserId,
            UUID clientActionId,
            Map<String, Object> payload,
            String actionTypeName) {
        log.info("[{}] Iniciando publicacion final para gameId={}", actionTypeName, gameId);
        long start = System.currentTimeMillis();

        int newStateVersion = resolveNewStateVersion(currentState, executionResult);
        GameStateDto resultingState;
        if (executionResult.gameState() == null) {
            resultingState = copyStateWithVersion(currentState, newStateVersion, clientActionId);
        } else {
            resultingState = copyStateWithVersion(executionResult.gameState(), newStateVersion, clientActionId);
        }

        long tSaveChanges = System.currentTimeMillis();
        gameStateRestorer.restoreFromSnapshot(game, resultingState);
        game.setStateVersion(newStateVersion);
        log.info("[{}] Guardar cambios: {} ms", actionTypeName, System.currentTimeMillis() - tSaveChanges);

        long tBuildState = System.currentTimeMillis();
        GameStateDto canonicalVisibleState = gameStateQueryService.buildVisibleState(game);
        resultingState = mergeCanonicalRuntimeState(resultingState, canonicalVisibleState, clientActionId);
        log.info("[{}] buildCanonicalVisibleState: {} ms", actionTypeName, System.currentTimeMillis() - tBuildState);

        long tSnapshot = System.currentTimeMillis();
        gameSnapshotService.saveSnapshot(gameId, newStateVersion, resultingState, actorUserId);
        log.info("[{}] Guardar snapshot: {} ms", actionTypeName, System.currentTimeMillis() - tSnapshot);

        Map<String, Object> resultData = Map.of(
                "emittedEventsCount", executionResult.emittedEvents().size(),
                "stateVersion", newStateVersion);

        long tLog = System.currentTimeMillis();
        gameActionLogger.logAction(
                gameId,
                actorUserId,
                clientActionId,
                payload,
                resultData,
                actionTypeName,
                newStateVersion);
        log.info("[{}] Registrar log: {} ms", actionTypeName, System.currentTimeMillis() - tLog);

        long tDispatch = System.currentTimeMillis();
        for (GameEventDto emittedEvent : executionResult.emittedEvents()) {
            gameRealtimeEventService.dispatch(emittedEvent);
        }
        dispatchStateSyncForPlayersWhenNeeded(resultingState, executionResult);
        log.info("[{}] Dispatch websocket: {} ms", actionTypeName, System.currentTimeMillis() - tDispatch);

        log.info("[{}] Total publisher: {} ms", actionTypeName, System.currentTimeMillis() - start);
        return new GameActionResponseDto(
                true,
                "Action processed successfully",
                newStateVersion,
                resultData);
    }

    private int resolveNewStateVersion(GameStateDto currentState, GameActionExecutionResult executionResult) {
        if (executionResult.gameState() == null || executionResult.gameState().stateVersion() <= currentState.stateVersion()) {
            return currentState.stateVersion() + 1;
        }

        return executionResult.gameState().stateVersion();
    }

    private GameStateDto copyStateWithVersion(GameStateDto sourceState, int stateVersion, UUID clientActionId) {
        Set<UUID> processedClientActionIds = new HashSet<>(sourceState.actions().processedClientActionIds());
        if (clientActionId != null) {
            processedClientActionIds.add(clientActionId);
        }

        ActionStateDto newActions = sourceState.actions().toBuilder()
                .processedClientActionIds(Set.copyOf(processedClientActionIds))
                .build();

        return sourceState.toBuilder()
                .stateVersion(stateVersion)
                .actions(newActions)
                .updatedAt(Instant.now())
                .build();
    }

    private GameStateDto mergeCanonicalRuntimeState(
            GameStateDto sourceState,
            GameStateDto runtimeState,
            UUID clientActionId) {
        Set<UUID> processedClientActionIds = new HashSet<>(runtimeState.actions().processedClientActionIds());
        processedClientActionIds.addAll(sourceState.actions().processedClientActionIds());
        if (clientActionId != null) {
            processedClientActionIds.add(clientActionId);
        }

        ActionStateDto newActions = sourceState.actions().toBuilder()
                .availableActions(runtimeState.actions().availableActions())
                .processedClientActionIds(Set.copyOf(processedClientActionIds))
                .build();

        return sourceState.toBuilder()
                .players(runtimeState.players())
                .boardPlayers(runtimeState.boardPlayers())
                .board(runtimeState.board())
                .actions(newActions)
                .build();
    }

    private void dispatchStateSyncForPlayersWhenNeeded(
            GameStateDto resultingState,
            GameActionExecutionResult executionResult) {
        if (hasStateSyncEvent(executionResult)) {
            return;
        }

        for (UUID playerId : resultingState.playerIds()) {
            GameStateDto visibleState = gameStateQueryService.sanitizeVisibleStateForViewer(resultingState, playerId);
            log.info("[PUBLISH] STATE_SYNC reutiliza estado canonico para viewerUserId={} sin reconstruir desde DB", playerId);
            gameRealtimeEventService.dispatchStateSync(visibleState, playerId);
        }
    }

    private boolean hasStateSyncEvent(GameActionExecutionResult executionResult) {
        for (GameEventDto event : executionResult.emittedEvents()) {
            if (GameEventType.STATE_SYNC.equals(event.eventType())) {
                return true;
            }
        }

        return false;
    }
}
