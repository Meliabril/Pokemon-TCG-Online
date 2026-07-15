package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameResumeUseCase;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateRestorer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameResumeUseCaseImpl implements GameResumeUseCase {

    private final GameLookupService gameLookupService;
    private final GameStateQueryService gameStateQueryService;
    private final GameStateRestorer gameStateRestorer;
    private final GameSnapshotService gameSnapshotService;
    private final GameRealtimeEventService gameRealtimeEventService;

    @Override
    @Transactional
    public GameStateDto resume(UUID gameId) {
        Game game = gameLookupService.getRequiredGame(gameId);

        if (game.getStatus() != GameStatus.PAUSED) {
            throw new InvalidGameActionException("Only paused games can be resumed");
        }

        Optional<GameStateDto> latestVisibleState = gameSnapshotService.findLatestVisibleState(gameId, null);
        if (latestVisibleState.isEmpty()) {
            throw new ResourceNotFoundException("No snapshot found for game " + gameId);
        }
        GameStateDto snapshot = latestVisibleState.get();

        gameStateRestorer.restoreFromSnapshot(game, snapshot);
        game.setPauseReason(null);
        game.setPausedAt(null);
        game.setStateVersion(game.getStateVersion() + 1);

        GameStateDto resumedState = copyStateWithStatus(snapshot, GameStatus.ACTIVE, game.getStateVersion());
        gameSnapshotService.saveSnapshot(gameId, resumedState.stateVersion(), resumedState, null);
        gameRealtimeEventService.dispatchPublic(
                gameId,
                GameEventType.GAME_RESUMED,
                resumedState.stateVersion(),
                Map.of("resumedFromVersion", snapshot.stateVersion()));

        return resumedState;
    }

    private GameStateDto copyStateWithStatus(GameStateDto sourceState, GameStatus status, int stateVersion) {
        return sourceState.toBuilder()
                .status(status)
                .stateVersion(stateVersion)
                .updatedAt(Instant.now())
                .build();
    }
}
