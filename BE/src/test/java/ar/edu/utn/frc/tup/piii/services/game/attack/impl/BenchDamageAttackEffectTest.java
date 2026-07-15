package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenchDamageAttackEffectTest {

    private static final String TARGET_KEY = "switchTargetPokemonInPlayId";

    @Mock
    private GameActionPayloadReader payloadReader;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private DamageApplicationService damageApplicationService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldResolveWithoutEffectWhenNoTargetIsSelectedAndOpponentBenchIsEmpty() {
        UUID gameId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        AttackEffectContext context = context(gameId, UUID.randomUUID(), defenderUserId, Map.of());
        BenchDamageAttackEffect effect = effect();

        when(payloadReader.optionalUuid(Map.of(), TARGET_KEY)).thenReturn(Optional.empty());
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, defenderUserId))
                .thenReturn(List.of(activePokemon(defenderUserId)));

        AttackEffectResult result = effect.apply(context);

        assertThat(result.events()).isEmpty();
        assertThat(result.choiceRequired()).isFalse();
        verify(damageApplicationService, never()).applyDamage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void shouldRequireTargetWhenOpponentHasBenchPokemon() {
        UUID gameId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        AttackEffectContext context = context(gameId, UUID.randomUUID(), defenderUserId, Map.of());
        BenchDamageAttackEffect effect = effect();

        when(payloadReader.optionalUuid(Map.of(), TARGET_KEY)).thenReturn(Optional.empty());
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, defenderUserId))
                .thenReturn(List.of(activePokemon(defenderUserId), benchPokemon(defenderUserId)));

        assertThatThrownBy(() -> effect.apply(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("target is required");
    }

    private BenchDamageAttackEffect effect() {
        return new BenchDamageAttackEffect(
                payloadReader,
                pokemonInPlayStateService,
                damageApplicationService,
                gameEventFactory);
    }

    private AttackEffectContext context(
            UUID gameId,
            UUID attackerUserId,
            UUID defenderUserId,
            Map<String, Object> payload) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                attackerUserId,
                defenderUserId,
                activePokemon(attackerUserId),
                activePokemon(defenderUserId),
                null,
                null,
                null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "BENCH_DAMAGE",
                AttackEffectPhase.AFTER_DAMAGE,
                "OPPONENT_BENCH",
                10,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
        return new AttackEffectContext(resolutionContext, resolutionContext.defenderPokemon(), operation, payload, 2, 1, 0);
    }

    private PokemonInPlay activePokemon(UUID ownerUserId) {
        PokemonInPlay pokemon = pokemon(ownerUserId);
        pokemon.setSlotPosition(0);
        return pokemon;
    }

    private PokemonInPlay benchPokemon(UUID ownerUserId) {
        PokemonInPlay pokemon = pokemon(ownerUserId);
        pokemon.setSlotPosition(1);
        return pokemon;
    }

    private PokemonInPlay pokemon(UUID ownerUserId) {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setOwnerUserId(ownerUserId);
        return pokemon;
    }
}
