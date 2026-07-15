package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/*
 * Models attacks like Yveltal-EX's Evil Ball: damage scales with the total
 * Energy attached across BOTH Active Pokemon (not just the attacker), unlike
 * DYNAMIC_ENERGY_COUNT_DAMAGE which only looks at the attacker.
 */
@Service
@RequiredArgsConstructor
public class DynamicDualEnergyCountDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "DYNAMIC_DUAL_ENERGY_COUNT_DAMAGE";

    private final GameEventFactory gameEventFactory;
    private final AttachedEnergyCounter attachedEnergyCounter;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay attacker = context.resolutionContext().attackerPokemon();
        PokemonInPlay defender = context.resolutionContext().defenderPokemon();
        int energyCount = attachedEnergyCounter.countAll(attacker) + attachedEnergyCounter.countAll(defender);
        int damageModifier = energyCount * context.operation().amount();

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "energyCount", energyCount,
                        "damageModifier", damageModifier,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", attacker.getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }
}
