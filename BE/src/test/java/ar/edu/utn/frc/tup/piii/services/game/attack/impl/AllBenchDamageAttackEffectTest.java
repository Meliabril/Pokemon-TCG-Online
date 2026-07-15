package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the bench-knockout bug report: ALL_BENCH_DAMAGE attacks (e.g. Dugtrio's "Earthquake",
 * which damages the attacker's own bench, or any future "hit the opponent's whole bench" attack)
 * applied damage to Benched Pokemon but never checked whether that damage was lethal. A Benched
 * Pokemon left at 0 HP stayed on the bench forever: no discard, no prize, no promotion handling.
 * {@link AllBenchDamageAttackEffect} must resolve a knockout for every Bench Pokemon it damages,
 * the same way {@link DamageCountersAttackEffect} already does for its own non-primary targets.
 */
@ExtendWith(MockitoExtension.class)
class AllBenchDamageAttackEffectTest {

    private static final String OWN_BENCH_TARGET = "OWN_BENCH";
    private static final String OPPONENT_BENCH_TARGET = "OPPONENT_BENCH";

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;
    @Mock
    private DamageApplicationService damageApplicationService;
    @Mock
    private GameEventFactory gameEventFactory;
    @Mock
    private CombatResolutionService combatResolutionService;

    private AllBenchDamageAttackEffect effect;

    private final UUID gameId = UUID.randomUUID();
    private final UUID attackerUserId = UUID.randomUUID();
    private final UUID defenderUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        effect = new AllBenchDamageAttackEffect(
                pokemonInPlayStateService, damageApplicationService, gameEventFactory, combatResolutionService);
        lenient().when(gameEventFactory.publicEvent(any(), any(), any(Integer.class), any()))
                .thenReturn(mockEvent());
    }

    @Test
    void earthquakeShouldKnockOutOwnBenchPokemonAtZeroHpAndRewardTheOpponent() {
        PokemonInPlay attackerActive = pokemon(attackerUserId, 0);
        PokemonInPlay defenderActive = pokemon(defenderUserId, 0);
        PokemonInPlay lethalBench = pokemon(attackerUserId, 1);
        PokemonInPlay survivingBench = pokemon(attackerUserId, 2);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(resolutionContext, defenderActive, earthquakeOperation());

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(attackerActive, lethalBench, survivingBench));
        when(damageApplicationService.applyDamage(lethalBench, 10)).thenReturn(2);
        when(damageApplicationService.applyDamage(survivingBench, 10)).thenReturn(1);

        GameEventDto prizeEvent = mockEvent();
        when(combatResolutionService.resolveKnockoutIfNeeded(
                eq(gameId), eq(attackerUserId), eq(defenderUserId), eq(lethalBench),
                any(), any(Integer.class), any(Integer.class), eq("ALL_BENCH_DAMAGE")))
                .thenReturn(new CombatResolutionService.CombatResolutionResult(true, false, false, null, List.of(prizeEvent)));
        when(combatResolutionService.resolveKnockoutIfNeeded(
                eq(gameId), eq(attackerUserId), eq(defenderUserId), eq(survivingBench),
                any(), any(Integer.class), any(Integer.class), eq("ALL_BENCH_DAMAGE")))
                .thenReturn(CombatResolutionService.CombatResolutionResult.noKnockout());

        AttackEffectResult result = effect.apply(context);

        // Earthquake's bench damage is self-inflicted: the lethal hit belongs to the attacker's
        // own Pokemon, so the prize must go to the opponent, never back to the attacker.
        verify(combatResolutionService).resolveKnockoutIfNeeded(
                gameId, attackerUserId, defenderUserId, lethalBench,
                resolutionContext.defenderUserId(), context.nextTurnNumber(), context.stateVersion(), "ALL_BENCH_DAMAGE");
        // The active Pokemon (attacker's or defender's) is never touched by an OWN_BENCH effect.
        verify(damageApplicationService, never()).applyDamage(eq(attackerActive), any(Integer.class));
        verify(damageApplicationService, never()).applyDamage(eq(defenderActive), any(Integer.class));
        assertThat(result.events()).contains(prizeEvent);
        assertThat(result.gameFinished()).isFalse();
        assertThat(result.promotionPending()).isFalse();
    }

    @Test
    void earthquakeShouldNeverRequirePromotionEvenWhenOwnBenchIsKnockedOut() {
        // A Bench knockout must never put the game into a "promotion required" state - that
        // resolution is owned entirely by CombatResolutionService (which only triggers it when the
        // knocked-out Pokemon was the active one); this test documents that the effect simply
        // surfaces whatever CombatResolutionService decided, without adding its own promotion logic.
        PokemonInPlay attackerActive = pokemon(attackerUserId, 0);
        PokemonInPlay defenderActive = pokemon(defenderUserId, 0);
        PokemonInPlay lethalBench = pokemon(attackerUserId, 1);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(resolutionContext, defenderActive, earthquakeOperation());

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(attackerActive, lethalBench));
        when(damageApplicationService.applyDamage(lethalBench, 10)).thenReturn(2);
        when(combatResolutionService.resolveKnockoutIfNeeded(
                eq(gameId), eq(attackerUserId), eq(defenderUserId), eq(lethalBench),
                any(), any(Integer.class), any(Integer.class), eq("ALL_BENCH_DAMAGE")))
                .thenReturn(new CombatResolutionService.CombatResolutionResult(true, false, false, null, List.of(mockEvent())));

        AttackEffectResult result = effect.apply(context);

        assertThat(result.promotionPending()).isFalse();
        assertThat(result.gameFinished()).isFalse();
    }

    @Test
    void earthquakeShouldStopSweepingAndBubbleUpGameFinishedWhenABenchKnockoutWinsTheGame() {
        PokemonInPlay attackerActive = pokemon(attackerUserId, 0);
        PokemonInPlay defenderActive = pokemon(defenderUserId, 0);
        PokemonInPlay lastPrizeBench = pokemon(attackerUserId, 1);
        PokemonInPlay neverReachedBench = pokemon(attackerUserId, 2);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(resolutionContext, defenderActive, earthquakeOperation());

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(attackerActive, lastPrizeBench, neverReachedBench));
        when(damageApplicationService.applyDamage(lastPrizeBench, 10)).thenReturn(2);
        when(combatResolutionService.resolveKnockoutIfNeeded(
                eq(gameId), eq(attackerUserId), eq(defenderUserId), eq(lastPrizeBench),
                any(), any(Integer.class), any(Integer.class), eq("ALL_BENCH_DAMAGE")))
                .thenReturn(new CombatResolutionService.CombatResolutionResult(true, true, false, defenderUserId, List.of(mockEvent())));

        AttackEffectResult result = effect.apply(context);

        assertThat(result.gameFinished()).isTrue();
        assertThat(result.winnerUserId()).isEqualTo(defenderUserId);
        // The sweep must stop the instant the game finishes - the next bench slot is never even
        // damaged, since there is no game left to apply it to.
        verify(damageApplicationService, never()).applyDamage(eq(neverReachedBench), any(Integer.class));
    }

    @Test
    void opponentBenchDamageShouldRewardTheAttackerWhenItKnocksOutTheDefendersBench() {
        PokemonInPlay attackerActive = pokemon(attackerUserId, 0);
        PokemonInPlay defenderActive = pokemon(defenderUserId, 0);
        PokemonInPlay defenderBench = pokemon(defenderUserId, 1);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(
                resolutionContext, defenderActive, benchDamageOperation(OPPONENT_BENCH_TARGET));

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, defenderUserId))
                .thenReturn(List.of(defenderActive, defenderBench));
        when(damageApplicationService.applyDamage(defenderBench, 10)).thenReturn(2);
        when(combatResolutionService.resolveKnockoutIfNeeded(
                eq(gameId), eq(defenderUserId), eq(attackerUserId), eq(defenderBench),
                any(), any(Integer.class), any(Integer.class), eq("ALL_BENCH_DAMAGE")))
                .thenReturn(CombatResolutionService.CombatResolutionResult.noKnockout());

        effect.apply(context);

        // Unlike OWN_BENCH (Earthquake), an OPPONENT_BENCH effect rewards the attacker normally.
        verify(combatResolutionService).resolveKnockoutIfNeeded(
                gameId, defenderUserId, attackerUserId, defenderBench,
                resolutionContext.defenderUserId(), context.nextTurnNumber(), context.stateVersion(), "ALL_BENCH_DAMAGE");
    }

    @Test
    void benchDamageBelowKnockoutThresholdShouldNotKnockOutOrAwardAPrize() {
        PokemonInPlay attackerActive = pokemon(attackerUserId, 0);
        PokemonInPlay defenderActive = pokemon(defenderUserId, 0);
        PokemonInPlay survivingBench = pokemon(attackerUserId, 1);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(resolutionContext, defenderActive, earthquakeOperation());

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(attackerActive, survivingBench));
        when(damageApplicationService.applyDamage(survivingBench, 10)).thenReturn(1);
        when(combatResolutionService.resolveKnockoutIfNeeded(
                eq(gameId), eq(attackerUserId), eq(defenderUserId), eq(survivingBench),
                any(), any(Integer.class), any(Integer.class), eq("ALL_BENCH_DAMAGE")))
                .thenReturn(CombatResolutionService.CombatResolutionResult.noKnockout());

        AttackEffectResult result = effect.apply(context);

        // The damage is still applied (the Pokemon stays Benched, damaged but alive) and the
        // effect still asks CombatResolutionService whether it was lethal - it just wasn't here.
        verify(damageApplicationService).applyDamage(survivingBench, 10);
        verify(combatResolutionService).resolveKnockoutIfNeeded(
                eq(gameId), eq(attackerUserId), eq(defenderUserId), eq(survivingBench),
                any(), any(Integer.class), any(Integer.class), eq("ALL_BENCH_DAMAGE"));
        assertThat(result.gameFinished()).isFalse();
        assertThat(result.promotionPending()).isFalse();
    }

    @Test
    void shouldSkipKnockoutResolutionForTheAttacksOwnPrimaryTargetWhenItIsBenched() {
        // Most attacks can never reach this branch (the primary target is always the active
        // Pokemon), but some attacks allow the defender's Benched Pokemon to be selected as the
        // primary target directly. If such an attack also carried an OPPONENT_BENCH effect, the
        // primary target's own knockout/prize handling is already owned by AttackServiceImpl right
        // after this effect returns - resolving it again here would double-award the prize.
        PokemonInPlay attackerActive = pokemon(attackerUserId, 0);
        PokemonInPlay defenderActive = pokemon(defenderUserId, 0);
        PokemonInPlay primaryBenchTarget = pokemon(defenderUserId, 1);
        PokemonInPlay otherBench = pokemon(defenderUserId, 2);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(
                resolutionContext, primaryBenchTarget, benchDamageOperation(OPPONENT_BENCH_TARGET));

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, defenderUserId))
                .thenReturn(List.of(defenderActive, primaryBenchTarget, otherBench));
        when(damageApplicationService.applyDamage(any(PokemonInPlay.class), eq(10))).thenReturn(1);
        when(combatResolutionService.resolveKnockoutIfNeeded(
                eq(gameId), eq(defenderUserId), eq(attackerUserId), eq(otherBench),
                any(), any(Integer.class), any(Integer.class), eq("ALL_BENCH_DAMAGE")))
                .thenReturn(CombatResolutionService.CombatResolutionResult.noKnockout());

        effect.apply(context);

        verify(combatResolutionService, never()).resolveKnockoutIfNeeded(
                any(), any(), any(), eq(primaryBenchTarget), any(), any(Integer.class), any(Integer.class), any());
        verify(combatResolutionService).resolveKnockoutIfNeeded(
                eq(gameId), eq(defenderUserId), eq(attackerUserId), eq(otherBench),
                any(), any(Integer.class), any(Integer.class), eq("ALL_BENCH_DAMAGE"));
    }

    private AttackEffectOperation earthquakeOperation() {
        return benchDamageOperation(OWN_BENCH_TARGET);
    }

    private AttackEffectOperation benchDamageOperation(String target) {
        return new AttackEffectOperation(
                "ALL_BENCH_DAMAGE",
                AttackEffectPhase.AFTER_DAMAGE,
                target,
                10,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
    }

    private AttackResolutionContext resolutionContext(PokemonInPlay attackerActive, PokemonInPlay defenderActive) {
        return new AttackResolutionContext(
                gameId,
                attackerUserId,
                defenderUserId,
                attackerActive,
                defenderActive,
                new Card(),
                new Card(),
                null);
    }

    private AttackEffectContext effectContext(
            AttackResolutionContext resolutionContext, PokemonInPlay targetPokemon, AttackEffectOperation operation) {
        return new AttackEffectContext(resolutionContext, targetPokemon, operation, Map.of(), 5, 10, 0);
    }

    private PokemonInPlay pokemon(UUID ownerUserId, int slotPosition) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(UUID.randomUUID());
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setSlotPosition(slotPosition);
        pokemonInPlay.setDamageCounters(0);
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setCardId(UUID.randomUUID());
        pokemonInPlay.setActiveCardInstance(cardInstance);
        return pokemonInPlay;
    }

    private GameEventDto mockEvent() {
        return new GameEventDto(UUID.randomUUID(), gameId, null, 0, false, java.time.Instant.now(), Map.of());
    }
}
