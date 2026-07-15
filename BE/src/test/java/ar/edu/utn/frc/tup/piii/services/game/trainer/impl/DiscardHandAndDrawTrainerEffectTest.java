package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

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
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscardHandAndDrawTrainerEffectTest {

    @Mock
    private DrawCardsEffectService drawCardsEffectService;
    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock
    private GameEventFactory gameEventFactory;

    // ─── supports ────────────────────────────────────────────────────────────

    @Test
    void supports_nullCard_returnsFalse() {
        assertThat(effect().supports(null)).isFalse();
    }

    @Test
    void supports_pokemonCard_returnsFalse() {
        Card card = card(CardCategory.BASIC_POKEMON);
        assertThat(effect().supports(card)).isFalse();
    }

    @Test
    void supports_itemTrainerWrongEffectType_returnsFalse() {
        Card card = card(CardCategory.ITEM_TRAINER);
        card.setExternalId("xy1-999");
        assertThat(effect().supports(card)).isFalse();
    }

    @Test
    void supports_supporterTrainerWithDiscardHandAndDraw_returnsTrue() {
        // xy1-122 = Professor Sycamore: DISCARD_HAND_AND_DRAW, amount=7
        Card card = card(CardCategory.SUPPORTER_TRAINER);
        card.setExternalId("xy1-122");
        assertThat(effect().supports(card)).isTrue();
    }

    @Test
    void supports_itemTrainerWithRawJsonEffect_returnsTrue() {
        Card card = card(CardCategory.ITEM_TRAINER);
        card.setRawJson("{\"engineEffect\":{\"type\":\"DISCARD_HAND_AND_DRAW\",\"amount\":4}}");
        assertThat(effect().supports(card)).isTrue();
    }

    @Test
    void supports_zeroAmount_returnsFalse() {
        Card card = card(CardCategory.ITEM_TRAINER);
        card.setRawJson("{\"engineEffect\":{\"type\":\"DISCARD_HAND_AND_DRAW\",\"amount\":0}}");
        assertThat(effect().supports(card)).isFalse();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_discardHandAndDrawsConfiguredCards() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER);
        trainerCard.setExternalId("xy1-122"); // amount=7

        GameCardInstance trainerInstance = instance(UUID.randomUUID(), actorUserId, trainerCard.getId(), CardZone.HAND, 1);
        GameCardInstance otherCard = instance(UUID.randomUUID(), actorUserId, UUID.randomUUID(), CardZone.HAND, 2);
        GameCardInstance drawnCard = instance(UUID.randomUUID(), actorUserId, UUID.randomUUID(), CardZone.HAND, 3);
        GameEventDto publicEvent = event(gameId, false);
        GameEventDto privateEvent = event(gameId, true);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.HAND))
                .thenReturn(List.of(trainerInstance, otherCard));
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.DISCARD)).thenReturn(1);
        when(drawCardsEffectService.drawCards(gameId, actorUserId, 7)).thenReturn(List.of(drawnCard));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any()))
                .thenReturn(publicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any(), eq(actorUserId)))
                .thenReturn(privateEvent);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(otherCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(otherCard.getFaceDown()).isFalse();
        assertThat(trainerInstance.getZone()).isEqualTo(CardZone.HAND);

        assertThat(result.effectData()).containsEntry("effectType", "DISCARD_HAND_AND_DRAW");
        assertThat(result.effectData()).containsEntry("cardsDrawn", 7);
        assertThat(result.emittedEvents()).containsExactly(publicEvent, privateEvent);

        verify(gameCardInstanceStateService).saveAll(List.of(otherCard));
        verify(gameCardInstanceStateService).resequenceZone(gameId, actorUserId, CardZone.DISCARD);
        verify(gameCardInstanceStateService).resequenceZone(gameId, actorUserId, CardZone.HAND);
        verify(drawCardsEffectService).drawCards(gameId, actorUserId, 7);
    }

    @Test
    void apply_emptyHand_drawsWithoutDiscard() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER);
        trainerCard.setExternalId("xy1-122");
        GameCardInstance trainerInstance = instance(UUID.randomUUID(), actorUserId, trainerCard.getId(), CardZone.HAND, 1);
        GameEventDto publicEvent = event(gameId, false);
        GameEventDto privateEvent = event(gameId, true);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.HAND))
                .thenReturn(List.of(trainerInstance));
        when(drawCardsEffectService.drawCards(gameId, actorUserId, 7)).thenReturn(List.of());
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any()))
                .thenReturn(publicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any(), eq(actorUserId)))
                .thenReturn(privateEvent);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(result.effectData()).containsEntry("cardsDrawn", 7);
        verify(gameCardInstanceStateService).saveAll(List.of());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private DiscardHandAndDrawTrainerEffect effect() {
        return new DiscardHandAndDrawTrainerEffect(
                new TrainerEffectDefinitionReader(new ObjectMapper()),
                drawCardsEffectService,
                gameCardInstanceStateService,
                gameEventFactory);
    }

    private Card card(CardCategory category) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setSupertype(CardSupertype.TRAINER);
        card.setCategory(category);
        return card;
    }

    private GameCardInstance instance(UUID id, UUID ownerUserId, UUID cardId, CardZone zone, int position) {
        GameCardInstance inst = new GameCardInstance();
        inst.setId(id);
        inst.setOwnerUserId(ownerUserId);
        inst.setCardId(cardId);
        inst.setZone(zone);
        inst.setZonePosition(position);
        inst.setFaceDown(false);
        return inst;
    }

    private GameEventDto event(UUID gameId, boolean privateEvent) {
        return new GameEventDto(UUID.randomUUID(), gameId, GameEventType.CARD_DRAWN, 7,
                privateEvent, Instant.now(), Map.of());
    }

    private TrainerEffectContext context(UUID gameId, UUID actorUserId, Card trainerCard, GameCardInstance trainerInstance) {
        GameStateDto state = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 3, 6, actorUserId,
                List.of(GameActionType.PLAY_TRAINER), Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.PLAY_TRAINER, 6,
                Map.of("cardId", trainerCard.getId().toString()));
        return new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);
    }
}
