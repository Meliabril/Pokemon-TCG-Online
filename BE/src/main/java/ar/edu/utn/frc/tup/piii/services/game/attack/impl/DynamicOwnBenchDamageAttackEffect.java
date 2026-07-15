package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DynamicOwnBenchDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "DYNAMIC_OWN_BENCH_DAMAGE";

    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        int benchCount = pokemonInPlayStateService.countBenchPokemon(
                context.resolutionContext().gameId(),
                context.resolutionContext().attackerUserId());
        int targetDamage = context.operation().amount() * benchCount;
        int baseDamage = baseDamage(context.resolutionContext().selectedAttack());
        return new AttackEffectResult(targetDamage - baseDamage, false, List.of());
    }

    private int baseDamage(Attack attack) {
        if (attack == null || attack.getBaseDamage() == null) {
            return 0;
        }
        return attack.getBaseDamage();
    }
}
