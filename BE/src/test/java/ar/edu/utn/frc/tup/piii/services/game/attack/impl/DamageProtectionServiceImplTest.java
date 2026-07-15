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
class DamageProtectionServiceImplTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Test
    void consumeProtectionShouldClearAndReturnTrueWhenTurnMatches() {
        DamageProtectionServiceImpl service = new DamageProtectionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setDamageProtectionTurn(5);

        boolean prevented = service.consumeProtection(pokemon, 5);

        assertThat(prevented).isTrue();
        assertThat(pokemon.getDamageProtectionTurn()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void consumeProtectionShouldReturnFalseWhenTurnDoesNotMatch() {
        DamageProtectionServiceImpl service = new DamageProtectionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setDamageProtectionTurn(5);

        boolean prevented = service.consumeProtection(pokemon, 6);

        assertThat(prevented).isFalse();
        assertThat(pokemon.getDamageProtectionTurn()).isEqualTo(5);
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }

    @Test
    void consumeProtectionShouldReturnFalseWhenNoProtectionSet() {
        DamageProtectionServiceImpl service = new DamageProtectionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();

        boolean prevented = service.consumeProtection(pokemon, 5);

        assertThat(prevented).isFalse();
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }

    @Test
    void consumeProtectionShouldHonorDamageThreshold() {
        DamageProtectionServiceImpl service = new DamageProtectionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setDamageProtectionTurn(5);
        pokemon.setDamageProtectionThreshold(60);

        boolean prevented = service.consumeProtection(pokemon, 5, 60);

        assertThat(prevented).isTrue();
        assertThat(pokemon.getDamageProtectionTurn()).isNull();
        assertThat(pokemon.getDamageProtectionThreshold()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void consumeProtectionShouldNotPreventButShouldStillClearWhenDamageExceedsThreshold() {
        DamageProtectionServiceImpl service = new DamageProtectionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setDamageProtectionTurn(5);
        pokemon.setDamageProtectionThreshold(60);

        boolean prevented = service.consumeProtection(pokemon, 5, 70);

        // The Pokemon was still legitimately attacked during its protection window, so the
        // effect must disappear even though the damage was too high to be prevented - it must
        // not linger waiting for a separate, later cleanup pass.
        assertThat(prevented).isFalse();
        assertThat(pokemon.getDamageProtectionTurn()).isNull();
        assertThat(pokemon.getDamageProtectionThreshold()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void consumeProtectionShouldClearEvenWhenIncomingDamageWouldKnockOutThePokemon() {
        DamageProtectionServiceImpl service = new DamageProtectionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setDamageProtectionTurn(5);
        pokemon.setDamageProtectionThreshold(60);

        boolean prevented = service.consumeProtection(pokemon, 5, 200);

        assertThat(prevented).isFalse();
        assertThat(pokemon.getDamageProtectionTurn())
                .as("the shield flag must not survive on the entity after this attack resolves, even when it is lethal")
                .isNull();
        assertThat(pokemon.getDamageProtectionThreshold()).isNull();
    }

    @Test
    void expireProtectionShouldClearWhenProtectedTurnHasPassed() {
        DamageProtectionServiceImpl service = new DamageProtectionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setDamageProtectionTurn(5);

        service.expireProtection(pokemon, 5);

        assertThat(pokemon.getDamageProtectionTurn()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void expireProtectionShouldNotClearWhenProtectionStillPending() {
        DamageProtectionServiceImpl service = new DamageProtectionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setDamageProtectionTurn(6);

        service.expireProtection(pokemon, 5);

        assertThat(pokemon.getDamageProtectionTurn()).isEqualTo(6);
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }

    @Test
    void expireProtectionShouldDoNothingWhenNoProtectionSet() {
        DamageProtectionServiceImpl service = new DamageProtectionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();

        service.expireProtection(pokemon, 5);

        assertThat(pokemon.getDamageProtectionTurn()).isNull();
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }
}
