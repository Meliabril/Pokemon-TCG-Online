package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import ar.edu.utn.frc.tup.piii.repositories.SpecialConditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpecialConditionStateServiceImpl implements SpecialConditionStateService {

    private final SpecialConditionRepository specialConditionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SpecialCondition> findByPokemonInPlayId(UUID pokemonInPlayId) {
        return specialConditionRepository.findByPokemonInPlay_IdOrderByCreatedAtAsc(pokemonInPlayId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SpecialCondition> findByPokemonInPlayIdAndConditionType(UUID pokemonInPlayId, SpecialConditionType conditionType) {
        return specialConditionRepository.findByPokemonInPlay_IdAndConditionType(pokemonInPlayId, conditionType);
    }

    @Override
    @Transactional
    public SpecialCondition save(SpecialCondition specialCondition) {
        return specialConditionRepository.save(specialCondition);
    }

    @Override
    @Transactional
    public void delete(SpecialCondition specialCondition) {
        specialConditionRepository.delete(specialCondition);
    }

    @Override
    @Transactional
    public void deleteByPokemonInPlayId(UUID pokemonInPlayId) {
        specialConditionRepository.deleteByPokemonInPlay_Id(pokemonInPlayId);
    }

    @Override
    @Transactional
    public void clearConditions(UUID pokemonInPlayId, Set<SpecialConditionType> conditionTypes) {
        for (SpecialCondition specialCondition : findByPokemonInPlayId(pokemonInPlayId)) {
            if (conditionTypes.contains(specialCondition.getConditionType())) {
                specialConditionRepository.delete(specialCondition);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecialConditionType> activeConditionTypes(UUID pokemonInPlayId) {
        List<SpecialConditionType> activeTypes = new ArrayList<>();
        for (SpecialCondition specialCondition : findByPokemonInPlayId(pokemonInPlayId)) {
            activeTypes.add(specialCondition.getConditionType());
        }
        return List.copyOf(activeTypes);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<SpecialConditionType>> activeConditionsByPlayer(
            UUID gameId,
            List<UUID> playerIds,
            PokemonInPlayStateService pokemonInPlayStateService) {
        Map<UUID, List<SpecialConditionType>> conditionsByPlayer = new LinkedHashMap<>();
        for (UUID playerId : playerIds) {
            Optional<ar.edu.utn.frc.tup.piii.entities.PokemonInPlay> activePokemon = pokemonInPlayStateService.findActivePokemon(gameId, playerId);
            List<SpecialConditionType> conditions = List.of();
            if (activePokemon.isPresent()) {
                conditions = activeConditionTypes(activePokemon.get().getId());
            }
            conditionsByPlayer.put(playerId, conditions);
        }
        return Map.copyOf(conditionsByPlayer);
    }
}
