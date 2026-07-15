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
        int newStateVersion = resolveNewStateVersion(currentState, executionResult);
        GameStateDto resultingState;
        if (executionResult.gameState() == null) {
            resultingState = copyStateWithVersion(currentState, newStateVersion, clientActionId);
        } else {
            resultingState = copyStateWithVersion(executionResult.gameState(), newStateVersion, clientActionId);
        }

        gameStateRestorer.restoreFromSnapshot(game, resultingState);
        game.setStateVersion(newStateVersion);
        GameStateDto visibleRuntimeState = gameStateQueryService.buildVisibleState(game);
        resultingState = copyStateWithRuntimeMaps(resultingState, visibleRuntimeState, clientActionId);

        gameSnapshotService.saveSnapshot(gameId, newStateVersion, resultingState, actorUserId);

        Map<String, Object> resultData = Map.of(
                "emittedEventsCount", executionResult.emittedEvents().size(),
                "stateVersion", newStateVersion);

        gameActionLogger.logAction(
                gameId,
                actorUserId,
                clientActionId,
                payload,
                resultData,
                actionTypeName,
                newStateVersion);

        for (GameEventDto emittedEvent : executionResult.emittedEvents()) {
            gameRealtimeEventService.dispatch(emittedEvent);
        }
        dispatchStateSyncForPlayersWhenNeeded(game, resultingState, executionResult);

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

    private GameStateDto copyStateWithRuntimeMaps(
            GameStateDto sourceState,
            GameStateDto runtimeState,
            UUID clientActionId) {
        Set<UUID> processedClientActionIds = new HashSet<>(runtimeState.actions().processedClientActionIds());
        processedClientActionIds.addAll(sourceState.actions().processedClientActionIds());
        if (clientActionId != null) {
            processedClientActionIds.add(clientActionId);
        }

        ActionStateDto newActions = sourceState.actions().toBuilder()
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
            Game game,
            GameStateDto resultingState,
            GameActionExecutionResult executionResult) {
        if (hasStateSyncEvent(executionResult)) {
            return;
        }

        for (UUID playerId : resultingState.playerIds()) {
            GameStateDto visibleState = gameStateQueryService.buildVisibleState(game, playerId);
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
