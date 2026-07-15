package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateSyncUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameStateSyncUseCaseImpl implements GameStateSyncUseCase {

    private final GameLookupService gameLookupService;
    private final GameSnapshotService gameSnapshotService;
    private final GameStateService gameStateService;
    private final GameRealtimeEventService gameRealtimeEventService;

    @Override
    @Transactional
    public void syncState(UUID gameId, UUID viewerUserId) {
        gameLookupService.assertParticipant(gameId, viewerUserId);

        Optional<GameStateDto> latestVisibleState = gameSnapshotService.findLatestVisibleState(gameId, viewerUserId);
        GameStateDto visibleState;
        if (latestVisibleState.isPresent()) {
            visibleState = latestVisibleState.get();
        } else {
            visibleState = buildCurrentVisibleState(gameId, viewerUserId);
        }

        gameRealtimeEventService.dispatchStateSync(visibleState, viewerUserId);
    }

    private GameStateDto buildCurrentVisibleState(UUID gameId, UUID viewerUserId) {
        Game game = gameLookupService.getRequiredGame(gameId);
        return gameStateService.buildVisibleState(game, viewerUserId);
    }
}
