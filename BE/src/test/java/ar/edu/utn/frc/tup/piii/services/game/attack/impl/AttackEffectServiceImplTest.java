package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectDefinition;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttackEffectServiceImplTest {

    private static final String EFFECT_TYPE = "TEST_EFFECT";

    @Mock
    private AttackEffectDefinitionReader attackEffectDefinitionReader;

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private AttackEffect testEffect;

    @Test
    void shouldApplyEffectWhenCoinResultMatchesHeadsRequirement() {
        when(testEffect.supports(any())).thenReturn(true);
        when(testEffect.apply(any())).thenReturn(new AttackEffectResult(10, false, List.of()));
        when(gameRandomService.flipCoin()).thenReturn(true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(operation(CoinRequirement.HEADS, false));

        AttackEffectResult result = service.applyBeforeDamage(
                resolutionContext(), definition, null, Map.of(), 1, 1);

        assertThat(result.damageModifier()).isEqualTo(10);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeCoinResultsArrayActorAndPokemonInCoinFlipEventPayload() {
        when(testEffect.supports(any())).thenReturn(true);
        when(testEffect.apply(any())).thenReturn(new AttackEffectResult(10, false, List.of()));
        when(gameRandomService.flipCoin()).thenReturn(true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(operation(CoinRequirement.HEADS, false));
        AttackResolutionContext context = resolutionContext();

        service.applyBeforeDamage(context, definition, null, Map.of(), 1, 1);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("coinResults")).isEqualTo(List.of("HEADS"));
        assertThat(payload.get("actorPlayerId")).isEqualTo(context.attackerUserId().toString());
        assertThat(payload.get("pokemonInPlayId")).isEqualTo(context.attackerPokemon().getId().toString());
    }

    @Test
    void shouldSkipEffectWhenCoinResultDoesNotMatchHeadsRequirement() {
        when(gameRandomService.flipCoin()).thenReturn(false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(operation(CoinRequirement.HEADS, false));

        AttackEffectResult result = service.applyBeforeDamage(
                resolutionContext(), definition, null, Map.of(), 1, 1);

        assertThat(result.damageModifier()).isZero();
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
        verify(testEffect, never()).apply(any());
    }

    @Test
    void shouldCancelAttackWhenTailsAndCancelOnTailsIsSet() {
        when(gameRandomService.flipCoin()).thenReturn(false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(operation(CoinRequirement.HEADS, true));

        AttackEffectResult result = service.applyBeforeDamage(
                resolutionContext(), definition, null, Map.of(), 1, 1);

        assertThat(result.attackCancelled()).isTrue();
        verify(testEffect, never()).apply(any());
    }

    @Test
    void shouldApplyEffectUnconditionallyWhenCoinRequirementIsNone() {
        when(testEffect.supports(any())).thenReturn(true);
        when(testEffect.apply(any())).thenReturn(new AttackEffectResult(5, false, List.of()));

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(operation(CoinRequirement.NONE, false));

        AttackEffectResult result = service.applyBeforeDamage(
                resolutionContext(), definition, null, Map.of(), 1, 1);

        assertThat(result.damageModifier()).isEqualTo(5);
        verify(gameRandomService, never()).flipCoin();
    }

    @Test
    void shouldPropagateIgnoreResistanceFromEffects() {
        when(testEffect.supports(any())).thenReturn(true);
        when(testEffect.apply(any())).thenReturn(new AttackEffectResult(0, false, List.of(), true));

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(operation(CoinRequirement.NONE, false));

        AttackEffectResult result = service.applyBeforeDamage(
                resolutionContext(), definition, null, Map.of(), 1, 1);

        assertThat(result.ignoreResistance()).isTrue();
    }

    @Test
    void shouldShareSingleCoinFlipAcrossOperationsWithSameGroupKey() {
        when(testEffect.supports(any())).thenReturn(true);
        when(testEffect.apply(any())).thenReturn(new AttackEffectResult(0, false, List.of()));
        when(gameRandomService.flipCoin()).thenReturn(true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(
                operation(CoinRequirement.HEADS, "combo"),
                operation(CoinRequirement.HEADS, "combo"));

        AttackEffectResult result = service.applyBeforeDamage(
                resolutionContext(), definition, null, Map.of(), 1, 1);

        verify(gameRandomService, times(1)).flipCoin();
        verify(testEffect, times(2)).apply(any());
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldApplyExactlyOneBranchWhenGroupKeyOperationsRequireOppositeResults() {
        when(testEffect.supports(any())).thenReturn(true);
        when(testEffect.apply(any())).thenReturn(new AttackEffectResult(0, false, List.of()));
        when(gameRandomService.flipCoin()).thenReturn(false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(
                operation(CoinRequirement.HEADS, "combo"),
                operation(CoinRequirement.TAILS, "combo"));

        AttackEffectResult result = service.applyBeforeDamage(
                resolutionContext(), definition, null, Map.of(), 1, 1);

        verify(gameRandomService, times(1)).flipCoin();
        verify(testEffect, times(1)).apply(any());
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldPropagateChoicePlayerIdFromEffectResult() {
        UUID defenderUserId = UUID.randomUUID();
        when(testEffect.supports(any())).thenReturn(true);
        when(testEffect.apply(any())).thenReturn(AttackEffectResult.requiringChoice(
                "SELECT_HAND_CARDS_TO_DISCARD", Map.of("discardCount", 1), List.of(), defenderUserId));

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(operation(CoinRequirement.NONE, false));

        AttackEffectResult result = service.applyBeforeDamage(
                resolutionContext(), definition, null, Map.of(), 1, 1);

        assertThat(result.choiceRequired()).isTrue();
        assertThat(result.choiceType()).isEqualTo("SELECT_HAND_CARDS_TO_DISCARD");
        assertThat(result.choicePlayerId()).isEqualTo(defenderUserId);
    }

    @Test
    void shouldCarryIgnoreWeaknessAndResistanceFlagsFromAppliedEffects() {
        when(testEffect.supports(any())).thenReturn(true);
        when(testEffect.apply(any())).thenReturn(AttackEffectResult.ignoringDefenderEffects(List.of()));

        AttackEffectServiceImpl service = service();
        AttackEffectDefinition definition = definitionWith(operation(CoinRequirement.NONE, false));

        AttackEffectResult result = service.applyBeforeDamage(
                resolutionContext(), definition, null, Map.of(), 1, 1);

        assertThat(result.ignoreWeakness()).isTrue();
        assertThat(result.ignoreResistance()).isTrue();
        assertThat(result.ignoreDefenderEffects()).isTrue();
    }

    private AttackEffectServiceImpl service() {
        return new AttackEffectServiceImpl(
                attackEffectDefinitionReader, List.of(testEffect), gameRandomService, gameEventFactory);
    }

    private AttackEffectOperation operation(CoinRequirement coinRequirement, boolean cancelOnTails) {
        return new AttackEffectOperation(
                EFFECT_TYPE,
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                10,
                null,
                coinRequirement,
                cancelOnTails,
                0,
                null);
    }

    private AttackEffectOperation operation(CoinRequirement coinRequirement, String coinGroupKey) {
        return new AttackEffectOperation(
                EFFECT_TYPE,
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                10,
                null,
                coinRequirement,
                false,
                0,
                coinGroupKey);
    }

    private AttackEffectDefinition definitionWith(AttackEffectOperation operation) {
        return new AttackEffectDefinition("xy1-test", "Test Attack", false, List.of(operation));
    }

    private AttackEffectDefinition definitionWith(AttackEffectOperation... operations) {
        return new AttackEffectDefinition("xy1-test", "Test Attack", false, List.of(operations));
    }

    private AttackResolutionContext resolutionContext() {
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());
        return new AttackResolutionContext(
                UUID.randomUUID(), UUID.randomUUID(), null, attackerPokemon, null, null, null, null);
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
