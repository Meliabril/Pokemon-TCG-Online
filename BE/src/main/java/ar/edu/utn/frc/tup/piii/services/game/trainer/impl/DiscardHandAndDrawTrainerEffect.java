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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DiscardHandAndDrawTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "DISCARD_HAND_AND_DRAW";
    private static final String EVENT_SOURCE = "TRAINER";

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
        UUID trainerInstanceId = context.trainerCardInstance().getId();

        List<GameCardInstance> handCards = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.HAND);

        List<GameCardInstance> toDiscard = new ArrayList<>();
        for (GameCardInstance card : handCards) {
            if (!card.getId().equals(trainerInstanceId)) {
                toDiscard.add(card);
            }
        }

        int nextDiscardPosition = gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.DISCARD);
        for (GameCardInstance card : toDiscard) {
            card.setZone(CardZone.DISCARD);
            card.setFaceDown(false);
            card.setZonePosition(nextDiscardPosition++);
        }
        gameCardInstanceStateService.saveAll(toDiscard);
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.DISCARD);
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.HAND);

        List<GameCardInstance> drawnCards = drawCardsEffectService.drawCards(gameId, actorUserId, cardsToDraw);
        List<String> drawnCardIds = new ArrayList<>();
        for (GameCardInstance drawnCard : drawnCards) {
            drawnCardIds.add(drawnCard.getCardId().toString());
        }

        List<GameEventDto> events = new ArrayList<>();
        Map<String, Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("playerId", actorUserId.toString());
        publicPayload.put("cardsDrawn", cardsToDraw);
        publicPayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.publicEvent(
                gameId, GameEventType.CARD_DRAWN, context.stateVersion(), Map.copyOf(publicPayload)));

        Map<String, Object> privatePayload = new LinkedHashMap<>();
        privatePayload.put("playerId", actorUserId.toString());
        privatePayload.put("cardIds", List.copyOf(drawnCardIds));
        privatePayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.privateEvent(
                gameId, GameEventType.CARD_DRAWN, context.stateVersion(), Map.copyOf(privatePayload), actorUserId));

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", EFFECT_TYPE);
        effectData.put("cardsDrawn", cardsToDraw);
        effectData.put("discardedHandCards", toDiscard.size());
        return new TrainerEffectResult(Map.copyOf(effectData), List.copyOf(events));
    }

    private boolean isItemOrSupporter(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }
        return CardCategory.ITEM_TRAINER.equals(card.getCategory())
                || CardCategory.SUPPORTER_TRAINER.equals(card.getCategory());
    }
}
