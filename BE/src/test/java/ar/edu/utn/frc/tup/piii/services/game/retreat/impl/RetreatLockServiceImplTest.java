package ar.edu.utn.frc.tup.piii.services.game.retreat.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RetreatLockServiceImplTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Test
    void lockShouldSetRetreatLockedTurnAndPersist() {
        RetreatLockServiceImpl service = new RetreatLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();

        service.lock(pokemon, 6);

        assertThat(pokemon.getRetreatLockedTurn()).isEqualTo(6);
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void isLockedShouldReturnTrueWhenTurnMatches() {
        RetreatLockServiceImpl service = new RetreatLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setRetreatLockedTurn(6);

        assertThat(service.isLocked(pokemon, 6)).isTrue();
    }

    @Test
    void isLockedShouldReturnFalseWhenTurnDoesNotMatch() {
        RetreatLockServiceImpl service = new RetreatLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setRetreatLockedTurn(6);

        assertThat(service.isLocked(pokemon, 7)).isFalse();
    }

    @Test
    void isLockedShouldReturnFalseWhenNoLockSet() {
        RetreatLockServiceImpl service = new RetreatLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();

        assertThat(service.isLocked(pokemon, 6)).isFalse();
    }

    @Test
    void expireLockShouldClearWhenLockedTurnHasPassed() {
        RetreatLockServiceImpl service = new RetreatLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setRetreatLockedTurn(6);

        service.expireLock(pokemon, 6);

        assertThat(pokemon.getRetreatLockedTurn()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void expireLockShouldNotClearWhenLockStillPending() {
        RetreatLockServiceImpl service = new RetreatLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setRetreatLockedTurn(7);

        service.expireLock(pokemon, 6);

        assertThat(pokemon.getRetreatLockedTurn()).isEqualTo(7);
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }

    @Test
    void expireLockShouldDoNothingWhenNoLockSet() {
        RetreatLockServiceImpl service = new RetreatLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();

        service.expireLock(pokemon, 6);

        assertThat(pokemon.getRetreatLockedTurn()).isNull();
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }
}
