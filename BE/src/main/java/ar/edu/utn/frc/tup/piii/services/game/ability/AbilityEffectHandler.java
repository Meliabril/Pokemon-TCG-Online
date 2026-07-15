package ar.edu.utn.frc.tup.piii.services.game.ability;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;

import java.util.UUID;

public interface AbilityEffectHandler {

    AbilityCode supports();

    AbilityResolution resolve(
            GameActionContext context,
            Game game,
            UUID playerId,
            PokemonInPlay sourcePokemon,
            UseAbilityRequest request,
            int stateVersion);
}
