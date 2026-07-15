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
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicCoinDamageAttackEffectTest {

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private AttachedEnergyCounter attachedEnergyCounter;

    @Test
    void shouldFlipOneCoinPerAttackerDamageCounter() {
        DynamicCoinDamageAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(true, false, true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        PokemonInPlay attacker = attackerWithDamageCounters(3);
        AttackEffectResult result = effect.apply(context(
                attacker,
                attackWithBaseDamage(30),
                operation("ATTACKER_DAMAGE_COUNTERS", null, 30)));

        assertThat(result.damageModifier()).isEqualTo(30);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldCompensateImportedBaseDamageWhenAllDynamicCoinsAreTails() {
        DynamicCoinDamageAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(false, false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        PokemonInPlay attacker = attackerWithDamageCounters(2);
        AttackEffectResult result = effect.apply(context(
                attacker,
                attackWithBaseDamage(30),
                operation("ATTACKER_DAMAGE_COUNTERS", null, 30)));

        assertThat(result.damageModifier()).isEqualTo(-30);
    }

    @Test
    void shouldFlipOneCoinPerMatchingAttachedEnergyType() {
        DynamicCoinDamageAttackEffect effect = effect();
        PokemonInPlay attacker = attackerWithDamageCounters(0);
        when(attachedEnergyCounter.countByType(attacker, "Fighting")).thenReturn(2);
        when(gameRandomService.flipCoin()).thenReturn(true, true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(
                attacker,
                attackWithBaseDamage(50),
                operation("ATTACKER_ATTACHED_ENERGY_TYPE", "Fighting", 50)));

        assertThat(result.damageModifier()).isEqualTo(50);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeActorAndPokemonIdentifiersInEventPayload() {
        DynamicCoinDamageAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(true, false, true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        PokemonInPlay attacker = attackerWithDamageCounters(3);
        AttackEffectContext context = context(attacker, attackWithBaseDamage(30), operation("ATTACKER_DAMAGE_COUNTERS", null, 30));
        effect.apply(context);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("actorPlayerId")).isEqualTo(context.resolutionContext().attackerUserId().toString());
        assertThat(payload.get("pokemonInPlayId")).isEqualTo(context.resolutionContext().attackerPokemon().getId().toString());
    }

    private DynamicCoinDamageAttackEffect effect() {
        return new DynamicCoinDamageAttackEffect(
                gameRandomService,
                gameEventFactory,
                attachedEnergyCounter);
    }

    private AttackEffectOperation operation(String dynamicCoinCountSource, String energyType, int amount) {
        return new AttackEffectOperation(
                "DYNAMIC_COIN_DAMAGE",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                amount,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                dynamicCoinCountSource,
                energyType);
    }

    private AttackEffectContext context(PokemonInPlay attacker, Attack attack, AttackEffectOperation operation) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(), UUID.randomUUID(), null, attacker, null, null, null, attack);
        return new AttackEffectContext(resolutionContext, null, operation, Map.of(), 1, 1, 0);
    }

    private PokemonInPlay attackerWithDamageCounters(int damageCounters) {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setId(UUID.randomUUID());
        attacker.setDamageCounters(damageCounters);
        return attacker;
    }

    private Attack attackWithBaseDamage(int baseDamage) {
        Attack attack = new Attack();
        attack.setBaseDamage(baseDamage);
        return attack;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
