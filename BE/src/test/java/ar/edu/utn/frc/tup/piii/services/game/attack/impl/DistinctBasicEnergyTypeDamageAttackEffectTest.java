package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
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
class DistinctBasicEnergyTypeDamageAttackEffectTest {

    @Mock
    private AttachedEnergyCounter attachedEnergyCounter;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldAddDamageForEachDistinctBasicEnergyTypeAttachedToAttacker() {
        DistinctBasicEnergyTypeDamageAttackEffect effect =
                new DistinctBasicEnergyTypeDamageAttackEffect(attachedEnergyCounter, gameEventFactory);
        PokemonInPlay attacker = pokemon();
        when(attachedEnergyCounter.countDistinctBasicEnergyTypes(attacker)).thenReturn(3);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(attacker, operation()));

        assertThat(result.damageModifier()).isEqualTo(90);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldAddNoDamageWhenAttackerHasNoBasicEnergyTypesAttached() {
        DistinctBasicEnergyTypeDamageAttackEffect effect =
                new DistinctBasicEnergyTypeDamageAttackEffect(attachedEnergyCounter, gameEventFactory);
        PokemonInPlay attacker = pokemon();
        when(attachedEnergyCounter.countDistinctBasicEnergyTypes(attacker)).thenReturn(0);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(attacker, operation()));

        assertThat(result.damageModifier()).isZero();
    }

    @Test
    void shouldOnlySupportDistinctBasicEnergyTypeDamageOperations() {
        DistinctBasicEnergyTypeDamageAttackEffect effect =
                new DistinctBasicEnergyTypeDamageAttackEffect(attachedEnergyCounter, gameEventFactory);

        assertThat(effect.supports(operation())).isTrue();
        assertThat(effect.supports(null)).isFalse();
        assertThat(effect.supports(new AttackEffectOperation(
                "DYNAMIC_ENERGY_COUNT_DAMAGE",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                30,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null))).isFalse();
    }

    private AttackEffectContext context(PokemonInPlay attacker, AttackEffectOperation operation) {
        return new AttackEffectContext(
                new AttackResolutionContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        attacker,
                        pokemon(),
                        card(),
                        card(),
                        attack()),
                null,
                operation,
                Map.of(),
                5,
                3,
                0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "DISTINCT_BASIC_ENERGY_TYPE_DAMAGE",
                AttackEffectPhase.BEFORE_DAMAGE,
                "ATTACKER",
                30,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
    }

    private PokemonInPlay pokemon() {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setOwnerUserId(UUID.randomUUID());
        return pokemon;
    }

    private Card card() {
        Card card = new Card();
        card.setExternalId("xy1-17");
        card.setName("Vivillon");
        return card;
    }

    private Attack attack() {
        Attack attack = new Attack();
        attack.setName("Colorful Wind");
        return attack;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 3, false, Instant.now(), Map.of());
    }
}
