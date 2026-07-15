package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IgnoreDefenderEffectsAttackEffectTest {

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldRequestDamageCalculationWithoutWeaknessResistanceAndDefenderEffects() {
        IgnoreDefenderEffectsAttackEffect effect = new IgnoreDefenderEffectsAttackEffect(gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context());

        assertThat(result.damageModifier()).isZero();
        assertThat(result.ignoreWeakness()).isTrue();
        assertThat(result.ignoreResistance()).isTrue();
        assertThat(result.ignoreDefenderEffects()).isTrue();
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldSupportOnlyIgnoreDefenderEffectsOperations() {
        IgnoreDefenderEffectsAttackEffect effect = new IgnoreDefenderEffectsAttackEffect(gameEventFactory);

        assertThat(effect.supports(operation(IgnoreDefenderEffectsAttackEffect.EFFECT_TYPE))).isTrue();
        assertThat(effect.supports(operation("IGNORE_WEAKNESS_RESISTANCE"))).isFalse();
        assertThat(effect.supports(null)).isFalse();
    }

    private AttackEffectContext context() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setId(UUID.randomUUID());
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                attacker,
                null,
                null,
                null,
                null);
        return new AttackEffectContext(resolutionContext, null, operation(IgnoreDefenderEffectsAttackEffect.EFFECT_TYPE), Map.of(), 1, 1, 0);
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

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
