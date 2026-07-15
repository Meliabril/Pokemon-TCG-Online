package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

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
class AttackLockServiceImplTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Test
    void consumeLockShouldClearAndReturnTrueWhenTurnMatches() {
        AttackLockServiceImpl service = new AttackLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setAttackLockedTurn(5);

        boolean locked = service.consumeLock(pokemon, 5);

        assertThat(locked).isTrue();
        assertThat(pokemon.getAttackLockedTurn()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void consumeLockShouldReturnFalseWhenTurnDoesNotMatch() {
        AttackLockServiceImpl service = new AttackLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setAttackLockedTurn(5);

        boolean locked = service.consumeLock(pokemon, 6);

        assertThat(locked).isFalse();
        assertThat(pokemon.getAttackLockedTurn()).isEqualTo(5);
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }

    @Test
    void consumeLockShouldReturnFalseWhenNoLockSet() {
        AttackLockServiceImpl service = new AttackLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();

        boolean locked = service.consumeLock(pokemon, 5);

        assertThat(locked).isFalse();
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }

    @Test
    void expireLockShouldClearWhenLockedTurnHasPassed() {
        AttackLockServiceImpl service = new AttackLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setAttackLockedTurn(5);

        service.expireLock(pokemon, 5);

        assertThat(pokemon.getAttackLockedTurn()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void expireLockShouldNotClearWhenLockStillPending() {
        AttackLockServiceImpl service = new AttackLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setAttackLockedTurn(6);

        service.expireLock(pokemon, 5);

        assertThat(pokemon.getAttackLockedTurn()).isEqualTo(6);
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }

    @Test
    void expireLockShouldDoNothingWhenNoLockSet() {
        AttackLockServiceImpl service = new AttackLockServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();

        service.expireLock(pokemon, 5);

        assertThat(pokemon.getAttackLockedTurn()).isNull();
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }
}
