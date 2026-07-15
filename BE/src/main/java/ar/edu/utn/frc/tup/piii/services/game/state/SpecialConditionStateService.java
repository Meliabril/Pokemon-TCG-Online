package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SpecialConditionStateService {

    List<SpecialCondition> findByPokemonInPlayId(UUID pokemonInPlayId);

    Optional<SpecialCondition> findByPokemonInPlayIdAndConditionType(UUID pokemonInPlayId, SpecialConditionType conditionType);

    SpecialCondition save(SpecialCondition specialCondition);

    void delete(SpecialCondition specialCondition);

    void deleteByPokemonInPlayId(UUID pokemonInPlayId);

    void clearConditions(UUID pokemonInPlayId, Set<SpecialConditionType> conditionTypes);

    List<SpecialConditionType> activeConditionTypes(UUID pokemonInPlayId);

    Map<UUID, List<SpecialConditionType>> activeConditionsByPlayer(UUID gameId, List<UUID> playerIds, PokemonInPlayStateService pokemonInPlayStateService);
}
