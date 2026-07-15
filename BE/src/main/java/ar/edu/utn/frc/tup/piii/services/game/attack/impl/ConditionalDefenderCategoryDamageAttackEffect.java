package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConditionalDefenderCategoryDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "CONDITIONAL_DEFENDER_CATEGORY_DAMAGE";

    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        CardCategory expectedCategory = expectedCategory(context.operation().cardCategory());
        Card defenderCard = context.resolutionContext().defenderCard();
        boolean conditionMet = matchesCategory(defenderCard, expectedCategory);
        int damageModifier = conditionMet ? context.operation().amount() : 0;

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "conditionMet", conditionMet,
                        "damageModifier", damageModifier,
                        "expectedCategory", expectedCategory.name(),
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }

    private CardCategory expectedCategory(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidGameActionException("Conditional defender category damage must declare a card category");
        }

        try {
            return CardCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidGameActionException("Unsupported defender card category: " + value);
        }
    }

    private boolean matchesCategory(Card defenderCard, CardCategory expectedCategory) {
        if (defenderCard == null || defenderCard.getCategory() == null) {
            return false;
        }
        if (defenderCard.getCategory() == expectedCategory) {
            return true;
        }

        return expectedCategory == CardCategory.POKEMON_EX
                && defenderCard.getCategory() == CardCategory.MEGA_POKEMON;
    }
}
