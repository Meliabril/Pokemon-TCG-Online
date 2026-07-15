package ar.edu.utn.frc.tup.piii.services.game.engine.impl;




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
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.GamePauseUseCaseImpl;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.GameResumeUseCaseImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameLifecycleServicesUnitTest {

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameStateQueryService gameStateQueryService;

    @Mock
    private GameSnapshotService gameSnapshotService;

    @Mock
    private GameRealtimeEventService gameRealtimeEventService;

    @Mock
    private GameStateRestorer gameStateRestorer;

    @Test
    void shouldPauseActiveGameAndReturnPausedState() {
        GamePauseUseCaseImpl gamePauseService = new GamePauseUseCaseImpl(
                gameLookupService,
                gameStateQueryService,
                gameSnapshotService,
                gameRealtimeEventService);
        UUID gameId = UUID.randomUUID();
        Game game = game(gameId, GameStatus.ACTIVE, TurnPhase.ATTACK, 3, 4);
        GameStateDto resumableState = state(gameId, GameStatus.ACTIVE, TurnPhase.ATTACK, 3, 4);
        GameStateDto pausedState = state(gameId, GameStatus.PAUSED, TurnPhase.ATTACK, 3, 5);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameSnapshotService.findLatestVisibleState(gameId, null)).thenReturn(Optional.empty(), Optional.empty());
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(resumableState);

        GameStateDto result = gamePauseService.pause(gameId, "manual pause");

        assertThat(result.status()).isEqualTo(GameStatus.PAUSED);
        assertThat(result.stateVersion()).isEqualTo(5);
        assertThat(result.turn().currentPhase()).isEqualTo(TurnPhase.ATTACK);
        assertThat(game.getStatus()).isEqualTo(GameStatus.PAUSED);
        assertThat(game.getPauseReason()).isEqualTo("manual pause");
        assertThat(game.getPausedAt()).isNotNull();
        assertThat(game.getStateVersion()).isEqualTo(5);
        verify(gameSnapshotService).saveSnapshot(gameId, 4, resumableState, null);
        verify(gameRealtimeEventService).dispatchPublic(
                gameId,
                GameEventType.GAME_PAUSED,
                5,
                Map.of("reason", "manual pause"));
    }

    @Test
    void shouldRejectPauseWhenGameIsFinished() {
        GamePauseUseCaseImpl gamePauseService = new GamePauseUseCaseImpl(
                gameLookupService,
                gameStateQueryService,
                gameSnapshotService,
                gameRealtimeEventService);
        UUID gameId = UUID.randomUUID();
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game(gameId, GameStatus.FINISHED, TurnPhase.ATTACK, 3, 4));

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gamePauseService.pause(gameId, "manual pause");
            }
        })
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("cannot be paused");
    }

    @Test
    void shouldResumePausedGameFromLatestSnapshot() {
        GameResumeUseCaseImpl gameResumeService = new GameResumeUseCaseImpl(
                gameLookupService,
                gameStateQueryService,
                gameStateRestorer,
                gameSnapshotService,
                gameRealtimeEventService);
        UUID gameId = UUID.randomUUID();
        Game game = game(gameId, GameStatus.PAUSED, TurnPhase.MAIN, 8, 4);
        GameStateDto snapshot = state(gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 8, 4);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameSnapshotService.findLatestVisibleState(gameId, null)).thenReturn(Optional.of(snapshot));

        GameStateDto result = gameResumeService.resume(gameId);

        assertThat(result.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(result.stateVersion()).isEqualTo(5);
        assertThat(result.turn().currentPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(game.getPauseReason()).isNull();
        assertThat(game.getPausedAt()).isNull();
        assertThat(game.getStateVersion()).isEqualTo(5);
        verify(gameStateRestorer).restoreFromSnapshot(game, snapshot);
        verify(gameSnapshotService).saveSnapshot(
                org.mockito.ArgumentMatchers.eq(gameId),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.argThat(new org.mockito.ArgumentMatcher<GameStateDto>() {
                    @Override
                    public boolean matches(GameStateDto savedState) {
                        return savedState != null
                                && savedState.status() == GameStatus.ACTIVE
                                && savedState.stateVersion() == 5
                                && savedState.turn().currentPhase() == TurnPhase.MAIN;
                    }
                }),
                org.mockito.ArgumentMatchers.isNull());
        verify(gameRealtimeEventService).dispatchPublic(
                gameId,
                GameEventType.GAME_RESUMED,
                5,
                Map.of("resumedFromVersion", 4));
    }

    @Test
    void shouldRejectResumeWhenSnapshotIsMissing() {
        GameResumeUseCaseImpl gameResumeService = new GameResumeUseCaseImpl(
                gameLookupService,
                gameStateQueryService,
                gameStateRestorer,
                gameSnapshotService,
                gameRealtimeEventService);
        UUID gameId = UUID.randomUUID();
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game(gameId, GameStatus.PAUSED, TurnPhase.DRAW, 1, 2));
        when(gameSnapshotService.findLatestVisibleState(gameId, null)).thenReturn(Optional.empty());

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameResumeService.resume(gameId);
            }
        })
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(gameId.toString());
    }

    private Game game(UUID gameId, GameStatus status, TurnPhase phase, int turnNumber, int stateVersion) {
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(status);
        game.setCurrentPhase(phase);
        game.setTurnNumber(turnNumber);
        game.setStateVersion(stateVersion);
        game.setPausedAt(Instant.parse("2026-05-23T20:00:00Z"));
        game.setPauseReason("temporary disconnect");
        return game;
    }

    private GameStateDto state(UUID gameId, GameStatus status, TurnPhase phase, int turnNumber, int stateVersion) {
        return GameStateTestFactory.state(
                gameId,
                status,
                phase,
                turnNumber,
                stateVersion,
                UUID.randomUUID(),
                List.of(),
                Instant.parse("2026-05-23T20:00:00Z"));
    }
}
