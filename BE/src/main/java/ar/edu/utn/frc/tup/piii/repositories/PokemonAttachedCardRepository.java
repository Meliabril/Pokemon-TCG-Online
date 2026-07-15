package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PokemonAttachedCardRepository extends JpaRepository<PokemonAttachedCard, UUID> {

    @EntityGraph(attributePaths = {"gameCardInstance"})
    List<PokemonAttachedCard> findByPokemonInPlay_IdOrderByCreatedAtAsc(UUID pokemonInPlayId);

    @EntityGraph(attributePaths = {"gameCardInstance"})
    Optional<PokemonAttachedCard> findByGameCardInstance_Id(UUID gameCardInstanceId);
}
