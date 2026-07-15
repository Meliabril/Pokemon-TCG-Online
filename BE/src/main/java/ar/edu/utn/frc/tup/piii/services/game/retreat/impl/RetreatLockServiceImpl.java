package ar.edu.utn.frc.tup.piii.services.game.retreat.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.retreat.RetreatLockService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetreatLockServiceImpl implements RetreatLockService {

    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public void lock(PokemonInPlay pokemonInPlay, int lockedTurn) {
        pokemonInPlay.setRetreatLockedTurn(lockedTurn);
        pokemonInPlayStateService.save(pokemonInPlay);
    }

    @Override
    public boolean isLocked(PokemonInPlay pokemonInPlay, int currentTurnNumber) {
        Integer lockedTurn = pokemonInPlay.getRetreatLockedTurn();
        return lockedTurn != null && lockedTurn == currentTurnNumber;
    }

    @Override
    public void expireLock(PokemonInPlay pokemonInPlay, int currentTurnNumber) {
        Integer lockedTurn = pokemonInPlay.getRetreatLockedTurn();
        if (lockedTurn == null || lockedTurn > currentTurnNumber) {
            return;
        }

        pokemonInPlay.setRetreatLockedTurn(null);
        pokemonInPlayStateService.save(pokemonInPlay);
    }
}
