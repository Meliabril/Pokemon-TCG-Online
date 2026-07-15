package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PokemonAttachedCardStateService {

    List<PokemonAttachedCard> findByPokemonInPlayId(UUID pokemonInPlayId);

    Optional<PokemonAttachedCard> findByGameCardInstanceId(UUID gameCardInstanceId);

    PokemonAttachedCard save(PokemonAttachedCard pokemonAttachedCard);

    void delete(PokemonAttachedCard pokemonAttachedCard);
}
