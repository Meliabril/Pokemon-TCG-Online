package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.repositories.PokemonAttachedCardRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PokemonAttachedCardStateServiceImpl implements PokemonAttachedCardStateService {

    private final PokemonAttachedCardRepository pokemonAttachedCardRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PokemonAttachedCard> findByPokemonInPlayId(UUID pokemonInPlayId) {
        return pokemonAttachedCardRepository.findByPokemonInPlay_IdOrderByCreatedAtAsc(pokemonInPlayId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PokemonAttachedCard> findByGameCardInstanceId(UUID gameCardInstanceId) {
        return pokemonAttachedCardRepository.findByGameCardInstance_Id(gameCardInstanceId);
    }

    @Override
    @Transactional
    public PokemonAttachedCard save(PokemonAttachedCard pokemonAttachedCard) {
        return pokemonAttachedCardRepository.save(pokemonAttachedCard);
    }

    @Override
    @Transactional
    public void delete(PokemonAttachedCard pokemonAttachedCard) {
        pokemonAttachedCardRepository.delete(pokemonAttachedCard);
    }
}
