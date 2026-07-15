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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Dodrio (XY1-99) "Rage": "This attack does 10 more damage for each damage counter on this
 * Pokemon" - the bonus must come from the ATTACKER's own current damage counters, not the
 * defender's, and must scale by the operation's configured amount (10 per counter).
 */
@ExtendWith(MockitoExtension.class)
class SelfDamageCountersBonusAttackEffectTest {

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldAddTenDamagePerExistingDamageCounterOnTheAttacker() {
        SelfDamageCountersBonusAttackEffect effect = new SelfDamageCountersBonusAttackEffect(gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(damageCounters(3)));

        assertThat(result.damageModifier()).isEqualTo(30);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldAddNoBonusWhenTheAttackerHasNoDamageCounters() {
        SelfDamageCountersBonusAttackEffect effect = new SelfDamageCountersBonusAttackEffect(gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(damageCounters(0)));

        assertThat(result.damageModifier()).isZero();
    }

    @Test
    void shouldTreatNullDamageCountersAsZero() {
        SelfDamageCountersBonusAttackEffect effect = new SelfDamageCountersBonusAttackEffect(gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setId(UUID.randomUUID());
        attacker.setDamageCounters(null);

        AttackEffectResult result = effect.apply(context(attacker));

        assertThat(result.damageModifier()).isZero();
    }

    @Test
    void shouldSupportOnlySelfDamageCountersBonusOperationType() {
        SelfDamageCountersBonusAttackEffect effect = new SelfDamageCountersBonusAttackEffect(gameEventFactory);

        assertThat(effect.supports(operation())).isTrue();
        assertThat(effect.supports(null)).isFalse();
    }

    private PokemonInPlay damageCounters(int count) {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setId(UUID.randomUUID());
        attacker.setDamageCounters(count);
        return attacker;
    }

    private AttackEffectContext context(PokemonInPlay attacker) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                attacker,
                null,
                null,
                null,
                null);
        return new AttackEffectContext(resolutionContext, null, operation(), Map.of(), 1, 1, 0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "SELF_DAMAGE_COUNTERS_BONUS",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                10,
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
