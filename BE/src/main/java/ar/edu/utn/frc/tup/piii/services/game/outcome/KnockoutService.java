package ar.edu.utn.frc.tup.piii.services.game.outcome;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.UUID;

public interface KnockoutService {

    KnockoutResult resolveKnockout(UUID gameId, UUID ownerUserId, PokemonInPlay knockedOutPokemon);

    record KnockoutResult(PokemonInPlay promotedPokemon, boolean hasReplacementActivePokemon) {
    }
}
