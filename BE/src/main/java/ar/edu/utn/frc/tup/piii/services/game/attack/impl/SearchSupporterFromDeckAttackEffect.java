package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
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

@Service
@RequiredArgsConstructor
public class SearchSupporterFromDeckAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "SEARCH_SUPPORTER_FROM_DECK";
    private static final int FIRST_TEMPORARY_ZONE_POSITION = -1;

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;
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

        GameCardInstance supporterCard = firstSupporter(deckCards);
        boolean foundSupporter = supporterCard != null;
        if (foundSupporter) {
            moveToHand(gameId, attackerUserId, supporterCard);
            shuffleDeck(gameId, attackerUserId, deckCards, supporterCard);
        }

        GameEventDto event = gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "foundSupporter", foundSupporter,
                        "cardInstanceId", foundSupporter ? supporterCard.getId().toString() : "",
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private GameCardInstance firstSupporter(List<GameCardInstance> deckCards) {
        for (GameCardInstance deckCard : deckCards) {
            Card card = cardService.getCardEntityById(deckCard.getCardId());
            if (card != null && CardCategory.SUPPORTER_TRAINER.equals(card.getCategory())) {
                return deckCard;
            }
        }
        return null;
    }

    private void moveToHand(UUID gameId, UUID attackerUserId, GameCardInstance supporterCard) {
        supporterCard.setZone(CardZone.HAND);
        supporterCard.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.HAND));
        supporterCard.setFaceDown(false);
        gameCardInstanceStateService.save(supporterCard);
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
