package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
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
public class CoinAllHeadsOpponentDeckDiscardAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "COIN_ALL_HEADS_OPPONENT_DECK_DISCARD";

    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;
    private final GameCardInstanceStateService gameCardInstanceStateService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        int coinCount = context.operation().coinCount();
        List<String> coinResults = new ArrayList<>();
        int headsCount = 0;
        for (int i = 0; i < coinCount; i++) {
            boolean heads = gameRandomService.flipCoin();
            coinResults.add(heads ? "HEADS" : "TAILS");
            if (heads) {
                headsCount++;
            }
        }

        boolean allHeads = coinCount > 0 && headsCount == coinCount;
        int discardCount = allHeads ? discardTopOpponentDeckCards(context) : 0;

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "coinResults", List.copyOf(coinResults),
                        "headsCount", headsCount,
                        "allHeads", allHeads,
                        "discardCount", discardCount,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private int discardTopOpponentDeckCards(AttackEffectContext context) {
        int requestedDiscardCount = attackerDamageCounters(context);
        if (requestedDiscardCount <= 0) {
            return 0;
        }

        UUID gameId = context.resolutionContext().gameId();
        UUID defenderUserId = context.resolutionContext().defenderUserId();
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                defenderUserId,
                CardZone.DECK);
        int discardCount = Math.min(requestedDiscardCount, deckCards.size());
        if (discardCount <= 0) {
            return 0;
        }

        int nextDiscardPosition = gameCardInstanceStateService.nextZonePosition(gameId, defenderUserId, CardZone.DISCARD);
        List<GameCardInstance> discardedCards = new ArrayList<>();
        for (int i = 0; i < discardCount; i++) {
            GameCardInstance card = deckCards.get(i);
            card.setZone(CardZone.DISCARD);
            card.setZonePosition(nextDiscardPosition + i);
            card.setFaceDown(false);
            discardedCards.add(card);
        }

        gameCardInstanceStateService.saveAll(discardedCards);
        gameCardInstanceStateService.resequenceZone(gameId, defenderUserId, CardZone.DECK);
        return discardCount;
    }

    private int attackerDamageCounters(AttackEffectContext context) {
        PokemonInPlay attacker = context.resolutionContext().attackerPokemon();
        if (attacker == null || attacker.getDamageCounters() == null) {
            return 0;
        }
        return attacker.getDamageCounters();
    }
}
