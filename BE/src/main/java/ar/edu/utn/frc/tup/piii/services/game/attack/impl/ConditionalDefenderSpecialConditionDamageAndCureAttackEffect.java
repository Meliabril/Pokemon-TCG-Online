package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConditionalDefenderSpecialConditionDamageAndCureAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "CONDITIONAL_DEFENDER_SPECIAL_CONDITION_DAMAGE_AND_CURE";

    private final SpecialConditionStateService specialConditionStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay defender = context.targetPokemon();
        List<SpecialCondition> activeConditions = specialConditionStateService.findByPokemonInPlayId(defender.getId());
        Set<SpecialConditionType> requiredConditions = requiredConditions(context.operation());
        boolean conditionMet = conditionMet(activeConditions, requiredConditions);
        int damageModifier = conditionMet ? context.operation().amount() : 0;

        if (conditionMet) {
            clearConditions(defender, activeConditions, requiredConditions);
        }

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "conditionMet", conditionMet,
                        "damageModifier", damageModifier,
                        "pokemonInPlayId", defender.getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }

    private Set<SpecialConditionType> requiredConditions(AttackEffectOperation operation) {
        if (operation.conditionTypes().isEmpty()
                && (operation.conditionType() == null || operation.conditionType().isBlank())) {
            return Set.of();
        }

        Set<SpecialConditionType> requiredConditions = EnumSet.noneOf(SpecialConditionType.class);
        for (String conditionType : operation.conditionTypes()) {
            requiredConditions.add(conditionType(conditionType));
        }
        if (operation.conditionTypes().isEmpty()) {
            requiredConditions.add(conditionType(operation.conditionType()));
        }
        return Set.copyOf(requiredConditions);
    }

    private boolean conditionMet(List<SpecialCondition> activeConditions, Set<SpecialConditionType> requiredConditions) {
        if (activeConditions == null || activeConditions.isEmpty()) {
            return false;
        }
        if (requiredConditions.isEmpty()) {
            return true;
        }
        for (SpecialCondition activeCondition : activeConditions) {
            if (requiredConditions.contains(activeCondition.getConditionType())) {
                return true;
            }
        }
        return false;
    }

    private void clearConditions(
            PokemonInPlay defender,
            List<SpecialCondition> activeConditions,
            Set<SpecialConditionType> requiredConditions) {
        if (requiredConditions.isEmpty()) {
            specialConditionStateService.deleteByPokemonInPlayId(defender.getId());
            return;
        }
        for (SpecialCondition activeCondition : activeConditions) {
            if (requiredConditions.contains(activeCondition.getConditionType())) {
                specialConditionStateService.delete(activeCondition);
            }
        }
    }

    private SpecialConditionType conditionType(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidGameActionException("Conditional special condition damage effect contains an empty condition type");
        }
        try {
            return SpecialConditionType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidGameActionException("Unsupported special condition type: " + value);
        }
    }
}
