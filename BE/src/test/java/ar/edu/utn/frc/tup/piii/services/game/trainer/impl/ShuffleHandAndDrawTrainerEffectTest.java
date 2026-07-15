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
class ShuffleHandAndDrawTrainerEffectTest {

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
        assertThat(effect().supports(card(CardCategory.BASIC_POKEMON))).isFalse();
    }

    @Test
    void supports_itemTrainerWrongType_returnsFalse() {
        Card card = card(CardCategory.ITEM_TRAINER);
        card.setRawJson("{\"engineEffect\":{\"type\":\"DRAW_CARDS\",\"amount\":3}}");
        assertThat(effect().supports(card)).isFalse();
    }

    @Test
    void supports_zeroAmount_returnsFalse() {
        Card card = card(CardCategory.SUPPORTER_TRAINER);
        card.setRawJson("{\"engineEffect\":{\"type\":\"SHUFFLE_HAND_AND_DRAW\",\"amount\":0}}");
        assertThat(effect().supports(card)).isFalse();
    }

    @Test
    void supports_supporterTrainerShauna_returnsTrue() {
        // xy1-127 = Shauna: SHUFFLE_HAND_AND_DRAW, amount=5
        Card card = card(CardCategory.SUPPORTER_TRAINER);
        card.setExternalId("xy1-127");
        assertThat(effect().supports(card)).isTrue();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_shufflesHandAndDeckThenDraws() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER);
        trainerCard.setExternalId("xy1-127"); // amount=5

        GameCardInstance trainerInstance = instance(UUID.randomUUID(), actorUserId, trainerCard.getId(), CardZone.HAND, 1);
        GameCardInstance handCard = instance(UUID.randomUUID(), actorUserId, UUID.randomUUID(), CardZone.HAND, 2);
        GameCardInstance deckCard = instance(UUID.randomUUID(), actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        GameCardInstance drawnCard = instance(UUID.randomUUID(), actorUserId, UUID.randomUUID(), CardZone.HAND, 1);
        GameEventDto publicEvent = event(gameId, false);
        GameEventDto privateEvent = event(gameId, true);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.HAND))
                .thenReturn(List.of(trainerInstance, handCard));
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(deckCard));
        when(drawCardsEffectService.drawCards(gameId, actorUserId, 5)).thenReturn(List.of(drawnCard));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any()))
                .thenReturn(publicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any(), eq(actorUserId)))
                .thenReturn(privateEvent);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        // handCard (not trainer) and deckCard both become DECK
        assertThat(handCard.getZone()).isEqualTo(CardZone.DECK);
        assertThat(handCard.getFaceDown()).isTrue();
        assertThat(deckCard.getZone()).isEqualTo(CardZone.DECK);
        // trainer instance itself stays in HAND (was excluded from combined list)
        assertThat(trainerInstance.getZone()).isEqualTo(CardZone.HAND);

        assertThat(result.effectData()).containsEntry("effectType", "SHUFFLE_HAND_AND_DRAW");
        assertThat(result.effectData()).containsEntry("cardsDrawn", 5);
        assertThat(result.emittedEvents()).containsExactly(publicEvent, privateEvent);

        verify(drawCardsEffectService).drawCards(gameId, actorUserId, 5);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private ShuffleHandAndDrawTrainerEffect effect() {
        return new ShuffleHandAndDrawTrainerEffect(
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
        inst.setFaceDown(zone == CardZone.DECK);
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
