package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChooseSpecialConditionAttackEffect implements AttackEffect {

    public static final String EFFECT_TYPE = "CHOOSE_SPECIAL_CONDITION";
    public static final String CHOICE_TYPE = "CHOOSE_SPECIAL_CONDITION";
    public static final String CONDITION_TYPES_KEY = "conditionTypes";

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        List<String> conditionTypes = allowedConditionTypes(context.operation());
        return AttackEffectResult.requiringChoice(
                CHOICE_TYPE,
                Map.of(
                        "defenderPokemonInPlayId", context.targetPokemon().getId().toString(),
                        CONDITION_TYPES_KEY, conditionTypes),
                List.of());
    }

    private List<String> allowedConditionTypes(AttackEffectOperation operation) {
        List<String> configuredTypes = operation.conditionTypes();
        if (configuredTypes.isEmpty() && operation.conditionType() != null && !operation.conditionType().isBlank()) {
            configuredTypes = List.of(operation.conditionType());
        }
        if (configuredTypes.isEmpty()) {
            throw new InvalidGameActionException("Choice special condition effect must declare condition types");
        }

        List<String> conditionTypes = new ArrayList<>();
        for (String configuredType : configuredTypes) {
            String conditionType = normalizedConditionType(configuredType);
            if (!conditionTypes.contains(conditionType)) {
                conditionTypes.add(conditionType);
            }
        }
        return List.copyOf(conditionTypes);
    }

    private String normalizedConditionType(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidGameActionException("Choice special condition effect contains an empty condition type");
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        try {
            return SpecialConditionType.valueOf(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new InvalidGameActionException("Unsupported special condition choice: " + value);
        }
    }
}
