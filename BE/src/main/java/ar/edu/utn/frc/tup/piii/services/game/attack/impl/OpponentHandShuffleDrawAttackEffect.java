package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpponentHandShuffleDrawAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "SHUFFLE_OPPONENT_HAND_INTO_DECK_DRAW";
    private static final String EVENT_SOURCE = "ATTACK";
    private static final int FIRST_TEMPORARY_ZONE_POSITION = -1;

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final DrawCardsEffectService drawCardsEffectService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        int cardsToDraw = context.operation().amount();
        if (cardsToDraw <= 0) {
            throw new InvalidGameActionException("Opponent shuffle draw amount must be positive");
        }

        UUID gameId = context.resolutionContext().gameId();
        UUID defenderUserId = context.resolutionContext().defenderUserId();
        List<GameCardInstance> handCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                defenderUserId,
                CardZone.HAND);

        shuffleHandIntoDeck(gameId, defenderUserId, handCards);
        List<GameCardInstance> drawnCards = drawCardsEffectService.drawCards(gameId, defenderUserId, cardsToDraw);

        return new AttackEffectResult(0, false, events(context, handCards.size(), drawnCards));
    }

    private void shuffleHandIntoDeck(UUID gameId, UUID defenderUserId, List<GameCardInstance> handCards) {
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                defenderUserId,
                CardZone.DECK);
        List<GameCardInstance> cardsToShuffle = new ArrayList<>(deckCards);

        for (GameCardInstance handCard : handCards) {
            handCard.setZone(CardZone.DECK);
            handCard.setFaceDown(true);
            cardsToShuffle.add(handCard);
        }

        List<GameCardInstance> shuffledDeckCards = gameRandomService.shuffledCopy(cardsToShuffle);
        int temporaryPosition = FIRST_TEMPORARY_ZONE_POSITION;
        for (GameCardInstance deckCard : shuffledDeckCards) {
            deckCard.setZonePosition(temporaryPosition--);
        }
        gameCardInstanceStateService.saveAll(shuffledDeckCards);
        gameCardInstanceStateService.flush();

        int position = 1;
        for (GameCardInstance deckCard : shuffledDeckCards) {
            deckCard.setZonePosition(position++);
        }
        gameCardInstanceStateService.saveAll(shuffledDeckCards);
        gameCardInstanceStateService.resequenceZone(gameId, defenderUserId, CardZone.HAND);
    }

    private List<GameEventDto> events(
            AttackEffectContext context,
            int shuffledHandCards,
            List<GameCardInstance> drawnCards) {
        UUID gameId = context.resolutionContext().gameId();
        UUID defenderUserId = context.resolutionContext().defenderUserId();
        List<String> drawnCardIds = new ArrayList<>();
        for (GameCardInstance drawnCard : drawnCards) {
            drawnCardIds.add(drawnCard.getCardId().toString());
        }

        List<GameEventDto> events = new ArrayList<>();
        Map<String, Object> publicDrawPayload = new LinkedHashMap<>();
        publicDrawPayload.put("playerId", defenderUserId.toString());
        publicDrawPayload.put("cardsDrawn", drawnCards.size());
        publicDrawPayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.CARD_DRAWN,
                context.stateVersion(),
                Map.copyOf(publicDrawPayload)));

        Map<String, Object> privateDrawPayload = new LinkedHashMap<>();
        privateDrawPayload.put("playerId", defenderUserId.toString());
        privateDrawPayload.put("cardIds", List.copyOf(drawnCardIds));
        privateDrawPayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.privateEvent(
                gameId,
                GameEventType.CARD_DRAWN,
                context.stateVersion(),
                Map.copyOf(privateDrawPayload),
                defenderUserId));

        Map<String, Object> effectPayload = new LinkedHashMap<>();
        effectPayload.put("effectType", EFFECT_TYPE);
        effectPayload.put("opponentPlayerId", defenderUserId.toString());
        effectPayload.put("shuffledHandCards", shuffledHandCards);
        effectPayload.put("cardsDrawn", drawnCards.size());
        effectPayload.put("pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString());
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.copyOf(effectPayload)));

        return List.copyOf(events);
    }
}
