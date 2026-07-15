package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public interface OutgoingDamageReductionService {

    int consumeReduction(PokemonInPlay pokemonInPlay, int currentTurnNumber);

    void expireReduction(PokemonInPlay pokemonInPlay, int currentTurnNumber);
}
