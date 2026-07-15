package ar.edu.utn.frc.tup.piii.services.game.turn.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackChoiceService;
import ar.edu.utn.frc.tup.piii.services.game.attack.impl.OpponentCoinTailsHandDiscardAttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionResultPublisher;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.turn.TurnService;
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnTimeoutServiceImplTest {

    @Mock private GameRepository gameRepository;
    @Mock private GameParticipantStateService gameParticipantStateService;
    @Mock private GameStateQueryService gameStateQueryService;
    @Mock private GameSnapshotService gameSnapshotService;
    @Mock private TurnService turnService;
    @Mock private AttackChoiceService attackChoiceService;
    @Mock private GameActionResultPublisher gameActionResultPublisher;
    @Mock private GameEventFactory gameEventFactory;
    @Mock private GameRealtimeEventService gameRealtimeEventService;

    private TurnTimeoutServiceImpl newService() {
        return new TurnTimeoutServiceImpl(
                gameRepository,
                gameParticipantStateService,
                gameStateQueryService,
                gameSnapshotService,
                turnService,
                attackChoiceService,
                gameActionResultPublisher,
                gameEventFactory,
                gameRealtimeEventService);
    }

    @Test
    void shouldForceEndTurnAndIncrementCounterWhenSingleTimeout() {
        UUID gameId = UUID.randomUUID();
        UUID timedOutUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        Game game = activeGame(gameId, timedOutUserId, Instant.now().minus(java.time.Duration.ofMinutes(3)));
        GameParticipant timedOutParticipant = participant(gameId, timedOutUserId, 0);
        GameStateDto currentState = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 1, 5, timedOutUserId,
                List.of(timedOutUserId, opponentUserId), List.of(), Instant.now());

        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));
        when(gameParticipantStateService.findOpponentUserId(gameId, timedOutUserId)).thenReturn(opponentUserId);
        when(gameParticipantStateService.findByGameIdAndUserId(gameId, timedOutUserId))
                .thenReturn(Optional.of(timedOutParticipant));
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(currentState);
        when(turnService.expireTimedOutTurn(ArgumentMatchers.any(GameActionContext.class)))
                .thenReturn(new GameActionExecutionResult(null, List.of()));
        when(gameActionResultPublisher.publish(
                ArgumentMatchers.eq(gameId),
                ArgumentMatchers.eq(game),
                ArgumentMatchers.eq(currentState),
                ArgumentMatchers.any(GameActionExecutionResult.class),
                ArgumentMatchers.eq(timedOutUserId),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.anyString()))
                .thenReturn(new GameActionResponseDto(true, "ok", 6, Map.of()));

        newService().applyTimeoutIfDue(gameId);

        assertThat(timedOutParticipant.getConsecutiveTimeouts()).isEqualTo(1);
        verify(gameParticipantStateService).save(timedOutParticipant);
        verify(gameParticipantStateService).resetConsecutiveTimeouts(gameId, opponentUserId);

        ArgumentCaptor<GameActionContext> contextCaptor = ArgumentCaptor.forClass(GameActionContext.class);
        verify(turnService).expireTimedOutTurn(contextCaptor.capture());
        assertThat(contextCaptor.getValue().actorUserId()).isEqualTo(timedOutUserId);
        assertThat(contextCaptor.getValue().request().actionType()).isEqualTo(GameActionType.END_TURN);
        assertThat(contextCaptor.getValue().request().payload()).containsEntry("reason", "TURN_TIMEOUT");

        verify(gameRepository, never()).save(ArgumentMatchers.any(Game.class));
        verify(gameRealtimeEventService, never()).dispatch(ArgumentMatchers.any(GameEventDto.class));
    }

    @Test
    void shouldResolvePendingAttackChoiceThroughBackendWhenDecisionOwnerTimesOut() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        Game game = activeGame(gameId, attackerUserId, Instant.now().minus(java.time.Duration.ofMinutes(3)));
        GameParticipant defenderParticipant = participant(gameId, defenderUserId, 0);
        GameStateDto currentState = GameStateTestFactory.state(
                        gameId, GameStatus.ACTIVE, TurnPhase.BETWEEN_TURNS, 1, 5, attackerUserId,
                        List.of(attackerUserId, defenderUserId), List.of(), Instant.now())
                .toBuilder()
                .resolution(ResolutionStateDto.builder()
                        .resolutionType(ResolutionStateDto.ATTACK_CHOICE_REQUIRED)
                        .pendingChoicePlayerId(defenderUserId)
                        .pendingChoiceType(OpponentCoinTailsHandDiscardAttackEffect.SELECT_HAND_CARDS_TO_DISCARD)
                        .pendingChoicePayload(Map.of(
                                "discardCount", 2,
                                "cards", List.of(Map.of("cardInstanceId", UUID.randomUUID().toString()))))
                        .nextActivePlayerId(defenderUserId)
                        .nextTurnNumber(2)
                        .turnEndingPlayerId(attackerUserId)
                        .build())
                .build();
        GameActionExecutionResult timeoutResult = new GameActionExecutionResult(
                currentState.toBuilder().resolution(null).build(),
                List.of());

        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));
        when(gameParticipantStateService.findOpponentUserId(gameId, defenderUserId)).thenReturn(attackerUserId);
        when(gameParticipantStateService.findByGameIdAndUserId(gameId, defenderUserId))
                .thenReturn(Optional.of(defenderParticipant));
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(currentState);
        when(attackChoiceService.resolveAttackChoiceForTimeout(ArgumentMatchers.any(GameActionContext.class)))
                .thenReturn(timeoutResult);
        when(gameActionResultPublisher.publish(
                ArgumentMatchers.eq(gameId),
                ArgumentMatchers.eq(game),
                ArgumentMatchers.eq(currentState),
                ArgumentMatchers.any(GameActionExecutionResult.class),
                ArgumentMatchers.eq(defenderUserId),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.anyString()))
                .thenReturn(new GameActionResponseDto(true, "ok", 6, Map.of()));

        newService().applyTimeoutIfDue(gameId);

        assertThat(defenderParticipant.getConsecutiveTimeouts()).isEqualTo(1);

        ArgumentCaptor<GameActionContext> contextCaptor = ArgumentCaptor.forClass(GameActionContext.class);
        verify(attackChoiceService).resolveAttackChoiceForTimeout(contextCaptor.capture());
        assertThat(contextCaptor.getValue().actorUserId()).isEqualTo(defenderUserId);
        assertThat(contextCaptor.getValue().request().actionType()).isEqualTo(GameActionType.RESOLVE_ATTACK_CHOICE);
        assertThat(contextCaptor.getValue().request().payload()).containsEntry("reason", "TURN_TIMEOUT");

        verify(gameActionResultPublisher).publish(
                ArgumentMatchers.eq(gameId),
                ArgumentMatchers.eq(game),
                ArgumentMatchers.eq(currentState),
                ArgumentMatchers.eq(timeoutResult),
                ArgumentMatchers.eq(defenderUserId),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(GameActionType.RESOLVE_ATTACK_CHOICE.name()));
    }

    @Test
    void shouldFinishGameInFavorOfOpponentOnSecondConsecutiveTimeout() {
        UUID gameId = UUID.randomUUID();
        UUID timedOutUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        Game game = activeGame(gameId, timedOutUserId, Instant.now().minus(java.time.Duration.ofMinutes(3)));
        GameParticipant timedOutParticipant = participant(gameId, timedOutUserId, 1);
        GameEventDto finishedEvent = new GameEventDto(
                UUID.randomUUID(), gameId, GameEventType.GAME_FINISHED, 1, false, Instant.now(), Map.of());

        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));
        when(gameParticipantStateService.findOpponentUserId(gameId, timedOutUserId)).thenReturn(opponentUserId);
        when(gameParticipantStateService.findByGameIdAndUserId(gameId, timedOutUserId))
                .thenReturn(Optional.of(timedOutParticipant));
        when(gameParticipantStateService.findPlayerIds(gameId)).thenReturn(List.of(timedOutUserId, opponentUserId));
        GameStateDto finishedState = GameStateTestFactory.state(
                gameId, GameStatus.FINISHED, null, 1, 1, null, List.of(), Instant.now());
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(finishedState);
        when(gameStateQueryService.buildVisibleState(ArgumentMatchers.eq(game), ArgumentMatchers.any(UUID.class)))
                .thenReturn(finishedState);
        when(gameEventFactory.publicEvent(
                ArgumentMatchers.eq(gameId),
                ArgumentMatchers.eq(GameEventType.GAME_FINISHED),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyMap()))
                .thenReturn(finishedEvent);

        newService().applyTimeoutIfDue(gameId);

        assertThat(timedOutParticipant.getConsecutiveTimeouts()).isEqualTo(2);
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getWinnerPlayerId()).isEqualTo(opponentUserId);
        verify(gameRepository).save(game);
        verify(gameSnapshotService).saveSnapshot(gameId, 6, finishedState, opponentUserId);
        verify(gameRealtimeEventService).dispatch(finishedEvent);
        verify(gameRealtimeEventService, org.mockito.Mockito.times(2))
                .dispatchStateSync(ArgumentMatchers.any(GameStateDto.class), ArgumentMatchers.any(UUID.class));
        verify(turnService, never()).expireTimedOutTurn(ArgumentMatchers.any(GameActionContext.class));
    }

    @Test
    void shouldFinishGameInFavorOfOpponentWhenPendingPromotionTimesOut() {
        UUID gameId = UUID.randomUUID();
        UUID playerWaitingForPromotionId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        // The active player tracked on the Game entity can be stale while a promotion is
        // pending; the player who actually owes the action is resolution.playerToPromoteId(),
        // not game.getActivePlayerId(). Use different values here to prove the fix reads
        // the right one.
        Game game = activeGame(gameId, opponentUserId, Instant.now().minus(java.time.Duration.ofMinutes(3)));
        GameEventDto finishedEvent = new GameEventDto(
                UUID.randomUUID(), gameId, GameEventType.GAME_FINISHED, 1, false, Instant.now(), Map.of());

        ResolutionStateDto pendingPromotion = ResolutionStateDto.builder()
                .resolutionType(ResolutionStateDto.PROMOTION_REQUIRED)
                .playerToPromoteId(playerWaitingForPromotionId)
                .build();
        GameStateDto pendingPromotionState = GameStateTestFactory.state(
                        gameId, GameStatus.ACTIVE, TurnPhase.BETWEEN_TURNS, 1, 5, opponentUserId,
                        List.of(playerWaitingForPromotionId, opponentUserId), List.of(), Instant.now())
                .toBuilder()
                .resolution(pendingPromotion)
                .build();
        GameStateDto finishedState = GameStateTestFactory.state(
                gameId, GameStatus.FINISHED, null, 1, 6, null, List.of(), Instant.now());

        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));
        when(gameParticipantStateService.findOpponentUserId(gameId, playerWaitingForPromotionId))
                .thenReturn(opponentUserId);
        when(gameParticipantStateService.findPlayerIds(gameId))
                .thenReturn(List.of(playerWaitingForPromotionId, opponentUserId));
        when(gameStateQueryService.buildVisibleState(game))
                .thenReturn(pendingPromotionState)
                .thenReturn(finishedState);
        when(gameStateQueryService.buildVisibleState(ArgumentMatchers.eq(game), ArgumentMatchers.any(UUID.class)))
                .thenReturn(finishedState);
        when(gameEventFactory.publicEvent(
                ArgumentMatchers.eq(gameId),
                ArgumentMatchers.eq(GameEventType.GAME_FINISHED),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyMap()))
                .thenReturn(finishedEvent);

        newService().applyTimeoutIfDue(gameId);

        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getWinnerPlayerId()).isEqualTo(opponentUserId);
        verify(gameRepository).save(game);
        verify(gameSnapshotService).saveSnapshot(gameId, 6, finishedState, opponentUserId);
        verify(gameEventFactory).publicEvent(
                gameId,
                GameEventType.GAME_FINISHED,
                6,
                Map.of("winnerPlayerId", opponentUserId.toString(), "reason", "PROMOTION_TIMEOUT"));
        verify(gameRealtimeEventService).dispatch(finishedEvent);
        verify(turnService, never()).expireTimedOutTurn(ArgumentMatchers.any(GameActionContext.class));
        verify(gameParticipantStateService, never()).save(ArgumentMatchers.any(GameParticipant.class));
        verify(gameParticipantStateService, never())
                .resetConsecutiveTimeouts(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void shouldDoNothingWhenTurnHasNotTimedOutYet() {
        UUID gameId = UUID.randomUUID();
        UUID activePlayerId = UUID.randomUUID();
        Game game = activeGame(gameId, activePlayerId, Instant.now());

        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));

        newService().applyTimeoutIfDue(gameId);

        verify(gameParticipantStateService, never()).findOpponentUserId(ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(gameRepository, never()).save(ArgumentMatchers.any(Game.class));
        verify(turnService, never()).expireTimedOutTurn(ArgumentMatchers.any(GameActionContext.class));
    }

    private Game activeGame(UUID gameId, UUID activePlayerId, Instant turnStartedAt) {
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(1);
        game.setStateVersion(5);
        game.setActivePlayerId(activePlayerId);
        game.setTurnStartedAt(turnStartedAt);
        return game;
    }

    private GameParticipant participant(UUID gameId, UUID userId, int consecutiveTimeouts) {
        GameParticipant participant = new GameParticipant();
        participant.setId(UUID.randomUUID());
        participant.setUserId(userId);
        participant.setConsecutiveTimeouts(consecutiveTimeouts);
        return participant;
    }

}
