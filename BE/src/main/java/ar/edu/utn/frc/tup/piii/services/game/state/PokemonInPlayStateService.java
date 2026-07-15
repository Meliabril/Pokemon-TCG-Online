package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PokemonInPlayStateService {

    List<PokemonInPlay> findByGameIdAndOwnerUserId(UUID gameId, UUID ownerUserId);

    Optional<PokemonInPlay> findActivePokemon(UUID gameId, UUID ownerUserId);

    Optional<PokemonInPlay> findByIdAndGameIdAndOwnerUserId(UUID id, UUID gameId, UUID ownerUserId);

    List<PokemonInPlay> findByGameIdOrdered(UUID gameId);

    PokemonInPlay save(PokemonInPlay pokemonInPlay);

    void delete(PokemonInPlay pokemonInPlay);

    int swapActiveWithBench(UUID activePokemonId, UUID benchPokemonId, Integer benchSlot);

    int countBenchPokemon(UUID gameId, UUID ownerUserId);

    Map<UUID, Integer> benchCountByPlayer(UUID gameId, List<UUID> playerIds);
}
