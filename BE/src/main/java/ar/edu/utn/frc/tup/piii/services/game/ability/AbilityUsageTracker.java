package ar.edu.utn.frc.tup.piii.services.game.ability;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.entities.Game;

import java.util.UUID;

public interface AbilityUsageTracker {

    String ABILITY_USAGE_KEY = "abilityUsage";

    boolean wasUsedThisTurn(Game game, UUID pokemonInPlayId, AbilityCode abilityCode);

    void markUsed(Game game, UUID pokemonInPlayId, AbilityCode abilityCode);

    void clearTurnUsage(Game game);
}
