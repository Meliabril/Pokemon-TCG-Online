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

/**
 * Models attacks such as Dodrio (XY1-99) "Rage" ("Furia"): "Este ataque hace 10 puntos de dano
 * mas por cada contador de dano en este Pokemon" - the attacking Pokemon's OWN current damage
 * counters (not the defender's) add a flat bonus to this attack's damage. The bonus flows back
 * into {@code damageModifier}, so it is computed BEFORE_DAMAGE and goes through the normal
 * Weakness/Resistance pipeline together with the attack's base damage, same as the real card.
 */
@Service
@RequiredArgsConstructor
public class SelfDamageCountersBonusAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "SELF_DAMAGE_COUNTERS_BONUS";

    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay attacker = context.resolutionContext().attackerPokemon();
        int damageCounters = attacker.getDamageCounters() == null ? 0 : attacker.getDamageCounters();
        int damageModifier = damageCounters * context.operation().amount();

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "damageCounters", damageCounters,
                        "damageModifier", damageModifier,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", attacker.getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }
}
