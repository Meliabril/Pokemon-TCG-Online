package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
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
class DynamicEnergyCountDamageAttackEffectTest {

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private AttachedEnergyCounter attachedEnergyCounter;

    @Test
    void shouldScaleDamageWithMatchingAttachedEnergyCount() {
        DynamicEnergyCountDamageAttackEffect effect = new DynamicEnergyCountDamageAttackEffect(
                gameEventFactory,
                attachedEnergyCounter);
        PokemonInPlay attacker = attacker();
        when(attachedEnergyCounter.countByType(attacker, "Water")).thenReturn(3);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(attacker, 10));

        assertThat(result.damageModifier()).isEqualTo(60);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldAddBlazeBallDamageForEachAttachedFireEnergy() {
        DynamicEnergyCountDamageAttackEffect effect = new DynamicEnergyCountDamageAttackEffect(
                gameEventFactory,
                attachedEnergyCounter);
        PokemonInPlay attacker = attacker();
        when(attachedEnergyCounter.countByType(attacker, "Fire")).thenReturn(4);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(attacker, 50, "Fire"));

        assertThat(result.damageModifier()).isEqualTo(80);
    }

    @Test
    void shouldAddNoDamageWhenNoMatchingEnergyIsAttached() {
        DynamicEnergyCountDamageAttackEffect effect = new DynamicEnergyCountDamageAttackEffect(
                gameEventFactory,
                attachedEnergyCounter);
        PokemonInPlay attacker = attacker();
        when(attachedEnergyCounter.countByType(attacker, "Water")).thenReturn(0);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(attacker, 10));

        assertThat(result.damageModifier()).isEqualTo(0);
    }

    private AttackEffectContext context(PokemonInPlay attacker, int baseDamage) {
        return context(attacker, baseDamage, "Water");
    }

    private AttackEffectContext context(PokemonInPlay attacker, int baseDamage, String energyType) {
        AttackEffectOperation operation = new AttackEffectOperation(
                "DYNAMIC_ENERGY_COUNT_DAMAGE",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                20,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                null,
                energyType);
        Attack attack = new Attack();
        attack.setBaseDamage(baseDamage);
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                attacker,
                null,
                null,
                null,
                attack);
        return new AttackEffectContext(resolutionContext, null, operation, Map.of(), 1, 1, 0);
    }

    private PokemonInPlay attacker() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setId(UUID.randomUUID());
        return attacker;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
