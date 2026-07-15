package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.effect.DamageCounterEffectService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the Gourgeist (xy1-57) "Eerie Voice" / "Spirit Scream" scenarios:
 * Eerie Voice must damage every opponent Pokemon; Spirit Scream targets BOTH_ACTIVE.
 */
@ExtendWith(MockitoExtension.class)
class DamageCountersAttackEffectTest {

    private static final String ALL_OPPONENT_TARGET = "ALL_OPPONENT";
    private static final String BOTH_ACTIVE_TARGET = "BOTH_ACTIVE";

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private DamageCounterEffectService damageCounterEffectService;

    @Mock
    private CardService cardService;

    private DamageCountersAttackEffect effect;

    private final UUID gameId = UUID.randomUUID();
    private final UUID attackerUserId = UUID.randomUUID();
    private final UUID defenderUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        effect = new DamageCountersAttackEffect(pokemonInPlayStateService, damageCounterEffectService, cardService);

        lenient().when(damageCounterEffectService.placeDamageCounters(
                any(), any(), anyInt(), any(), any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(new DamageCounterEffectService.DamageCounterResult(
                        20, 2, false, false, null, List.of()));
    }

    @Test
    void eerieVoiceShouldDamageEveryOpponentPokemonAndLeaveAttackerSideUntouched() {
        PokemonInPlay attackerActive = pokemon(attackerUserId);
        PokemonInPlay attackerBench = pokemon(attackerUserId);
        PokemonInPlay defenderActive = pokemon(defenderUserId);
        PokemonInPlay defenderBench = pokemon(defenderUserId);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(resolutionContext, defenderActive, eerieVoiceOperation());

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, defenderUserId))
                .thenReturn(List.of(defenderActive, defenderBench));

        AttackEffectResult result = effect.apply(context);

        // Both of the defender's Pokemon take damage
        verify(damageCounterEffectService).placeDamageCounters(
                eq(gameId), eq(defenderActive), eq(2), any(), any(), anyInt(), anyInt(), eq("DAMAGE_COUNTERS"), eq(false));
        verify(damageCounterEffectService).placeDamageCounters(
                eq(gameId), eq(defenderBench), eq(2), any(), any(), anyInt(), anyInt(), eq("DAMAGE_COUNTERS"), eq(true));
        // Attacker's Pokemon are never targeted
        verify(damageCounterEffectService, never()).placeDamageCounters(
                any(), eq(attackerActive), anyInt(), any(), any(), anyInt(), anyInt(), anyString(), anyBoolean());
        verify(damageCounterEffectService, never()).placeDamageCounters(
                any(), eq(attackerBench), anyInt(), any(), any(), anyInt(), anyInt(), anyString(), anyBoolean());

        assertThat(result.gameFinished()).isFalse();
        assertThat(result.promotionPending()).isFalse();
    }

    @Test
    void eerieVoiceShouldEmitEventsFromDamageCounterService() {
        PokemonInPlay attackerActive = pokemon(attackerUserId);
        PokemonInPlay defenderActive = pokemon(defenderUserId);
        PokemonInPlay defenderBench = pokemon(defenderUserId);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(resolutionContext, defenderActive, eerieVoiceOperation());

        GameEventDto benchEvent = mockEvent();
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, defenderUserId))
                .thenReturn(List.of(defenderActive, defenderBench));
        when(damageCounterEffectService.placeDamageCounters(
                eq(gameId), eq(defenderBench), anyInt(), any(), any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(new DamageCounterEffectService.DamageCounterResult(
                        20, 2, false, false, null, List.of(benchEvent)));

        AttackEffectResult result = effect.apply(context);

        assertThat(result.events()).contains(benchEvent);
    }

    @Test
    void eerieVoiceShouldReturnKnockoutOutcomeWhenBenchPokemonIsKnockedOut() {
        PokemonInPlay attackerActive = pokemon(attackerUserId);
        PokemonInPlay defenderActive = pokemon(defenderUserId);
        PokemonInPlay defenderBench = pokemon(defenderUserId);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(resolutionContext, defenderActive, eerieVoiceOperation());

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, defenderUserId))
                .thenReturn(List.of(defenderActive, defenderBench));
        // defenderActive is primary target — its KO is deferred to AttackServiceImpl, so
        // even a gameFinished=true result from it should NOT short-circuit here
        // defenderBench is not primary — its KO should cause promotionPending
        when(damageCounterEffectService.placeDamageCounters(
                eq(gameId), eq(defenderBench), anyInt(), any(), any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(new DamageCounterEffectService.DamageCounterResult(
                        20, 2, false, true, null, List.of(mockEvent())));

        AttackEffectResult result = effect.apply(context);

        assertThat(result.promotionPending()).isTrue();
    }

    @Test
    void spiritScreamShouldOnlyDamageBothActivesAndNeverTouchTheBench() {
        PokemonInPlay attackerActive = pokemon(attackerUserId);
        PokemonInPlay defenderActive = pokemon(defenderUserId);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(resolutionContext, defenderActive, spiritScreamOperation());

        Card sharedCardStats = new Card();
        sharedCardStats.setHp(60);
        when(cardService.getCardEntityById(any())).thenReturn(sharedCardStats);
        attackerActive.setDamageCounters(0);
        defenderActive.setDamageCounters(0);

        effect.apply(context);

        // remainingHp(60) - target(10) = 50 damage => 5 counters for each active
        verify(damageCounterEffectService).placeDamageCounters(
                eq(gameId), eq(attackerActive), eq(5), any(), any(), anyInt(), anyInt(), anyString(), eq(true));
        verify(damageCounterEffectService).placeDamageCounters(
                eq(gameId), eq(defenderActive), eq(5), any(), any(), anyInt(), anyInt(), anyString(), eq(false));
        // BOTH_ACTIVE never queries the bench service
        verify(pokemonInPlayStateService, never()).findByGameIdAndOwnerUserId(any(), any());
    }

    @Test
    void eerieVoiceWithNoOpponentBenchShouldOnlyDamageActive() {
        PokemonInPlay attackerActive = pokemon(attackerUserId);
        PokemonInPlay defenderActive = pokemon(defenderUserId);

        AttackResolutionContext resolutionContext = resolutionContext(attackerActive, defenderActive);
        AttackEffectContext context = effectContext(resolutionContext, defenderActive, eerieVoiceOperation());

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, defenderUserId))
                .thenReturn(List.of(defenderActive));

        effect.apply(context);

        verify(damageCounterEffectService).placeDamageCounters(
                eq(gameId), eq(defenderActive), eq(2), any(), any(), anyInt(), anyInt(), eq("DAMAGE_COUNTERS"), eq(false));
        verify(damageCounterEffectService, never()).placeDamageCounters(
                any(), eq(attackerActive), anyInt(), any(), any(), anyInt(), anyInt(), anyString(), anyBoolean());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackEffectOperation eerieVoiceOperation() {
        return new AttackEffectOperation(
                "DAMAGE_COUNTERS",
                AttackEffectPhase.AFTER_DAMAGE,
                ALL_OPPONENT_TARGET,
                2,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
    }

    private AttackEffectOperation spiritScreamOperation() {
        return new AttackEffectOperation(
                "DAMAGE_COUNTERS_UNTIL_REMAINING_HP",
                AttackEffectPhase.AFTER_DAMAGE,
                BOTH_ACTIVE_TARGET,
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

    private PokemonInPlay pokemon(UUID ownerUserId) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(UUID.randomUUID());
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setDamageCounters(0);
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setCardId(UUID.randomUUID());
        pokemonInPlay.setActiveCardInstance(cardInstance);
        return pokemonInPlay;
    }

    private GameEventDto mockEvent() {
        return new GameEventDto(UUID.randomUUID(), gameId, null, 0, false, Instant.now(), Map.of());
    }
}
