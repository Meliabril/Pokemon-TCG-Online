package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Models attacks like Zorua's Nasty Plot: search the deck for any 1 card,
 * put it into hand, then shuffle the deck.
 */
@Service
@RequiredArgsConstructor
public class SearchAnyCardFromDeckAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "SEARCH_ANY_CARD_FROM_DECK";
    private static final int FIRST_TEMPORARY_ZONE_POSITION = -1;

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        UUID gameId = context.resolutionContext().gameId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                attackerUserId,
                CardZone.DECK);

        boolean foundCard = !deckCards.isEmpty();
        GameCardInstance foundCardInstance = foundCard ? gameRandomService.chooseOne(deckCards) : null;
        if (foundCard) {
            moveToHand(gameId, attackerUserId, foundCardInstance);
            shuffleDeck(gameId, attackerUserId, deckCards, foundCardInstance);
        }

        GameEventDto event = gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "foundCard", foundCard,
                        "cardInstanceId", foundCard ? foundCardInstance.getId().toString() : "",
                        "actorPlayerId", attackerUserId.toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private void moveToHand(UUID gameId, UUID attackerUserId, GameCardInstance cardInstance) {
        cardInstance.setZone(CardZone.HAND);
        cardInstance.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.HAND));
        cardInstance.setFaceDown(false);
        gameCardInstanceStateService.save(cardInstance);
        gameCardInstanceStateService.resequenceZone(gameId, attackerUserId, CardZone.HAND);
    }

    private void shuffleDeck(
            UUID gameId,
            UUID attackerUserId,
            List<GameCardInstance> previousDeckCards,
            GameCardInstance removedCard) {
        List<GameCardInstance> remainingDeckCards = previousDeckCards.stream()
                .filter(deckCard -> !deckCard.getId().equals(removedCard.getId()))
                .sorted(Comparator.comparing(GameCardInstance::getZonePosition, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (remainingDeckCards.isEmpty()) {
            return;
        }

        List<GameCardInstance> shuffledDeckCards = gameRandomService.shuffledCopy(remainingDeckCards);
        List<GameCardInstance> cardsToSave = new ArrayList<>();
        int temporaryPosition = FIRST_TEMPORARY_ZONE_POSITION;
        for (GameCardInstance deckCard : shuffledDeckCards) {
            deckCard.setZonePosition(temporaryPosition--);
            cardsToSave.add(deckCard);
        }
        gameCardInstanceStateService.saveAll(cardsToSave);
        gameCardInstanceStateService.flush();

        int position = 1;
        for (GameCardInstance deckCard : shuffledDeckCards) {
            deckCard.setZonePosition(position++);
        }
        gameCardInstanceStateService.saveAll(shuffledDeckCards);
    }
}
