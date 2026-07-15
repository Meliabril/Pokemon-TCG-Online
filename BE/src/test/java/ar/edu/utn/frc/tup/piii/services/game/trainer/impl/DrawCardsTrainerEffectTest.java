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
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrawCardsTrainerEffectTest {

    @Mock
    private DrawCardsEffectService drawCardsEffectService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private GameRandomService gameRandomService;

    @Test
    void shouldSupportItemOrSupporterWithDrawCardsMetadata() {
        DrawCardsTrainerEffect effect = effect();

        assertThat(effect.supports(card(CardCategory.ITEM_TRAINER, drawRawJson(2)))).isTrue();
        assertThat(effect.supports(card(CardCategory.SUPPORTER_TRAINER, drawRawJson(2)))).isTrue();
        assertThat(effect.supports(card(CardCategory.ITEM_TRAINER, "{}"))).isFalse();
        assertThat(effect.supports(card(CardCategory.BASIC_POKEMON, drawRawJson(2)))).isFalse();
    }

    @Test
    void shouldDrawConfiguredAmountFromDeck() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, drawRawJson(2));
        GameCardInstance firstDeckCard = cardInstance(UUID.randomUUID(), actorUserId, UUID.randomUUID(), CardZone.HAND, 4);
        GameCardInstance secondDeckCard = cardInstance(UUID.randomUUID(), actorUserId, UUID.randomUUID(), CardZone.HAND, 5);
        GameEventDto publicEvent = event(gameId, false);
        GameEventDto privateEvent = event(gameId, true);
        DrawCardsTrainerEffect effect = effect();

        when(drawCardsEffectService.drawCards(gameId, actorUserId, 2))
                .thenReturn(List.of(firstDeckCard, secondDeckCard));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), eq(7), anyMap()))
                .thenReturn(publicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), eq(7), anyMap(), eq(actorUserId)))
                .thenReturn(privateEvent);

        TrainerEffectResult result = effect.apply(context(gameId, actorUserId, trainerCard));

        assertThat(result.effectData()).containsEntry("effectType", "DRAW_CARDS");
        assertThat(result.effectData()).containsEntry("cardsDrawn", 2);
        assertThat(result.emittedEvents()).containsExactly(publicEvent, privateEvent);
    }

    private DrawCardsTrainerEffect effect() {
        return new DrawCardsTrainerEffect(
                new TrainerEffectDefinitionReader(new ObjectMapper()),
                drawCardsEffectService,
                gameEventFactory,
                gameRandomService);
    }

    private TrainerEffectContext context(UUID gameId, UUID actorUserId, Card trainerCard) {
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                3,
                6,
                actorUserId,
                List.of(GameActionType.PLAY_TRAINER),
                Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.PLAY_TRAINER,
                6,
                Map.<String, Object>of("cardId", trainerCard.getId().toString()));
        return new TrainerEffectContext(
                gameId,
                actorUserId,
                request,
                state,
                cardInstance(UUID.randomUUID(), actorUserId, trainerCard.getId(), CardZone.HAND, 1),
                trainerCard,
                7);
    }

    private Card card(CardCategory category, String rawJson) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId("test-card-" + card.getId());
        card.setSupertype(CardSupertype.TRAINER);
        card.setCategory(category);
        card.setRawJson(rawJson);
        return card;
    }

    private GameCardInstance cardInstance(
            UUID instanceId,
            UUID ownerUserId,
            UUID cardId,
            CardZone zone,
            int position) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(instanceId);
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(zone);
        cardInstance.setZonePosition(position);
        cardInstance.setFaceDown(false);
        return cardInstance;
    }

    private GameEventDto event(UUID gameId, boolean privateEvent) {
        return new GameEventDto(
                UUID.randomUUID(),
                gameId,
                GameEventType.CARD_DRAWN,
                7,
                privateEvent,
                Instant.parse("2026-05-24T12:00:01Z"),
                Map.of());
    }

    private String drawRawJson(int amount) {
        return "{\"engineEffect\":{\"type\":\"DRAW_CARDS\",\"amount\":" + amount + "}}";
    }
}
