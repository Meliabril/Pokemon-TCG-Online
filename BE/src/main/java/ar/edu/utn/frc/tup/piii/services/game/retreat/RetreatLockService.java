package ar.edu.utn.frc.tup.piii.services.game.retreat;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

/**
 * Tracks attack-induced effects that prevent a specific Pokemon from retreating during
 * its own next turn (e.g. Zoroark's "Corner" / "Acorralar": "The Defending Pokemon can't
 * retreat during your opponent's next turn"). The lock is bound to the exact
 * {@link PokemonInPlay} instance that was the Defending Pokemon when the attack resolved,
 * so it has no effect if that Pokemon is later knocked out and replaced by a different one.
 */
public interface RetreatLockService {

    void lock(PokemonInPlay pokemonInPlay, int lockedTurn);

    boolean isLocked(PokemonInPlay pokemonInPlay, int currentTurnNumber);

    void expireLock(PokemonInPlay pokemonInPlay, int currentTurnNumber);
}
