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
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardPlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.GameServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.RuleValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceImplTest {

    @Mock private GameLookupService gameLookupService;
    @Mock private GameDataService gameDataService;
    @Mock private GameStateQueryService gameStateQueryService;
    @Mock private GameStateRestorer gameStateRestorer;
    @Mock private GameSnapshotService gameSnapshotService;
    @Mock private RuleValidator ruleValidator;
    @Mock private GameActionExecutor gameActionExecutor;
    @Mock private GameActionLogger gameActionLogger;
    @Mock private GameRealtimeEventService gameRealtimeEventService;
    @Mock private GameParticipantStateService gameParticipantStateService;

    private GameServiceImpl newService() {
        GameActionResultPublisher publisher = new GameActionResultPublisherImpl(
                gameStateQueryService,
                gameStateRestorer,
                gameSnapshotService,
                gameActionLogger,
                gameRealtimeEventService);
        return new GameServiceImpl(
                gameLookupService,
                gameDataService,
                gameStateQueryService,
                gameSnapshotService,
                ruleValidator,
                gameActionExecutor,
                publisher,
                gameParticipantStateService);
    }

    @Test
    void shouldDispatchEmittedEventsThroughRealtimeBoundary() {
        GameServiceImpl service = newService();

        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        game.setStateVersion(2);
        GameStateDto currentState = state(gameId, 2, List.of(actorUserId));
        GameEventDto emittedEvent = new GameEventDto(
                UUID.randomUUID(),
                gameId,
                GameEventType.GAME_STARTED,
                3,
                false,
                Instant.parse("2026-05-24T00:00:00Z"),
                Map.of("phase", "DRAW"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.DRAW_CARD,
                2,
                Map.of("card", "pikachu"));

        when(gameDataService.getRequiredGameForUpdate(gameId)).thenReturn(game);
        when(gameSnapshotService.findLatestVisibleState(gameId, actorUserId)).thenReturn(Optional.of(currentState));
        when(gameActionExecutor.execute(org.mockito.ArgumentMatchers.any(GameActionContext.class)))
                .thenReturn(new GameActionExecutionResult(null, List.of(emittedEvent)));
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(state(gameId, 3, List.of(actorUserId)));
        when(gameStateQueryService.buildVisibleState(game, actorUserId)).thenReturn(state(gameId, 3, List.of(actorUserId)));

        GameActionResponseDto response = service.executeAction(gameId, actorUserId, request);

        assertThat(response.success()).isTrue();
        assertThat(response.newStateVersion()).isEqualTo(3);
        verify(gameRealtimeEventService).dispatch(emittedEvent);
        verify(gameRealtimeEventService).dispatchStateSync(org.mockito.ArgumentMatchers.any(GameStateDto.class), org.mockito.ArgumentMatchers.eq(actorUserId));
    }

    @Test
    void shouldSaveSnapshotWithRuntimeBoardPlayersAndDispatchStateSyncForEachPlayer() {
        GameServiceImpl service = newService();

        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID clientActionId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        game.setStateVersion(2);
        Map<UUID, BoardPlayerStateDto> staleBoardPlayers = Map.of(
                actorUserId,
                BoardPlayerStateDto.empty(actorUserId));
        Map<UUID, BoardPlayerStateDto> runtimeBoardPlayers = Map.of(
                actorUserId,
                BoardPlayerStateDto.empty(actorUserId),
                opponentUserId,
                BoardPlayerStateDto.empty(opponentUserId));
        GameStateDto currentState = state(gameId, 2, List.of(actorUserId, opponentUserId)).toBuilder()
                .boardPlayers(staleBoardPlayers)
                .build();
        GameStateDto runtimeState = state(gameId, 3, List.of(actorUserId, opponentUserId)).toBuilder()
                .boardPlayers(runtimeBoardPlayers)
                .build();
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                clientActionId,
                GameActionType.DRAW_CARD,
                2,
                Map.of());

        when(gameDataService.getRequiredGameForUpdate(gameId)).thenReturn(game);
        when(gameSnapshotService.findLatestVisibleState(gameId, actorUserId)).thenReturn(Optional.of(currentState));
        when(gameActionExecutor.execute(org.mockito.ArgumentMatchers.any(GameActionContext.class)))
                .thenReturn(new GameActionExecutionResult(null, List.of()));
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(runtimeState);
        when(gameStateQueryService.buildVisibleState(game, actorUserId)).thenReturn(state(gameId, 3, List.of(actorUserId, opponentUserId)));
        when(gameStateQueryService.buildVisibleState(game, opponentUserId)).thenReturn(state(gameId, 3, List.of(actorUserId, opponentUserId)));

        service.executeAction(gameId, actorUserId, request);

        ArgumentCaptor<GameStateDto> stateCaptor = ArgumentCaptor.forClass(GameStateDto.class);
        verify(gameSnapshotService).saveSnapshot(
                org.mockito.ArgumentMatchers.eq(gameId),
                org.mockito.ArgumentMatchers.eq(3),
                stateCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(actorUserId));
        assertThat(stateCaptor.getValue().boardPlayers()).isEqualTo(runtimeBoardPlayers);
        assertThat(stateCaptor.getValue().boardPlayers()).isNotEqualTo(staleBoardPlayers);

        InOrder inOrder = inOrder(gameSnapshotService, gameRealtimeEventService);
        inOrder.verify(gameSnapshotService).saveSnapshot(
                org.mockito.ArgumentMatchers.eq(gameId),
                org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.any(GameStateDto.class),
                org.mockito.ArgumentMatchers.eq(actorUserId));
        inOrder.verify(gameRealtimeEventService).dispatchStateSync(
                org.mockito.ArgumentMatchers.any(GameStateDto.class),
                org.mockito.ArgumentMatchers.eq(actorUserId));
        inOrder.verify(gameRealtimeEventService).dispatchStateSync(
                org.mockito.ArgumentMatchers.any(GameStateDto.class),
                org.mockito.ArgumentMatchers.eq(opponentUserId));
    }

    @Test
    void shouldNotDispatchExtraStateSyncWhenHandlerAlreadyEmitsOne() {
        GameServiceImpl service = newService();

        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        game.setStateVersion(2);
        GameStateDto currentState = state(gameId, 2, List.of(actorUserId));
        GameEventDto stateSyncEvent = new GameEventDto(
                UUID.randomUUID(),
                gameId,
                GameEventType.STATE_SYNC,
                3,
                true,
                Instant.parse("2026-05-24T00:00:00Z"),
                Map.of("stateVersion", 3));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.START_GAME,
                2,
                Map.of());

        when(gameDataService.getRequiredGameForUpdate(gameId)).thenReturn(game);
        when(gameSnapshotService.findLatestVisibleState(gameId, actorUserId)).thenReturn(Optional.of(currentState));
        when(gameActionExecutor.execute(org.mockito.ArgumentMatchers.any(GameActionContext.class)))
                .thenReturn(new GameActionExecutionResult(null, List.of(stateSyncEvent)));
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(state(gameId, 3, List.of(actorUserId)));

        service.executeAction(gameId, actorUserId, request);

        verify(gameRealtimeEventService).dispatch(stateSyncEvent);
        verify(gameRealtimeEventService, never()).dispatchStateSync(
                org.mockito.ArgumentMatchers.any(GameStateDto.class),
                org.mockito.ArgumentMatchers.any(UUID.class));
    }

    private GameStateDto state(UUID gameId, int version, List<UUID> playerIds) {
        return GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.DRAW,
                1,
                version,
                UUID.randomUUID(),
                playerIds,
                List.of(),
                Instant.parse("2026-05-24T00:00:00Z"));
    }
}
