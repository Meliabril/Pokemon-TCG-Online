package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public interface PokemonToolModifierService {

    /** Extra damage added when this Pokémon attacks (e.g. Muscle Band +20). */
    int getOutgoingDamageBonus(PokemonInPlay attacker);

    /** Damage reduction applied when this Pokémon is attacked (e.g. Hard Charm -20, returned as negative int). */
    int getIncomingDamageModifier(PokemonInPlay defender);
}
