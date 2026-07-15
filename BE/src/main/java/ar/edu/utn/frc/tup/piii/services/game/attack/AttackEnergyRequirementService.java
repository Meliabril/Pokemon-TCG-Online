package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public interface AttackEnergyRequirementService {

    boolean hasRequiredEnergy(PokemonInPlay attackerPokemon, Attack attack);
}
