package ar.edu.utn.frc.tup.piii.services.game.stadium;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.UUID;

public interface StadiumModifierService {

    boolean isRetreatFree(UUID gameId, PokemonInPlay activePokemon);

    boolean isWeaknessNegated(UUID gameId, PokemonInPlay defenderPokemon);
}
