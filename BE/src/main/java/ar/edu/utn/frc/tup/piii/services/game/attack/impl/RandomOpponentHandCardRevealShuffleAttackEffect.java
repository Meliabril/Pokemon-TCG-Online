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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RandomOpponentHandCardRevealShuffleAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "RANDOM_OPPONENT_HAND_CARD_REVEAL_SHUFFLE";
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
        UUID defenderUserId = context.resolutionContext().defenderUserId();

        List<GameCardInstance> handCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                defenderUserId,
                CardZone.HAND);
        if (handCards.isEmpty()) {
            return AttackEffectResult.empty();
        }

        GameCardInstance revealedCard = gameRandomService.chooseOne(handCards);
        int handSizeBefore = handCards.size();
        shuffleIntoDeck(gameId, defenderUserId, revealedCard);
        gameCardInstanceStateService.resequenceZone(gameId, defenderUserId, CardZone.HAND);

        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.privateEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "revealedCardInstanceId", revealedCard.getId().toString(),
                        "revealedCardId", revealedCard.getCardId().toString(),
                        "opponentPlayerId", defenderUserId.toString()),
                attackerUserId));
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "actorPlayerId", attackerUserId.toString(),
                        "opponentPlayerId", defenderUserId.toString(),
                        "revealedCardId", revealedCard.getCardId().toString(),
                        "opponentHandSize", handSizeBefore,
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString())));
        return new AttackEffectResult(0, false, events);
    }

    private void shuffleIntoDeck(UUID gameId, UUID defenderUserId, GameCardInstance revealedCard) {
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                defenderUserId,
                CardZone.DECK);

        List<GameCardInstance> cardsToShuffle = new ArrayList<>(deckCards);
        cardsToShuffle.add(revealedCard);
        revealedCard.setZone(CardZone.DECK);
        revealedCard.setFaceDown(true);

        List<GameCardInstance> shuffledDeckCards = gameRandomService.shuffledCopy(cardsToShuffle);
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
