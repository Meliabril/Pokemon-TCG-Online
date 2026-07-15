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
class OutgoingDamageReductionServiceImplTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Test
    void shouldConsumeDamageReductionWhenTurnMatches() {
        OutgoingDamageReductionServiceImpl service = new OutgoingDamageReductionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setDamageReductionNextTurn(4);
        pokemon.setDamageReductionAmount(20);

        int reduction = service.consumeReduction(pokemon, 4);

        assertThat(reduction).isEqualTo(20);
        assertThat(pokemon.getDamageReductionNextTurn()).isNull();
        assertThat(pokemon.getDamageReductionAmount()).isNull();
        verify(pokemonInPlayStateService).save(pokemon);
    }

    @Test
    void shouldNotConsumeDamageReductionWhenTurnDoesNotMatch() {
        OutgoingDamageReductionServiceImpl service = new OutgoingDamageReductionServiceImpl(pokemonInPlayStateService);
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setDamageReductionNextTurn(5);
        pokemon.setDamageReductionAmount(20);

        int reduction = service.consumeReduction(pokemon, 4);

        assertThat(reduction).isZero();
        verify(pokemonInPlayStateService, never()).save(pokemon);
    }
}
