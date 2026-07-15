package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Malamar XY1-75 "Puncture" ignores Resistance entirely. Keep both contract checks:
 * the effect must advertise support only for the matching operation type, and when applied
 * it must unconditionally flag the attack as ignoring Resistance without adding other side effects.
 */
class IgnoreResistanceAttackEffectTest {

    private final IgnoreResistanceAttackEffect effect = new IgnoreResistanceAttackEffect();

    @Test
    void shouldSupportOnlyTheIgnoreResistanceOperationType() {
        assertThat(effect.supports(operation("IGNORE_RESISTANCE"))).isTrue();
        assertThat(effect.supports(operation("SOMETHING_ELSE"))).isFalse();
        assertThat(effect.supports(null)).isFalse();
    }

    @Test
    void shouldFlagDamageCalculationToIgnoreResistanceWithoutChangingAnythingElse() {
        AttackEffectResult result = effect.apply(context());

        assertThat(result.ignoreResistance()).isTrue();
        assertThat(result.damageModifier()).isZero();
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).isEmpty();
        assertThat(result.choiceRequired()).isFalse();
    }

    private AttackEffectContext context() {
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(), null, null, attackerPokemon, null, null, null, null);
        return new AttackEffectContext(resolutionContext, null, operation("IGNORE_RESISTANCE"), Map.of(), 1, 1, 0);
    }

    private AttackEffectOperation operation(String type) {
        return new AttackEffectOperation(
                type,
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
    }
}
