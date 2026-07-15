package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.repositories.PokemonInPlayRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PokemonInPlayStateServiceImpl implements PokemonInPlayStateService {

    private final PokemonInPlayRepository pokemonInPlayRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PokemonInPlay> findByGameIdAndOwnerUserId(UUID gameId, UUID ownerUserId) {
        return pokemonInPlayRepository.findByGame_IdAndOwnerUserIdOrderBySlotPositionAsc(gameId, ownerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PokemonInPlay> findActivePokemon(UUID gameId, UUID ownerUserId) {
        return pokemonInPlayRepository.findByGame_IdAndOwnerUserIdAndSlotPosition(gameId, ownerUserId, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PokemonInPlay> findByIdAndGameIdAndOwnerUserId(UUID id, UUID gameId, UUID ownerUserId) {
        return pokemonInPlayRepository.findByIdAndGame_IdAndOwnerUserId(id, gameId, ownerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PokemonInPlay> findByGameIdOrdered(UUID gameId) {
        return pokemonInPlayRepository.findByGame_IdOrderByOwnerUserIdAscSlotPositionAsc(gameId);
    }

    @Override
    @Transactional
    public PokemonInPlay save(PokemonInPlay pokemonInPlay) {
        return pokemonInPlayRepository.save(pokemonInPlay);
    }

    @Override
    @Transactional
    public void delete(PokemonInPlay pokemonInPlay) {
        pokemonInPlayRepository.delete(pokemonInPlay);
    }

    @Override
    @Transactional
    public int swapActiveWithBench(UUID activePokemonId, UUID benchPokemonId, Integer benchSlot) {
        return pokemonInPlayRepository.swapActiveWithBench(activePokemonId, benchPokemonId, benchSlot);
    }

    @Override
    @Transactional(readOnly = true)
    public int countBenchPokemon(UUID gameId, UUID ownerUserId) {
        int benchCount = 0;
        for (PokemonInPlay slot : findByGameIdAndOwnerUserId(gameId, ownerUserId)) {
            if (slot.getSlotPosition() != null && slot.getSlotPosition() > 0) {
                benchCount++;
            }
        }
        return benchCount;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Integer> benchCountByPlayer(UUID gameId, List<UUID> playerIds) {
        Map<UUID, Integer> benchCounts = new LinkedHashMap<>();
        for (UUID playerId : playerIds) {
            benchCounts.put(playerId, countBenchPokemon(gameId, playerId));
        }
        return Map.copyOf(benchCounts);
    }
}
