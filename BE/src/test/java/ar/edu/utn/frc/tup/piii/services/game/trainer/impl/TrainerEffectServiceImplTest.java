package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;




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
import ar.edu.utn.frc.tup.piii.services.game.ability.*;
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
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.trainer.SupporterLockService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerEffectServiceImplTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private CardService cardService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private SupporterLockService supporterLockService;

    @Mock
    private PassiveAbilityService passiveAbilityService;

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameStateQueryService gameStateQueryService;

    @Test
    void shouldExecuteItemTrainerDiscardCardAndKeepSupporterFlagFalse() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GameCardInstance trainerInstance = handCardInstance(gameId, actorUserId, cardId);
        Card trainerCard = trainerCard(cardId, CardCategory.ITEM_TRAINER);
        GameEventDto effectEvent = event(gameId, GameEventType.CARD_DRAWN);
        GameEventDto trainerEvent = event(gameId, GameEventType.TRAINER_PLAYED);
        TestTrainerEffect trainerEffect = new TestTrainerEffect(true, new TrainerEffectResult(
                Map.<String, Object>of("effectType", "TEST_EFFECT"),
                List.of(effectEvent)));
        TrainerEffectServiceImpl service = serviceWith(trainerEffect);

        configureTrainerLookup(gameId, actorUserId, cardId, trainerInstance, trainerCard);
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.DISCARD)).thenReturn(3);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TRAINER_PLAYED), eq(5), anyMap()))
                .thenReturn(trainerEvent);

        GameActionExecutionResult result = service.executeTrainer(context(gameId, actorUserId, cardId, false));

        assertThat(result.gameState().turn().supporterPlayedThisTurn()).isFalse();
        assertThat(result.gameState().stateVersion()).isEqualTo(5);
        assertThat(result.emittedEvents()).containsExactly(trainerEvent, effectEvent);
        assertThat(trainerInstance.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(trainerInstance.getZonePosition()).isEqualTo(3);
        assertThat(trainerEffect.applied()).isTrue();
        verify(gameCardInstanceStateService).save(trainerInstance);
        verify(gameCardInstanceStateService).resequenceZone(gameId, actorUserId, CardZone.HAND);
    }

    @Test
    void shouldExecuteSupporterTrainerAndSetSupporterFlag() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GameCardInstance trainerInstance = handCardInstance(gameId, actorUserId, cardId);
        Card trainerCard = trainerCard(cardId, CardCategory.SUPPORTER_TRAINER);
        GameEventDto trainerEvent = event(gameId, GameEventType.TRAINER_PLAYED);
        TestTrainerEffect trainerEffect = new TestTrainerEffect(true, TrainerEffectResult.empty());
        TrainerEffectServiceImpl service = serviceWith(trainerEffect);

        configureTrainerLookup(gameId, actorUserId, cardId, trainerInstance, trainerCard);
        when(supporterLockService.isSupporterLocked(gameId, actorUserId, 2)).thenReturn(false);
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.DISCARD)).thenReturn(4);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TRAINER_PLAYED), eq(5), anyMap()))
                .thenReturn(trainerEvent);

        GameActionExecutionResult result = service.executeTrainer(context(gameId, actorUserId, cardId, false));

        assertThat(result.gameState().turn().supporterPlayedThisTurn()).isTrue();
        assertThat(result.emittedEvents()).containsExactly(trainerEvent);
        assertThat(trainerEffect.applied()).isTrue();
    }

    @Test
    void shouldUseCardIdAsTrainerCardAndIgnoreTargetCardIdForTrainerLookup() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID targetCardId = UUID.randomUUID();
        GameCardInstance trainerInstance = handCardInstance(gameId, actorUserId, cardId);
        Card trainerCard = trainerCard(cardId, CardCategory.ITEM_TRAINER);
        GameEventDto trainerEvent = event(gameId, GameEventType.TRAINER_PLAYED);
        TestTrainerEffect trainerEffect = new TestTrainerEffect(true, TrainerEffectResult.empty());
        TrainerEffectServiceImpl service = serviceWith(trainerEffect);

        configureTrainerLookup(gameId, actorUserId, cardId, trainerInstance, trainerCard);
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.DISCARD)).thenReturn(4);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TRAINER_PLAYED), eq(5), anyMap()))
                .thenReturn(trainerEvent);

        service.executeTrainer(context(
                gameId,
                actorUserId,
                cardId,
                false,
                Map.<String, Object>of(
                        "cardId", cardId.toString(),
                        "targetCardId", targetCardId.toString())));

        verify(gameCardInstanceStateService).findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
                gameId,
                actorUserId,
                cardId,
                CardZone.HAND);
        verify(cardService).getCardEntityById(cardId);
        verify(cardService, never()).getCardEntityById(targetCardId);
        assertThat(trainerEffect.applied()).isTrue();
    }

    @Test
    void shouldExecuteTrainerUsingCardInstanceIdFromPayload() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GameCardInstance trainerInstance = handCardInstance(gameId, actorUserId, cardId);
        Card trainerCard = trainerCard(cardId, CardCategory.POKEMON_TOOL_TRAINER);
        GameEventDto trainerEvent = event(gameId, GameEventType.TRAINER_PLAYED);
        TestTrainerEffect trainerEffect = new TestTrainerEffect(true, TrainerEffectResult.empty());
        TrainerEffectServiceImpl service = serviceWith(trainerEffect);

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                trainerInstance.getId(),
                gameId,
                actorUserId)).thenReturn(Optional.of(trainerInstance));
        when(cardService.getCardEntityById(cardId)).thenReturn(trainerCard);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TRAINER_PLAYED), eq(5), anyMap()))
                .thenReturn(trainerEvent);

        service.executeTrainer(context(
                gameId,
                actorUserId,
                cardId,
                false,
                Map.<String, Object>of("cardInstanceId", trainerInstance.getId().toString())));

        verify(gameCardInstanceStateService).findByIdAndGameIdAndOwnerUserId(
                trainerInstance.getId(),
                gameId,
                actorUserId);
        verify(gameCardInstanceStateService, never()).findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
                gameId,
                actorUserId,
                cardId,
                CardZone.HAND);
        verify(cardService).getCardEntityById(cardId);
        assertThat(trainerEffect.applied()).isTrue();
    }

    @Test
    void shouldRejectSecondSupporterBeforeApplyingEffect() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GameCardInstance trainerInstance = handCardInstance(gameId, actorUserId, cardId);
        Card trainerCard = trainerCard(cardId, CardCategory.SUPPORTER_TRAINER);
        TestTrainerEffect trainerEffect = new TestTrainerEffect(true, TrainerEffectResult.empty());
        TrainerEffectServiceImpl service = serviceWith(trainerEffect);

        configureTrainerLookup(gameId, actorUserId, cardId, trainerInstance, trainerCard);
        when(supporterLockService.isSupporterLocked(gameId, actorUserId, 2)).thenReturn(false);

        assertThatThrownBy(executeCall(service, context(gameId, actorUserId, cardId, true)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Supporter already played this turn");
        assertThat(trainerEffect.applied()).isFalse();
        verify(gameCardInstanceStateService, never()).save(trainerInstance);
    }

    @Test
    void shouldRejectSupporterWhenSupportersAreLockedThisTurn() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GameCardInstance trainerInstance = handCardInstance(gameId, actorUserId, cardId);
        Card trainerCard = trainerCard(cardId, CardCategory.SUPPORTER_TRAINER);
        TestTrainerEffect trainerEffect = new TestTrainerEffect(true, TrainerEffectResult.empty());
        TrainerEffectServiceImpl service = serviceWith(trainerEffect);

        configureTrainerLookup(gameId, actorUserId, cardId, trainerInstance, trainerCard);
        when(supporterLockService.isSupporterLocked(gameId, actorUserId, 2)).thenReturn(true);

        assertThatThrownBy(executeCall(service, context(gameId, actorUserId, cardId, false)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Supporter cards cannot be played this turn");
        assertThat(trainerEffect.applied()).isFalse();
        verify(gameCardInstanceStateService, never()).save(trainerInstance);
    }

    @Test
    void shouldRejectTrainerWithoutRegisteredEffect() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GameCardInstance trainerInstance = handCardInstance(gameId, actorUserId, cardId);
        Card trainerCard = trainerCard(cardId, CardCategory.ITEM_TRAINER);
        TestTrainerEffect trainerEffect = new TestTrainerEffect(false, TrainerEffectResult.empty());
        TrainerEffectServiceImpl service = serviceWith(trainerEffect);

        configureTrainerLookup(gameId, actorUserId, cardId, trainerInstance, trainerCard);

        assertThatThrownBy(executeCall(service, context(gameId, actorUserId, cardId, false)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("No Trainer effect is registered");
    }

    private TrainerEffectServiceImpl serviceWith(TrainerEffect trainerEffect) {
        return new TrainerEffectServiceImpl(
                List.of(trainerEffect),
                new GameActionPayloadReaderImpl(),
                new AvailableActionsFactoryImpl(),
                gameCardInstanceStateService,
                cardService,
                gameEventFactory,
                supporterLockService,
                passiveAbilityService,
                gameLookupService,
                gameStateQueryService);
    }

    private void configureTrainerLookup(
            UUID gameId,
            UUID actorUserId,
            UUID cardId,
            GameCardInstance trainerInstance,
            Card trainerCard) {
        when(gameCardInstanceStateService.findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
                gameId,
                actorUserId,
                cardId,
                CardZone.HAND)).thenReturn(Optional.of(trainerInstance));
        when(cardService.getCardEntityById(cardId)).thenReturn(trainerCard);
    }

    private ThrowingCallable executeCall(TrainerEffectServiceImpl service, GameActionContext context) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                service.executeTrainer(context);
            }
        };
    }

    private GameActionContext context(UUID gameId, UUID actorUserId, UUID cardId, boolean supporterPlayedThisTurn) {
        return context(
                gameId,
                actorUserId,
                cardId,
                supporterPlayedThisTurn,
                Map.<String, Object>of("cardId", cardId.toString()));
    }

    private GameActionContext context(
            UUID gameId,
            UUID actorUserId,
            UUID cardId,
            boolean supporterPlayedThisTurn,
            Map<String, Object> payload) {
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                4,
                actorUserId,
                false,
                supporterPlayedThisTurn,
                false,
                List.of(GameActionType.PLAY_TRAINER),
                Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.PLAY_TRAINER,
                4,
                payload);
        return new GameActionContext(gameId, actorUserId, request, state);
    }

    private GameCardInstance handCardInstance(UUID gameId, UUID ownerUserId, UUID cardId) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(CardZone.HAND);
        cardInstance.setZonePosition(1);
        cardInstance.setFaceDown(false);
        return cardInstance;
    }

    private Card trainerCard(UUID cardId, CardCategory category) {
        Card card = new Card();
        card.setId(cardId);
        card.setExternalId("test-trainer-" + cardId);
        card.setSupertype(CardSupertype.TRAINER);
        card.setCategory(category);
        card.setRawJson("{}");
        return card;
    }

    private GameEventDto event(UUID gameId, GameEventType eventType) {
        return new GameEventDto(
                UUID.randomUUID(),
                gameId,
                eventType,
                5,
                false,
                Instant.parse("2026-05-24T12:00:01Z"),
                Map.of());
    }

    private static final class TestTrainerEffect implements TrainerEffect {

        private final boolean supported;
        private final TrainerEffectResult result;
        private boolean applied;

        private TestTrainerEffect(boolean supported, TrainerEffectResult result) {
            this.supported = supported;
            this.result = result;
            this.applied = false;
        }

        @Override
        public boolean supports(Card card) {
            return supported;
        }

        @Override
        public TrainerEffectResult apply(TrainerEffectContext context) {
            applied = true;
            return result;
        }

        private boolean applied() {
            return applied;
        }
    }
}
