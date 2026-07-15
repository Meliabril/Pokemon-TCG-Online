package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionalDefenderSpecialConditionDamageAndCureAttackEffectTest {

    @Mock
    private SpecialConditionStateService specialConditionStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldAddDamageAndClearAsleepWhenDefenderIsAsleep() {
        ConditionalDefenderSpecialConditionDamageAndCureAttackEffect effect = newEffect();
        PokemonInPlay defender = pokemon();
        SpecialCondition asleep = condition(defender, SpecialConditionType.ASLEEP);
        SpecialCondition poisoned = condition(defender, SpecialConditionType.POISONED);
        when(specialConditionStateService.findByPokemonInPlayId(defender.getId()))
                .thenReturn(List.of(asleep, poisoned));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(defender, wakeUpSlapOperation()));

        assertThat(result.damageModifier()).isEqualTo(60);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
        verify(specialConditionStateService).delete(asleep);
        verify(specialConditionStateService, never()).delete(poisoned);
        verify(specialConditionStateService, never()).deleteByPokemonInPlayId(defender.getId());
    }

    @Test
    void shouldNotAddDamageOrClearConditionsWhenDefenderIsNotAsleep() {
        ConditionalDefenderSpecialConditionDamageAndCureAttackEffect effect = newEffect();
        PokemonInPlay defender = pokemon();
        when(specialConditionStateService.findByPokemonInPlayId(defender.getId()))
                .thenReturn(List.of(condition(defender, SpecialConditionType.POISONED)));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(defender, wakeUpSlapOperation()));

        assertThat(result.damageModifier()).isZero();
        assertThat(result.events()).hasSize(1);
        verify(specialConditionStateService, never()).deleteByPokemonInPlayId(any());
        verify(specialConditionStateService, never()).delete(any());
    }

    @Test
    void shouldOnlyClearMatchingConfiguredConditionsWhenConditionTypesAreConfigured() {
        ConditionalDefenderSpecialConditionDamageAndCureAttackEffect effect = newEffect();
        PokemonInPlay defender = pokemon();
        SpecialCondition asleep = condition(defender, SpecialConditionType.ASLEEP);
        SpecialCondition poisoned = condition(defender, SpecialConditionType.POISONED);
        when(specialConditionStateService.findByPokemonInPlayId(defender.getId()))
                .thenReturn(List.of(asleep, poisoned));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(defender, configuredConditionOperation()));

        assertThat(result.damageModifier()).isEqualTo(60);
        verify(specialConditionStateService).delete(asleep);
        verify(specialConditionStateService, never()).delete(poisoned);
        verify(specialConditionStateService, never()).deleteByPokemonInPlayId(any());
    }

    @Test
    void shouldOnlySupportConditionalSpecialConditionDamageAndCureOperations() {
        ConditionalDefenderSpecialConditionDamageAndCureAttackEffect effect = newEffect();

        assertThat(effect.supports(wakeUpSlapOperation())).isTrue();
        assertThat(effect.supports(null)).isFalse();
        assertThat(effect.supports(new AttackEffectOperation(
                "CURE_SPECIAL_CONDITIONS",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null))).isFalse();
    }

    private ConditionalDefenderSpecialConditionDamageAndCureAttackEffect newEffect() {
        return new ConditionalDefenderSpecialConditionDamageAndCureAttackEffect(
                specialConditionStateService,
                gameEventFactory);
    }

    private AttackEffectContext context(PokemonInPlay defender, AttackEffectOperation operation) {
        return new AttackEffectContext(
                new AttackResolutionContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        pokemon(),
                        defender,
                        card(),
                        card(),
                        attack()),
                defender,
                operation,
                Map.of(),
                5,
                3,
                0);
    }

    private AttackEffectOperation wakeUpSlapOperation() {
        return new AttackEffectOperation(
                "CONDITIONAL_DEFENDER_SPECIAL_CONDITION_DAMAGE_AND_CURE",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                60,
                "ASLEEP",
                CoinRequirement.NONE,
                false,
                0,
                null);
    }

    private AttackEffectOperation configuredConditionOperation() {
        return new AttackEffectOperation(
                "CONDITIONAL_DEFENDER_SPECIAL_CONDITION_DAMAGE_AND_CURE",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                60,
                "ASLEEP",
                CoinRequirement.NONE,
                false,
                0,
                null);
    }

    private SpecialCondition condition(PokemonInPlay pokemon, SpecialConditionType conditionType) {
        SpecialCondition condition = new SpecialCondition();
        condition.setId(UUID.randomUUID());
        condition.setPokemonInPlay(pokemon);
        condition.setConditionType(conditionType);
        return condition;
    }

    private PokemonInPlay pokemon() {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setOwnerUserId(UUID.randomUUID());
        return pokemon;
    }

    private Card card() {
        Card card = new Card();
        card.setExternalId("xy1-67");
        card.setName("Conkeldurr");
        return card;
    }

    private Attack attack() {
        Attack attack = new Attack();
        attack.setName("Wake-Up Slap");
        return attack;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 3, false, Instant.now(), Map.of());
    }
}
