package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PokemonEvolutionStackStateService {

    List<PokemonEvolutionStack> findByPokemonInPlayId(UUID pokemonInPlayId);

    Optional<PokemonEvolutionStack> findTopByPokemonInPlayId(UUID pokemonInPlayId);

    boolean existsByGameCardInstanceId(UUID gameCardInstanceId);

    PokemonEvolutionStack save(PokemonEvolutionStack pokemonEvolutionStack);

    void delete(PokemonEvolutionStack pokemonEvolutionStack);

    int nextStackOrder(UUID pokemonInPlayId);
}
