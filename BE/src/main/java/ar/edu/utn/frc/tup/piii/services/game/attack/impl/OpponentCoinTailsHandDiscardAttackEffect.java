package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

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
import ar.edu.utn.frc.tup.piii.services.game.attack.PendingChoiceTimeoutPolicy;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Models attacks such as Malamar (XY1-76) "Mental Trash": the attacker flips a number of coins
 * and, for each tails, the DEFENDER discards one card of their own choosing from their hand (not
 * a random card - the card text never says "at random", so the discarding player picks). When at
 * least one card must be discarded, this effect suspends the attack on a pending choice owned by
 * the defender; {@code AttackChoiceServiceImpl} applies the actual discard once the defender
 * confirms their selection.
 */
@Service
@RequiredArgsConstructor
public class OpponentCoinTailsHandDiscardAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "OPPONENT_COIN_TAILS_HAND_DISCARD";
    public static final String SELECT_HAND_CARDS_TO_DISCARD = "SELECT_HAND_CARDS_TO_DISCARD";

    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        int coinCount = context.operation().coinCount();
        List<String> coinResults = new ArrayList<>();
        int tailsCount = 0;
        for (int i = 0; i < coinCount; i++) {
            boolean heads = gameRandomService.flipCoin();
            coinResults.add(heads ? "HEADS" : "TAILS");
            if (!heads) {
                tailsCount++;
            }
        }

        UUID gameId = context.resolutionContext().gameId();
        UUID defenderUserId = context.resolutionContext().defenderUserId();
        List<GameCardInstance> handCards;
        int discardCount;
        if (tailsCount > 0) {
            handCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                    gameId,
                    defenderUserId,
                    CardZone.HAND);
            discardCount = Math.min(tailsCount, handCards.size());
        } else {
            handCards = List.of();
            discardCount = 0;
        }

        GameEventDto coinEvent = gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "actorPlayerId", defenderUserId.toString(),
                        "pokemonInPlayId", context.resolutionContext().defenderPokemon().getId().toString(),
                        "coinResults", List.copyOf(coinResults),
                        "tailsCount", tailsCount,
                        "discardCount", discardCount));

        if (discardCount <= 0) {
            return new AttackEffectResult(0, false, List.of(coinEvent));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cards", cardOptions(handCards));
        payload.put("discardCount", discardCount);
        payload.put("coinResults", List.copyOf(coinResults));
        payload.put("tailsCount", tailsCount);
        payload.put(PendingAttackChoiceEffect.OPTIONAL_KEY, false);
        payload.put(PendingAttackChoiceEffect.TIMEOUT_POLICY_KEY, PendingChoiceTimeoutPolicy.AUTO_RANDOM_LEGAL.name());
        payload.put(PendingAttackChoiceEffect.MIN_SELECTIONS_KEY, discardCount);
        payload.put(PendingAttackChoiceEffect.MAX_SELECTIONS_KEY, discardCount);
        return AttackEffectResult.requiringChoice(
                SELECT_HAND_CARDS_TO_DISCARD,
                Map.copyOf(payload),
                List.of(coinEvent),
                defenderUserId);
    }

    private List<Map<String, Object>> cardOptions(List<GameCardInstance> handCards) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (GameCardInstance card : handCards) {
            options.add(cardOption(card));
        }
        return List.copyOf(options);
    }

    private Map<String, Object> cardOption(GameCardInstance card) {
        Card cardEntity = cardService.getCardEntityById(card.getCardId());
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("cardInstanceId", card.getId().toString());
        option.put("cardId", card.getCardId().toString());
        option.put("externalId", cardEntity.getExternalId());
        option.put("name", cardEntity.getName());
        option.put("imageSmallUrl", cardEntity.getImageSmallUrl());
        option.put("imageLargeUrl", cardEntity.getImageLargeUrl());
        option.put("category", cardEntity.getCategory() == null ? null : cardEntity.getCategory().name());
        return Collections.unmodifiableMap(option);
    }
}
