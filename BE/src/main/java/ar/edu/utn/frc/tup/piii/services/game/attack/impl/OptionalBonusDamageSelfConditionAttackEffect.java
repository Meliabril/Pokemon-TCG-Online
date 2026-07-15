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

/**
 * Models attacks such as Bibarel (XY1-107) "Hypno Headbutt": "You may do 30 more damage. If you
 * do, this Pokemon is now Asleep." The bonus damage is an attacker decision made when the attack
 * is declared (same {@code useBonusDamage} payload convention as
 * {@link OptionalBonusRecoilDamageAttackEffect} and {@link OptionalDiscardEnergyBonusDamageAttackEffect}
 * - there is no separate confirmation step because the choice is already final at declaration
 * time). Declining the bonus must not apply the self special condition; only taking it does.
 */
@Service
@RequiredArgsConstructor
public class OptionalBonusDamageSelfConditionAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "OPTIONAL_BONUS_DAMAGE_SELF_CONDITION";
    private static final String USE_BONUS_DAMAGE_PAYLOAD_KEY = "useBonusDamage";

    private final SpecialConditionApplicationService specialConditionApplicationService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        if (!Boolean.TRUE.equals(context.payload().get(USE_BONUS_DAMAGE_PAYLOAD_KEY))) {
            return AttackEffectResult.empty();
        }

        PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();
        SpecialConditionType conditionType = conditionType(context.operation().conditionType());
        return new AttackEffectResult(
                context.operation().amount(),
                false,
                specialConditionApplicationService.applyCondition(
                        context.resolutionContext().gameId(),
                        context.resolutionContext().attackerUserId(),
                        attackerPokemon,
                        conditionType,
                        context.turnNumber(),
                        context.stateVersion()));
    }

    private SpecialConditionType conditionType(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidGameActionException("Optional bonus damage effect must declare a self condition type");
        }

        return SpecialConditionType.valueOf(value.toUpperCase());
    }
}
