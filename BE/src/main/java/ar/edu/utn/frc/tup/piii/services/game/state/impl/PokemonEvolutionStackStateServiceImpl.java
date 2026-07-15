package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.repositories.PokemonEvolutionStackRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PokemonEvolutionStackStateServiceImpl implements PokemonEvolutionStackStateService {

    private final PokemonEvolutionStackRepository pokemonEvolutionStackRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PokemonEvolutionStack> findByPokemonInPlayId(UUID pokemonInPlayId) {
        return pokemonEvolutionStackRepository.findByPokemonInPlay_IdOrderByStackOrderAsc(pokemonInPlayId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PokemonEvolutionStack> findTopByPokemonInPlayId(UUID pokemonInPlayId) {
        return pokemonEvolutionStackRepository.findFirstByPokemonInPlay_IdOrderByStackOrderDesc(pokemonInPlayId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByGameCardInstanceId(UUID gameCardInstanceId) {
        return pokemonEvolutionStackRepository.existsByGameCardInstance_Id(gameCardInstanceId);
    }

    @Override
    @Transactional
    public PokemonEvolutionStack save(PokemonEvolutionStack pokemonEvolutionStack) {
        return pokemonEvolutionStackRepository.save(pokemonEvolutionStack);
    }

    @Override
    @Transactional
    public void delete(PokemonEvolutionStack pokemonEvolutionStack) {
        pokemonEvolutionStackRepository.delete(pokemonEvolutionStack);
    }

    @Override
    @Transactional(readOnly = true)
    public int nextStackOrder(UUID pokemonInPlayId) {
        int highestStackOrder = -1;
        List<PokemonEvolutionStack> evolutionStack = findByPokemonInPlayId(pokemonInPlayId);
        for (PokemonEvolutionStack stackEntry : evolutionStack) {
            Integer stackOrder = stackEntry.getStackOrder();
            if (stackOrder != null && stackOrder > highestStackOrder) {
                highestStackOrder = stackOrder;
            }
        }

        return highestStackOrder + 1;
    }
}
