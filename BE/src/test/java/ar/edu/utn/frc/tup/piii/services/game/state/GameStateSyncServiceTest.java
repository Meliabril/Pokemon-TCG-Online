package ar.edu.utn.frc.tup.piii.services.game.state;




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
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameStateSyncUseCaseImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameStateSyncServiceTest {

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameSnapshotService gameSnapshotService;

    @Mock
    private GameStateService gameStateService;

    @Mock
    private GameRealtimeEventService gameRealtimeEventService;

    @Test
    void shouldDispatchPrivateStateSyncUsingLatestVisibleSnapshot() {
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        GameStateDto snapshot = state(gameId, viewerUserId, 7, TurnPhase.MAIN);
        GameStateSyncUseCase service = new GameStateSyncUseCaseImpl(
                gameLookupService,
                gameSnapshotService,
                gameStateService,
                gameRealtimeEventService);

        when(gameSnapshotService.findLatestVisibleState(gameId, viewerUserId)).thenReturn(Optional.of(snapshot));

        service.syncState(gameId, viewerUserId);

        verify(gameLookupService).assertParticipant(gameId, viewerUserId);
        verify(gameSnapshotService).findLatestVisibleState(gameId, viewerUserId);
        verify(gameRealtimeEventService).dispatchStateSync(snapshot, viewerUserId);
        verify(gameLookupService, never()).getRequiredGame(gameId);
        verify(gameStateService, never()).buildVisibleState(any(Game.class));
    }

    @Test
    void shouldBuildVisibleStateWhenSnapshotIsMissing() {
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        GameStateDto visibleState = state(gameId, viewerUserId, 11, TurnPhase.DRAW);
        GameStateSyncUseCase service = new GameStateSyncUseCaseImpl(
                gameLookupService,
                gameSnapshotService,
                gameStateService,
                gameRealtimeEventService);

        when(gameSnapshotService.findLatestVisibleState(gameId, viewerUserId)).thenReturn(Optional.empty());
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameStateService.buildVisibleState(game, viewerUserId)).thenReturn(visibleState);

        service.syncState(gameId, viewerUserId);

        verify(gameLookupService).assertParticipant(gameId, viewerUserId);
        verify(gameLookupService).getRequiredGame(gameId);
        verify(gameStateService).buildVisibleState(game, viewerUserId);
        verify(gameRealtimeEventService).dispatchStateSync(visibleState, viewerUserId);
    }

    @Test
    void shouldStopWhenViewerIsNotParticipant() {
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        GameStateSyncUseCase service = new GameStateSyncUseCaseImpl(
                gameLookupService,
                gameSnapshotService,
                gameStateService,
                gameRealtimeEventService);

        doThrow(new ar.edu.utn.frc.tup.piii.exceptions.ForbiddenActionException("forbidden"))
                .when(gameLookupService)
                .assertParticipant(gameId, viewerUserId);

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                service.syncState(gameId, viewerUserId);
            }
        })
                .isInstanceOf(ar.edu.utn.frc.tup.piii.exceptions.ForbiddenActionException.class)
                .hasMessageContaining("forbidden");

        verify(gameSnapshotService, never()).findLatestVisibleState(gameId, viewerUserId);
        verify(gameRealtimeEventService, never()).dispatchStateSync(any(GameStateDto.class), any(UUID.class));
    }

    private GameStateDto state(UUID gameId, UUID viewerUserId, int stateVersion, TurnPhase phase) {
        return GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                phase,
                stateVersion,
                stateVersion,
                viewerUserId,
                List.of(viewerUserId, UUID.randomUUID()),
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-24T10:00:00Z"));
    }
}
