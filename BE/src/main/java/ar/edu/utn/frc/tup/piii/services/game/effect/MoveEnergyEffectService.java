package ar.edu.utn.frc.tup.piii.services.game.effect;

import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public interface MoveEnergyEffectService {

    PokemonAttachedCard moveEnergy(PokemonAttachedCard attachedCard, PokemonInPlay fromPokemon, PokemonInPlay toPokemon);
}
