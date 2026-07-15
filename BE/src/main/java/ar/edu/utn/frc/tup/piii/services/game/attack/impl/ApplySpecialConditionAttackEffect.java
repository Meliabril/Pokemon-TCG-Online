package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.SpecialConditionApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplySpecialConditionAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "APPLY_SPECIAL_CONDITION";

    private final SpecialConditionApplicationService specialConditionApplicationService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay targetPokemon = context.targetPokemon();
        if ("ATTACKER".equals(context.operation().target())) {
            targetPokemon = context.resolutionContext().attackerPokemon();
        }

        SpecialConditionType conditionType = conditionType(context.operation().conditionType());
        return new AttackEffectResult(
                0,
                false,
                specialConditionApplicationService.applyCondition(
                        context.resolutionContext().gameId(),
                        context.resolutionContext().attackerUserId(),
                        targetPokemon,
                        conditionType,
                        context.turnNumber(),
                        context.stateVersion()));
    }

    private SpecialConditionType conditionType(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidGameActionException("Attack special condition effect must declare a condition type");
        }

        return SpecialConditionType.valueOf(value.toUpperCase());
    }
}
