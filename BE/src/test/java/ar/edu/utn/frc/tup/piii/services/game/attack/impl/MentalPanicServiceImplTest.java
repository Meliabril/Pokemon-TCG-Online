package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.MentalPanicService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MentalPanicServiceImplTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void resolveBeforeAttackShouldAllowAttackOnHeadsAndClearLock() {
        MentalPanicServiceImpl service = service();
        PokemonInPlay pokemon = pokemonWithMentalPanicTurn(4);
        when(gameRandomService.flipCoin()).thenReturn(true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        MentalPanicService.MentalPanicResult result = service.resolveBeforeAttack(pokemon, 4, UUID.randomUUID(), 2);

        assertThat(result.attackCanProceed()).isTrue();
        assertThat(result.events()).hasSize(1);
        assertThat(pokemon.getMentalPanicTurn()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void resolveBeforeAttackShouldCancelAttackOnTailsAndClearLock() {
        MentalPanicServiceImpl service = service();
        PokemonInPlay pokemon = pokemonWithMentalPanicTurn(4);
        when(gameRandomService.flipCoin()).thenReturn(false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        MentalPanicService.MentalPanicResult result = service.resolveBeforeAttack(pokemon, 4, UUID.randomUUID(), 2);

        assertThat(result.attackCanProceed()).isFalse();
        assertThat(result.events()).hasSize(1);
        assertThat(pokemon.getMentalPanicTurn()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void resolveBeforeAttackShouldDoNothingWhenTurnDoesNotMatch() {
        MentalPanicServiceImpl service = service();
        PokemonInPlay pokemon = pokemonWithMentalPanicTurn(5);

        MentalPanicService.MentalPanicResult result = service.resolveBeforeAttack(pokemon, 4, UUID.randomUUID(), 2);

        assertThat(result.attackCanProceed()).isTrue();
        assertThat(result.events()).isEmpty();
        assertThat(pokemon.getMentalPanicTurn()).isEqualTo(5);
        verify(gameRandomService, never()).flipCoin();
        verify(pokemonInPlayStateService, never()).save(any());
    }

    @Test
    void expireLockShouldClearStaleLock() {
        MentalPanicServiceImpl service = service();
        PokemonInPlay pokemon = pokemonWithMentalPanicTurn(4);

        service.expireLock(pokemon, 4);

        assertThat(pokemon.getMentalPanicTurn()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolveBeforeAttackShouldIncludeCoinResultsArrayAndActorPlayerId() {
        MentalPanicServiceImpl service = service();
        PokemonInPlay pokemon = pokemonWithMentalPanicTurn(4);
        when(gameRandomService.flipCoin()).thenReturn(true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        service.resolveBeforeAttack(pokemon, 4, UUID.randomUUID(), 2);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("coinResults")).isEqualTo(List.of("HEADS"));
        assertThat(payload.get("actorPlayerId")).isEqualTo(pokemon.getOwnerUserId().toString());
        assertThat(payload.get("pokemonInPlayId")).isEqualTo(pokemon.getId().toString());
        assertThat(payload.containsKey("coinResult")).isFalse();
    }

    private MentalPanicServiceImpl service() {
        return new MentalPanicServiceImpl(pokemonInPlayStateService, gameRandomService, gameEventFactory);
    }

    private PokemonInPlay pokemonWithMentalPanicTurn(int turnNumber) {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setOwnerUserId(UUID.randomUUID());
        pokemon.setMentalPanicTurn(turnNumber);
        return pokemon;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
