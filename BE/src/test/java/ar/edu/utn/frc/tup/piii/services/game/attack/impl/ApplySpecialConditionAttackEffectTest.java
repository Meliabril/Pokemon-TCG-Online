package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplySpecialConditionAttackEffectTest {

    @Mock
    private SpecialConditionApplicationService specialConditionApplicationService;

    @Test
    void shouldApplyConditionToDefender() {
        ApplySpecialConditionAttackEffect effect = newEffect();
        AttackEffectContext context = context(operation("POISONED", "DEFENDER"));
        GameEventDto expectedEvent = new GameEventDto(
                UUID.randomUUID(),
                context.resolutionContext().gameId(),
                null,
                context.stateVersion(),
                false,
                null,
                Map.of("conditionType", "POISONED"));
        when(specialConditionApplicationService.applyCondition(
                any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(expectedEvent));

        AttackEffectResult result = effect.apply(context);

        assertThat(result.damageModifier()).isZero();
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).containsExactly(expectedEvent);
        verify(specialConditionApplicationService).applyCondition(
                eq(context.resolutionContext().gameId()),
                eq(context.resolutionContext().attackerUserId()),
                eq(context.targetPokemon()),
                eq(SpecialConditionType.POISONED),
                eq(context.turnNumber()),
                eq(context.stateVersion()));
    }

    @Test
    void shouldApplyConditionToAttackerWhenOperationTargetIsAttacker() {
        ApplySpecialConditionAttackEffect effect = newEffect();
        PokemonInPlay attacker = pokemon("attacker");
        AttackEffectContext context = new AttackEffectContext(
                new AttackResolutionContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        attacker,
                        pokemon("defender"),
                        card("xy1-42"),
                        card("defender-card"),
                        attack("Nuzzle")),
                pokemon("defender"),
                operation("PARALYZED", "ATTACKER"),
                Map.of(),
                5,
                3,
                0);
        when(specialConditionApplicationService.applyCondition(
                any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        effect.apply(context);

        verify(specialConditionApplicationService).applyCondition(
                eq(context.resolutionContext().gameId()),
                eq(context.resolutionContext().attackerUserId()),
                eq(attacker),
                eq(SpecialConditionType.PARALYZED),
                eq(context.turnNumber()),
                eq(context.stateVersion()));
    }

    @Test
    void shouldThrowWhenConditionTypeIsMissing() {
        ApplySpecialConditionAttackEffect effect = newEffect();
        AttackEffectContext context = context(operation(null, "DEFENDER"));

        assertThatThrownBy(() -> effect.apply(context))
                .isInstanceOf(InvalidGameActionException.class);
    }

    @Test
    void shouldOnlySupportApplySpecialConditionEffectType() {
        ApplySpecialConditionAttackEffect effect = newEffect();

        assertThat(effect.supports(operation("POISONED", "DEFENDER"))).isTrue();
        assertThat(effect.supports(null)).isFalse();
        assertThat(effect.supports(new AttackEffectOperation(
                "DRAW_CARDS",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null))).isFalse();
    }

    private ApplySpecialConditionAttackEffect newEffect() {
        return new ApplySpecialConditionAttackEffect(specialConditionApplicationService);
    }

    private AttackEffectContext context(AttackEffectOperation operation) {
        return new AttackEffectContext(
                new AttackResolutionContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        pokemon("attacker"),
                        pokemon("defender"),
                        card("xy1-42"),
                        card("defender-card"),
                        attack("Nuzzle")),
                pokemon("defender"),
                operation,
                Map.of(),
                5,
                3,
                0);
    }

    private AttackEffectOperation operation(String conditionType, String target) {
        return new AttackEffectOperation(
                "APPLY_SPECIAL_CONDITION",
                AttackEffectPhase.AFTER_DAMAGE,
                target,
                0,
                conditionType,
                CoinRequirement.NONE,
                false,
                0,
                null);
    }

    private PokemonInPlay pokemon(String tag) {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setOwnerUserId(UUID.randomUUID());
        pokemon.setSlotPosition(0);
        pokemon.setDamageCounters(0);
        pokemon.setEnteredPlayTurn(1);
        return pokemon;
    }

    private Card card(String externalId) {
        Card card = new Card();
        card.setExternalId(externalId);
        card.setName("Card " + externalId);
        return card;
    }

    private Attack attack(String name) {
        Attack attack = new Attack();
        attack.setName(name);
        return attack;
    }
}
