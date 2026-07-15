package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DrawCardsTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "DRAW_CARDS";
    private static final String EVENT_SOURCE = "TRAINER";
    private static final String ROLLER_SKATES_EXTERNAL_ID = "xy1-125";

    private final TrainerEffectDefinitionReader trainerEffectDefinitionReader;
    private final DrawCardsEffectService drawCardsEffectService;
    private final GameEventFactory gameEventFactory;
    private final GameRandomService gameRandomService;

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

        List<GameEventDto> events = new ArrayList<>();
        boolean rollerSkates = ROLLER_SKATES_EXTERNAL_ID.equals(context.trainerCard().getExternalId());

        if (rollerSkates) {
            boolean heads = gameRandomService.flipCoin();
            String coinResult = heads ? "HEADS" : "TAILS";

            Map<String, Object> coinPayload = new LinkedHashMap<>();
            coinPayload.put("playerId", context.actorUserId().toString());
            coinPayload.put("coinResults", List.of(coinResult));
            coinPayload.put("source", EVENT_SOURCE);
            events.add(gameEventFactory.publicEvent(
                    context.gameId(),
                    GameEventType.COIN_FLIPPED,
                    context.stateVersion(),
                    Map.copyOf(coinPayload)));

            if (!heads) {
                Map<String, Object> effectData = new LinkedHashMap<>();
                effectData.put("effectType", EFFECT_TYPE);
                effectData.put("cardsDrawn", 0);
                effectData.put("coinResults", List.of(coinResult));
                return new TrainerEffectResult(Map.copyOf(effectData), List.copyOf(events));
            }
        }

        List<GameCardInstance> drawnCards = drawCardsEffectService.drawCards(
                context.gameId(), context.actorUserId(), cardsToDraw);
        List<String> drawnCardIds = new ArrayList<>();
        for (GameCardInstance drawnCard : drawnCards) {
            drawnCardIds.add(drawnCard.getCardId().toString());
        }

        Map<String, Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("playerId", context.actorUserId().toString());
        publicPayload.put("cardsDrawn", cardsToDraw);
        publicPayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.CARD_DRAWN,
                context.stateVersion(),
                Map.copyOf(publicPayload)));

        Map<String, Object> privatePayload = new LinkedHashMap<>();
        privatePayload.put("playerId", context.actorUserId().toString());
        privatePayload.put("cardIds", List.copyOf(drawnCardIds));
        privatePayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.privateEvent(
                context.gameId(),
                GameEventType.CARD_DRAWN,
                context.stateVersion(),
                Map.copyOf(privatePayload),
                context.actorUserId()));

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", EFFECT_TYPE);
        effectData.put("cardsDrawn", cardsToDraw);
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
