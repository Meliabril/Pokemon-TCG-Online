package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public interface DamageApplicationService {

    int applyDamage(PokemonInPlay pokemonInPlay, int damage);

    int healDamage(PokemonInPlay pokemonInPlay, int healingAmount);
}
