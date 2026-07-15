package ar.edu.utn.frc.tup.piii.services.game.turn.impl;

import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.AvailableActionsFactoryImpl;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.attack.BetweenTurnsResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnServiceImplTest {

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameParticipantStateService gameParticipantStateService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private BetweenTurnsResolutionService betweenTurnsResolutionService;

    @Mock
    private GameStateQueryService gameStateQueryService;

    @Mock
    private AbilityUsageTracker abilityUsageTracker;

    @Test
    void shouldResetTurnFlagsWhenEndingMainPhaseTurnToOpponentDrawPhase() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        GameEventDto phaseEvent = event(gameId, GameEventType.PHASE_CHANGED);
        GameEventDto drawPhaseEvent = event(gameId, GameEventType.PHASE_CHANGED);
        GameEventDto turnStartedEvent = event(gameId, GameEventType.TURN_STARTED);
        TurnServiceImpl service = service();

        when(gameParticipantStateService.findOpponentUserId(gameId, actorUserId)).thenReturn(opponentUserId);
        when(betweenTurnsResolutionService.resolveBetweenTurns(gameId, actorUserId, opponentUserId, 3, 4, 8))
                .thenReturn(new BetweenTurnsResolutionService.BetweenTurnsResolutionResult(
                        List.of(),
                        Map.of(actorUserId, List.of(), opponentUserId, List.of(SpecialConditionType.POISONED)),
                        Map.of(actorUserId, 1, opponentUserId, 2),
                        false,
                        false,
                        null));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.PHASE_CHANGED), eq(8), anyMap()))
                .thenReturn(phaseEvent, drawPhaseEvent);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TURN_STARTED), eq(8), anyMap()))
                .thenReturn(turnStartedEvent);

        GameActionExecutionResult result = service.endTurn(context(
                gameId,
                actorUserId,
                state(gameId, actorUserId, opponentUserId, TurnPhase.MAIN, true, true, true)));

        assertThat(result.gameState().turn().currentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(result.gameState().turn().activePlayerId()).isEqualTo(opponentUserId);
        assertThat(result.gameState().turn().turnNumber()).isEqualTo(4);
        assertThat(result.gameState().turn().turnNumber()).isEqualTo(4);
        assertThat(result.gameState().turn().energyAttachedThisTurn()).isFalse();
        assertThat(result.gameState().turn().supporterPlayedThisTurn()).isFalse();
        assertThat(result.gameState().turn().retreatedThisTurn()).isFalse();
        assertThat(result.gameState().resolution().resolutionType()).isNull();
        assertThat(result.gameState().actions().availableActions()).containsExactly(GameActionType.DRAW_CARD);
        assertThat(result.emittedEvents()).containsExactly(phaseEvent, drawPhaseEvent, turnStartedEvent);
    }

    @Test
    void shouldFinishGameWhenMandatoryDrawFindsEmptyDeck() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        GameEventDto gameFinishedEvent = event(gameId, GameEventType.GAME_FINISHED);
        TurnServiceImpl service = service();

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of());
        when(gameParticipantStateService.findOpponentUserId(gameId, actorUserId)).thenReturn(opponentUserId);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.GAME_FINISHED), eq(8), anyMap()))
                .thenReturn(gameFinishedEvent);

        GameActionExecutionResult result = service.drawCard(context(
                gameId,
                actorUserId,
                state(gameId, actorUserId, opponentUserId, TurnPhase.DRAW, false, false, false)));

        assertThat(result.gameState().status()).isEqualTo(GameStatus.FINISHED);
        assertThat(result.gameState().actions().availableActions()).isEmpty();
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getWinnerPlayerId()).isEqualTo(opponentUserId);
        assertThat(result.emittedEvents()).containsExactly(gameFinishedEvent);
    }

    @Test
    void shouldCancelPendingResolutionAndAdvanceTurnWhenTimeoutExpiresInMainPhase() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        GameEventDto betweenTurnsEvent = event(gameId, GameEventType.PHASE_CHANGED);
        GameEventDto drawPhaseEvent = event(gameId, GameEventType.PHASE_CHANGED);
        GameEventDto turnStartedEvent = event(gameId, GameEventType.TURN_STARTED);
        TurnServiceImpl service = service();

        when(gameParticipantStateService.findOpponentUserId(gameId, actorUserId)).thenReturn(opponentUserId);
        when(betweenTurnsResolutionService.resolveBetweenTurns(gameId, actorUserId, opponentUserId, 3, 4, 8))
                .thenReturn(new BetweenTurnsResolutionService.BetweenTurnsResolutionResult(
                        List.of(),
                        Map.of(actorUserId, List.of(), opponentUserId, List.of()),
                        Map.of(actorUserId, 1, opponentUserId, 1),
                        false,
                        false,
                        null));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.PHASE_CHANGED), eq(8), anyMap()))
                .thenReturn(betweenTurnsEvent, drawPhaseEvent);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TURN_STARTED), eq(8), anyMap()))
                .thenReturn(turnStartedEvent);

        GameStateDto timedOutState = state(gameId, actorUserId, opponentUserId, TurnPhase.MAIN, true, false, false)
                .toBuilder()
                .resolution(ResolutionStateDto.builder()
                        .resolutionType(ResolutionStateDto.ATTACK_CHOICE_REQUIRED)
                        .pendingChoicePlayerId(actorUserId)
                        .pendingChoiceType("SELECT_DECK_CARD_AND_ATTACH_TO_SELF")
                        .pendingChoicePayload(Map.of("cards", List.of(Map.of("cardInstanceId", UUID.randomUUID().toString()))))
                        .nextActivePlayerId(opponentUserId)
                        .nextTurnNumber(4)
                        .turnEndingPlayerId(actorUserId)
                        .build())
                .build();

        GameActionExecutionResult result = service.expireTimedOutTurn(context(gameId, actorUserId, timedOutState));

        assertThat(result.gameState().resolution().resolutionType()).isNull();
        assertThat(result.gameState().turn().activePlayerId()).isEqualTo(opponentUserId);
        assertThat(result.gameState().turn().currentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(result.gameState().actions().availableActions()).containsExactly(GameActionType.DRAW_CARD);
        assertThat(result.emittedEvents()).containsExactly(
                betweenTurnsEvent,
                drawPhaseEvent,
                turnStartedEvent);
        verify(gameCardInstanceStateService, never())
                .findByGameIdAndOwnerUserIdAndZone(gameId, opponentUserId, CardZone.DECK);
    }

    @Test
    void shouldDrawOnlyTimedOutPlayerBeforePassingTurnWhenTimeoutExpiresInDrawPhase() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID drawnCardId = UUID.randomUUID();
        UUID drawnCardInstanceId = UUID.randomUUID();
        GameCardInstance drawnCard = new GameCardInstance();
        drawnCard.setId(drawnCardInstanceId);
        drawnCard.setCardId(drawnCardId);
        drawnCard.setZone(CardZone.DECK);
        GameEventDto cardDrawnPublicEvent = event(gameId, GameEventType.CARD_DRAWN);
        GameEventDto cardDrawnPrivateEvent = event(gameId, GameEventType.CARD_DRAWN);
        GameEventDto mainPhaseEvent = event(gameId, GameEventType.PHASE_CHANGED);
        GameEventDto betweenTurnsEvent = event(gameId, GameEventType.PHASE_CHANGED);
        GameEventDto drawPhaseEvent = event(gameId, GameEventType.PHASE_CHANGED);
        GameEventDto turnStartedEvent = event(gameId, GameEventType.TURN_STARTED);
        TurnServiceImpl service = service();

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(drawnCard));
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.HAND)).thenReturn(3);
        when(gameParticipantStateService.findOpponentUserId(gameId, actorUserId)).thenReturn(opponentUserId);
        when(betweenTurnsResolutionService.resolveBetweenTurns(gameId, actorUserId, opponentUserId, 3, 4, 9))
                .thenReturn(new BetweenTurnsResolutionService.BetweenTurnsResolutionResult(
                        List.of(),
                        Map.of(actorUserId, List.of(), opponentUserId, List.of()),
                        Map.of(actorUserId, 1, opponentUserId, 1),
                        false,
                        false,
                        null));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), eq(8), anyMap()))
                .thenReturn(cardDrawnPublicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), eq(8), anyMap(), eq(actorUserId)))
                .thenReturn(cardDrawnPrivateEvent);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.PHASE_CHANGED), eq(8), anyMap()))
                .thenReturn(mainPhaseEvent);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.PHASE_CHANGED), eq(9), anyMap()))
                .thenReturn(betweenTurnsEvent, drawPhaseEvent);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TURN_STARTED), eq(9), anyMap()))
                .thenReturn(turnStartedEvent);

        GameActionExecutionResult result = service.expireTimedOutTurn(context(
                gameId,
                actorUserId,
                state(gameId, actorUserId, opponentUserId, TurnPhase.DRAW, false, false, false)));

        assertThat(result.gameState().turn().activePlayerId()).isEqualTo(opponentUserId);
        assertThat(result.gameState().turn().currentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(result.gameState().actions().availableActions()).containsExactly(GameActionType.DRAW_CARD);
        assertThat(result.gameState().players().get(actorUserId).cardInstanceIdsInHand()).contains(drawnCardInstanceId);
        assertThat(result.emittedEvents()).containsExactly(
                cardDrawnPublicEvent,
                cardDrawnPrivateEvent,
                mainPhaseEvent,
                betweenTurnsEvent,
                drawPhaseEvent,
                turnStartedEvent);
        verify(gameCardInstanceStateService).save(drawnCard);
        verify(gameCardInstanceStateService)
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);
        verify(gameCardInstanceStateService, never())
                .findByGameIdAndOwnerUserIdAndZone(gameId, opponentUserId, CardZone.DECK);
    }

    @Test
    void shouldFinishGameWithoutEndingTurnWhenTimeoutAutoDrawFindsEmptyDeck() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        GameEventDto gameFinishedEvent = event(gameId, GameEventType.GAME_FINISHED);
        TurnServiceImpl service = service();

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of());
        when(gameParticipantStateService.findOpponentUserId(gameId, actorUserId)).thenReturn(opponentUserId);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.GAME_FINISHED), eq(8), anyMap()))
                .thenReturn(gameFinishedEvent);

        GameActionExecutionResult result = service.expireTimedOutTurn(context(
                gameId,
                actorUserId,
                state(gameId, actorUserId, opponentUserId, TurnPhase.DRAW, false, false, false)));

        assertThat(result.gameState().status()).isEqualTo(GameStatus.FINISHED);
        assertThat(result.emittedEvents()).containsExactly(gameFinishedEvent);
        verify(betweenTurnsResolutionService, never())
                .resolveBetweenTurns(gameId, actorUserId, opponentUserId, 3, 4, 8);
    }

    private TurnServiceImpl service() {
        return new TurnServiceImpl(
                gameLookupService,
                gameParticipantStateService,
                gameCardInstanceStateService,
                gameEventFactory,
                betweenTurnsResolutionService,
                new AvailableActionsFactoryImpl(),
                gameStateQueryService,
                abilityUsageTracker);
    }

    private GameActionContext context(UUID gameId, UUID actorUserId, GameStateDto state) {
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                expectedActionForPhase(state),
                state.stateVersion(),
                Map.<String, Object>of());
        return new GameActionContext(gameId, actorUserId, request, state);
    }

    private GameActionType expectedActionForPhase(GameStateDto state) {
        if (state.turn().currentPhase() == TurnPhase.DRAW) {
            return GameActionType.DRAW_CARD;
        }
        return GameActionType.END_TURN;
    }

    private GameStateDto state(
            UUID gameId,
            UUID actorUserId,
            UUID opponentUserId,
            TurnPhase phase,
            boolean energyAttachedThisTurn,
            boolean supporterPlayedThisTurn,
            boolean retreatedThisTurn) {
        return GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                phase,
                3,
                7,
                actorUserId,
                List.of(actorUserId, opponentUserId),
                energyAttachedThisTurn,
                supporterPlayedThisTurn,
                retreatedThisTurn,
                Map.of(actorUserId, 1, opponentUserId, 2),
                Map.of(actorUserId, List.of(), opponentUserId, List.of()),
                3,
                actorUserId,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-24T12:00:00Z"));
    }

    private GameEventDto event(UUID gameId, GameEventType eventType) {
        return new GameEventDto(
                UUID.randomUUID(),
                gameId,
                eventType,
                8,
                false,
                Instant.parse("2026-05-24T12:00:01Z"),
                Map.of());
    }
}
