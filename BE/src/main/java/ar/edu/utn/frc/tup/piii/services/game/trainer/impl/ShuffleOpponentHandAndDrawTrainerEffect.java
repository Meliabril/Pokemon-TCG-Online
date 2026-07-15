package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShuffleOpponentHandAndDrawTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "SHUFFLE_OPPONENT_HAND_AND_DRAW";
    private static final String EVENT_SOURCE = "TRAINER";
    private static final String EFFECT_TYPE_KEY = "effectType";
    private static final String SHUFFLED_HAND_CARDS_KEY = "shuffledHandCards";

    private final TrainerEffectDefinitionReader trainerEffectDefinitionReader;
    private final DrawCardsEffectService drawCardsEffectService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(Card card) {
        if (!isItemOrSupporter(card)) {
            return false;
        }
        TrainerEffectDefinition definition = trainerEffectDefinitionReader.read(card);
        return EFFECT_TYPE.equals(definition.type()) && definition.amount() > 0;
    }

    @Override
    public TrainerEffectResult apply(TrainerEffectContext context) {
        TrainerEffectDefinition definition = trainerEffectDefinitionReader.read(context.trainerCard());
        int cardsToDraw = definition.amount();
        if (cardsToDraw <= 0) {
            throw new InvalidGameActionException("Trainer draw amount must be positive");
        }

        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();
        UUID opponentUserId = opponentOf(context, actorUserId);

        List<GameCardInstance> opponentHand = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, opponentUserId, CardZone.HAND);
        List<GameCardInstance> opponentDeck = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, opponentUserId, CardZone.DECK);
        int shuffledHandCards = opponentHand.size();

        List<GameCardInstance> combined = new ArrayList<>(opponentHand);
        combined.addAll(opponentDeck);
        Collections.shuffle(combined);

        int position = 1;
        for (GameCardInstance card : combined) {
            card.setZone(CardZone.DECK);
            card.setFaceDown(true);
            card.setZonePosition(position++);
        }
        gameCardInstanceStateService.saveAll(combined);

        List<GameCardInstance> drawnCards = drawCardsEffectService.drawCards(gameId, opponentUserId, cardsToDraw);

        List<GameEventDto> events = new ArrayList<>();
        Map<String, Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("playerId", opponentUserId.toString());
        publicPayload.put("cardsDrawn", cardsToDraw);
        publicPayload.put("source", EVENT_SOURCE);
        publicPayload.put(EFFECT_TYPE_KEY, EFFECT_TYPE);
        publicPayload.put(SHUFFLED_HAND_CARDS_KEY, shuffledHandCards);
        events.add(gameEventFactory.publicEvent(
                gameId, GameEventType.CARD_DRAWN, context.stateVersion(), Map.copyOf(publicPayload)));

        List<String> drawnCardIds = new ArrayList<>();
        for (GameCardInstance drawnCard : drawnCards) {
            drawnCardIds.add(drawnCard.getCardId().toString());
        }
        Map<String, Object> privatePayload = new LinkedHashMap<>();
        privatePayload.put("playerId", opponentUserId.toString());
        privatePayload.put("cardIds", List.copyOf(drawnCardIds));
        privatePayload.put("source", EVENT_SOURCE);
        privatePayload.put(EFFECT_TYPE_KEY, EFFECT_TYPE);
        privatePayload.put(SHUFFLED_HAND_CARDS_KEY, shuffledHandCards);
        events.add(gameEventFactory.privateEvent(
                gameId, GameEventType.CARD_DRAWN, context.stateVersion(),
                Map.copyOf(privatePayload), opponentUserId));

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", EFFECT_TYPE);
        effectData.put("targetPlayerId", opponentUserId.toString());
        effectData.put("cardsDrawn", cardsToDraw);
        return new TrainerEffectResult(Map.copyOf(effectData), List.copyOf(events));
    }

    private UUID opponentOf(TrainerEffectContext context, UUID actorUserId) {
        return context.currentState().playerIds().stream()
                .filter(id -> !id.equals(actorUserId))
                .findFirst()
                .orElseThrow(() -> new InvalidGameActionException("Could not determine opponent"));
    }

    private boolean isItemOrSupporter(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }
        return CardCategory.ITEM_TRAINER.equals(card.getCategory())
                || CardCategory.SUPPORTER_TRAINER.equals(card.getCategory());
    }
}
