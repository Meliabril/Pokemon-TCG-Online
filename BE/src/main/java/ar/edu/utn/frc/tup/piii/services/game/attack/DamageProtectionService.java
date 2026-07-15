package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public interface DamageProtectionService {

    boolean consumeProtection(PokemonInPlay pokemonInPlay, int currentTurnNumber, int incomingDamage);

    void expireProtection(PokemonInPlay pokemonInPlay, int currentTurnNumber);
}
