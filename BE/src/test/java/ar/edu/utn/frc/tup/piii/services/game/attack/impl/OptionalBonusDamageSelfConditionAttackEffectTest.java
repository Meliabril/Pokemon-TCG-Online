package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.attack.SpecialConditionApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bibarel (XY1-107) "Hypno Headbutt": "You may do 30 more damage. If you do, this Pokemon is now
 * Asleep." The bonus is a declaration-time decision (the {@code useBonusDamage} flag sent with
 * the attack action) - declining it must leave the attacker's own special conditions untouched.
 */
@ExtendWith(MockitoExtension.class)
class OptionalBonusDamageSelfConditionAttackEffectTest {

    @Mock
    private SpecialConditionApplicationService specialConditionApplicationService;

    @Test
    void shouldAddBonusDamageAndPutTheAttackerToSleepWhenBonusIsUsed() {
        OptionalBonusDamageSelfConditionAttackEffect effect =
                new OptionalBonusDamageSelfConditionAttackEffect(specialConditionApplicationService);
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());
        List<GameEventDto> conditionEvents = List.of(event());
        when(specialConditionApplicationService.applyCondition(
                eq(gameId), eq(attackerUserId), eq(attackerPokemon), eq(SpecialConditionType.ASLEEP), eq(3), eq(5)))
                .thenReturn(conditionEvents);

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId, attackerPokemon, true));

        assertThat(result.damageModifier()).isEqualTo(30);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).isEqualTo(conditionEvents);
    }

    @Test
    void shouldDoNothingWhenTheBonusIsDeclined() {
        OptionalBonusDamageSelfConditionAttackEffect effect =
                new OptionalBonusDamageSelfConditionAttackEffect(specialConditionApplicationService);
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());

        AttackEffectResult result = effect.apply(
                context(UUID.randomUUID(), UUID.randomUUID(), attackerPokemon, false));

        assertThat(result.damageModifier()).isZero();
        assertThat(result.events()).isEmpty();
        verify(specialConditionApplicationService, never())
                .applyCondition(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void shouldRequireAConditionTypeOnTheOperation() {
        OptionalBonusDamageSelfConditionAttackEffect effect =
                new OptionalBonusDamageSelfConditionAttackEffect(specialConditionApplicationService);
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), attackerPokemon, null, null, null, null);
        AttackEffectOperation operationWithoutCondition = new AttackEffectOperation(
                "OPTIONAL_BONUS_DAMAGE_SELF_CONDITION",
                AttackEffectPhase.BEFORE_DAMAGE,
                "ATTACKER",
                30,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
        AttackEffectContext context = new AttackEffectContext(
                resolutionContext, null, operationWithoutCondition, Map.of("useBonusDamage", true), 5, 3, 0);

        assertThatThrownBy(() -> effect.apply(context)).isInstanceOf(InvalidGameActionException.class);
    }

    private AttackEffectContext context(
            UUID gameId, UUID attackerUserId, PokemonInPlay attackerPokemon, boolean useBonusDamage) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId, attackerUserId, UUID.randomUUID(), attackerPokemon, null, null, null, null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "OPTIONAL_BONUS_DAMAGE_SELF_CONDITION",
                AttackEffectPhase.BEFORE_DAMAGE,
                "ATTACKER",
                30,
                "ASLEEP",
                CoinRequirement.NONE,
                false,
                0,
                null);
        return new AttackEffectContext(
                resolutionContext, null, operation, Map.of("useBonusDamage", useBonusDamage), 5, 3, 0);
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.STATUS_APPLIED, 1, false, Instant.now(), Map.of());
    }
}
