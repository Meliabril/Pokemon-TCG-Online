package ar.edu.utn.frc.tup.piii.services.game.outcome;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public interface KnockoutDetectionService {

    boolean isKnockedOut(PokemonInPlay pokemonInPlay);
}
