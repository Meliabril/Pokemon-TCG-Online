package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public interface AttackLockService {

    boolean consumeLock(PokemonInPlay pokemonInPlay, int currentTurnNumber);

    void expireLock(PokemonInPlay pokemonInPlay, int currentTurnNumber);
}
