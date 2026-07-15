package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PokemonEvolutionStackRepository extends JpaRepository<PokemonEvolutionStack, UUID> {

    @EntityGraph(attributePaths = {"gameCardInstance"})
    List<PokemonEvolutionStack> findByPokemonInPlay_IdOrderByStackOrderAsc(UUID pokemonInPlayId);

    @EntityGraph(attributePaths = {"gameCardInstance"})
    Optional<PokemonEvolutionStack> findFirstByPokemonInPlay_IdOrderByStackOrderDesc(UUID pokemonInPlayId);

    boolean existsByGameCardInstance_Id(UUID gameCardInstanceId);
}
