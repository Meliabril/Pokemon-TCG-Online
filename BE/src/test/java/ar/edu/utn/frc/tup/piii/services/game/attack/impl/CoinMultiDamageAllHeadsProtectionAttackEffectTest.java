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
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoinMultiDamageAllHeadsProtectionAttackEffectTest {

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Test
    void shouldApplyDamageAndProtectionWhenAllCoinsAreHeads() {
        CoinMultiDamageAllHeadsProtectionAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(true, true, true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        PokemonInPlay attacker = attacker();
        AttackEffectResult result = effect.apply(context(attacker, 3, attackWithBaseDamage(40)));

        assertThat(result.damageModifier()).isEqualTo(80);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
        assertThat(attacker.getDamageProtectionTurn()).isEqualTo(4);
        verify(pokemonInPlayStateService).save(attacker);
    }

    @Test
    void shouldApplyDamageWithoutProtectionWhenAnyCoinIsTails() {
        CoinMultiDamageAllHeadsProtectionAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(true, false, true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        PokemonInPlay attacker = attacker();
        AttackEffectResult result = effect.apply(context(attacker, 5, attackWithBaseDamage(40)));

        assertThat(result.damageModifier()).isEqualTo(40);
        assertThat(attacker.getDamageProtectionTurn()).isNull();
        verifyNoInteractions(pokemonInPlayStateService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeActorAndPokemonIdentifiersInEventPayload() {
        CoinMultiDamageAllHeadsProtectionAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(true, true, true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        PokemonInPlay attacker = attacker();
        AttackEffectContext context = context(attacker, 3, attackWithBaseDamage(40));
        effect.apply(context);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("actorPlayerId")).isEqualTo(context.resolutionContext().attackerUserId().toString());
        assertThat(payload.get("pokemonInPlayId")).isEqualTo(context.resolutionContext().attackerPokemon().getId().toString());
    }

    private CoinMultiDamageAllHeadsProtectionAttackEffect effect() {
        return new CoinMultiDamageAllHeadsProtectionAttackEffect(
                gameRandomService,
                gameEventFactory,
                pokemonInPlayStateService);
    }

    private AttackEffectContext context(PokemonInPlay attacker, int turnNumber, Attack attack) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                attacker,
                null,
                null,
                null,
                attack);
        return new AttackEffectContext(
                resolutionContext,
                null,
                operation(),
                Map.of(),
                1,
                turnNumber,
                0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "COIN_MULTI_DAMAGE_ALL_HEADS_PROTECTION",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                40,
                null,
                CoinRequirement.NONE,
                false,
                3,
                null);
    }

    private PokemonInPlay attacker() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setId(UUID.randomUUID());
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
