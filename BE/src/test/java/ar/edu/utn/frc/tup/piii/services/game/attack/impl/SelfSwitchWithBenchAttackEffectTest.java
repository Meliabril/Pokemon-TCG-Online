package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelfSwitchWithBenchAttackEffectTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private GameActionPayloadReader payloadReader;

    @Test
    void shouldSwitchActivePokemonWithSelectedBenchPokemon() {
        SelfSwitchWithBenchAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        PokemonInPlay activePokemon = pokemon(UUID.randomUUID(), 0);
        PokemonInPlay secondBench = pokemon(UUID.randomUUID(), 2);
        PokemonInPlay firstBench = pokemon(UUID.randomUUID(), 1);
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(activePokemon, secondBench, firstBench));
        when(payloadReader.requiredUuid(any(), eq("selfTargetPokemonInPlayId")))
                .thenReturn(firstBench.getId());
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(firstBench.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(firstBench));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId, activePokemon));

        assertThat(result.events()).hasSize(1);
        verify(gameCardInstanceStateService).swapActiveWithBench(
                activePokemon.getActiveCardInstance().getId(),
                firstBench.getActiveCardInstance().getId(),
                1);
        verify(pokemonInPlayStateService).swapActiveWithBench(activePokemon.getId(), firstBench.getId(), 1);
    }

    @Test
    void shouldOnlyEmitEventWhenBenchIsEmpty() {
        SelfSwitchWithBenchAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        PokemonInPlay activePokemon = pokemon(UUID.randomUUID(), 0);
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(activePokemon));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId, activePokemon));

        assertThat(result.events()).hasSize(1);
        verify(gameCardInstanceStateService, never()).swapActiveWithBench(any(), any(), any());
        verify(pokemonInPlayStateService, never()).swapActiveWithBench(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeActorAndPokemonIdentifiersInEventPayload() {
        SelfSwitchWithBenchAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        PokemonInPlay activePokemon = pokemon(UUID.randomUUID(), 0);
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(activePokemon));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectContext context = context(gameId, attackerUserId, activePokemon);
        effect.apply(context);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("actorPlayerId")).isEqualTo(context.resolutionContext().attackerUserId().toString());
        assertThat(payload.get("pokemonInPlayId")).isEqualTo(context.resolutionContext().attackerPokemon().getId().toString());
    }

    private SelfSwitchWithBenchAttackEffect effect() {
        return new SelfSwitchWithBenchAttackEffect(
                pokemonInPlayStateService,
                gameCardInstanceStateService,
                gameEventFactory,
                payloadReader);
    }

    private AttackEffectContext context(UUID gameId, UUID attackerUserId, PokemonInPlay activePokemon) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                attackerUserId,
                UUID.randomUUID(),
                activePokemon,
                null,
                null,
                null,
                null);
        return new AttackEffectContext(resolutionContext, null, operation(), Map.of(), 1, 1, 0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "SELF_SWITCH_WITH_BENCH",
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                0,
                null,
                CoinRequirement.HEADS,
                false,
                0,
                null);
    }

    private PokemonInPlay pokemon(UUID cardInstanceId, int slotPosition) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(cardInstanceId);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setActiveCardInstance(cardInstance);
        pokemon.setSlotPosition(slotPosition);
        return pokemon;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
