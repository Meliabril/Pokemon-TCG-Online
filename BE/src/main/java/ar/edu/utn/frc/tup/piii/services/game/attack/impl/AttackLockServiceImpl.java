package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackLockService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttackLockServiceImpl implements AttackLockService {

    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public boolean consumeLock(PokemonInPlay pokemonInPlay, int currentTurnNumber) {
        Integer lockedTurn = pokemonInPlay.getAttackLockedTurn();
        if (lockedTurn == null || lockedTurn != currentTurnNumber) {
            return false;
        }

        pokemonInPlay.setAttackLockedTurn(null);
        pokemonInPlayStateService.save(pokemonInPlay);
        return true;
    }

    @Override
    public void expireLock(PokemonInPlay pokemonInPlay, int currentTurnNumber) {
        Integer lockedTurn = pokemonInPlay.getAttackLockedTurn();
        if (lockedTurn == null || lockedTurn > currentTurnNumber) {
            return;
        }

        pokemonInPlay.setAttackLockedTurn(null);
        pokemonInPlayStateService.save(pokemonInPlay);
    }
}
