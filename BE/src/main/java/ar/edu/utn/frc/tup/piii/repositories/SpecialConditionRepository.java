package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecialConditionRepository extends JpaRepository<SpecialCondition, UUID> {

    List<SpecialCondition> findByPokemonInPlay_IdOrderByCreatedAtAsc(UUID pokemonInPlayId);

    Optional<SpecialCondition> findByPokemonInPlay_IdAndConditionType(UUID pokemonInPlayId, ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType conditionType);

    @Transactional
    void deleteByPokemonInPlay_Id(UUID pokemonInPlayId);
}
