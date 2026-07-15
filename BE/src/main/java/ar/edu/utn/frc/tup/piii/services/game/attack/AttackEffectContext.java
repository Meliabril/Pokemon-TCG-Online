package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.Map;

public record AttackEffectContext(
        AttackResolutionContext resolutionContext,
        PokemonInPlay targetPokemon,
        AttackEffectOperation operation,
        Map<String, Object> payload,
        int stateVersion,
        int turnNumber,
        int currentDamage,
        int nextTurnNumber) {

    // A turn only ever advances by one once an attack resolves, so callers that do not care about
    // (or predate) knockouts resolved mid-effect can omit nextTurnNumber and get the same value
    // AttackServiceImpl itself derives (currentTurnNumber + 1).
    public AttackEffectContext(
            AttackResolutionContext resolutionContext,
            PokemonInPlay targetPokemon,
            AttackEffectOperation operation,
            Map<String, Object> payload,
            int stateVersion,
            int turnNumber,
            int currentDamage) {
        this(resolutionContext, targetPokemon, operation, payload, stateVersion, turnNumber, currentDamage, turnNumber + 1);
    }
}
