package ar.edu.utn.frc.tup.piii.services.game.effect;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public interface SwitchActivePokemonEffectService {

    void switchWithBench(PokemonInPlay activePokemon, PokemonInPlay benchPokemon);
}
