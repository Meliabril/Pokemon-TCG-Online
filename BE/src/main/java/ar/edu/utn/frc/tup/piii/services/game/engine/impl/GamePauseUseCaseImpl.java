package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GamePauseUseCase;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GamePauseUseCaseImpl implements GamePauseUseCase {

    private final GameLookupService gameLookupService;
    private final GameStateQueryService gameStateQueryService;
    private final GameSnapshotService gameSnapshotService;
    private final GameRealtimeEventService gameRealtimeEventService;

    @Override
    @Transactional
    public GameStateDto pause(UUID gameId, String reason) {
        Game game = gameLookupService.getRequiredGame(gameId);

        if (game.getStatus() == GameStatus.PAUSED) {
            throw new InvalidGameActionException("Game is already paused");
        }

        if (game.getStatus() == GameStatus.FINISHED || game.getStatus() == GameStatus.CANCELLED) {
            throw new InvalidGameActionException("Finished or cancelled games cannot be paused");
        }

        GameStateDto resumableState = latestOrCurrentVisibleState(gameId, game);
        if (shouldPersistPauseSnapshot(gameId, game.getStateVersion())) {
            gameSnapshotService.saveSnapshot(gameId, game.getStateVersion(), resumableState, null);
        }

        game.setStatus(GameStatus.PAUSED);
        game.setPauseReason(reason);
        game.setPausedAt(Instant.now());
        game.setStateVersion(game.getStateVersion() + 1);

        GameStateDto pausedState = copyStateWithStatus(resumableState, GameStatus.PAUSED, game.getStateVersion(), Instant.now());
        Map<String, Object> payload = Map.of();
        if (reason != null) {
            payload = Map.of("reason", reason);
        }
        gameRealtimeEventService.dispatchPublic(
                gameId,
                GameEventType.GAME_PAUSED,
                pausedState.stateVersion(),
                payload);

        return pausedState;
    }

    private GameStateDto latestOrCurrentVisibleState(UUID gameId, Game game) {
        Optional<GameStateDto> latestVisibleState = gameSnapshotService.findLatestVisibleState(gameId, null);
        if (latestVisibleState.isPresent() && latestVisibleState.get().stateVersion() == game.getStateVersion()) {
            return latestVisibleState.get();
        }

        return gameStateQueryService.buildVisibleState(game);
    }

    private GameStateDto copyStateWithStatus(GameStateDto sourceState, GameStatus status, int stateVersion, Instant updatedAt) {
        return sourceState.toBuilder()
                .status(status)
                .stateVersion(stateVersion)
                .updatedAt(updatedAt)
                .build();
    }

    private boolean shouldPersistPauseSnapshot(UUID gameId, int stateVersion) {
        Optional<GameStateDto> latestVisibleState = gameSnapshotService.findLatestVisibleState(gameId, null);
        if (latestVisibleState.isEmpty()) {
            return true;
        }

        return latestVisibleState.get().stateVersion() != stateVersion;
    }
}
