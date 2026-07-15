package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.Map;

public interface AttackEffectService {

    AttackEffectDefinition definitionFor(AttackResolutionContext context);

    AttackEffectResult applyBeforeDamage(
            AttackResolutionContext context,
            AttackEffectDefinition definition,
            PokemonInPlay targetPokemon,
            Map<String, Object> payload,
            int turnNumber,
            int stateVersion);

    AttackEffectResult applyAfterDamage(
            AttackResolutionContext context,
            AttackEffectDefinition definition,
            PokemonInPlay targetPokemon,
            Map<String, Object> payload,
            int turnNumber,
            int stateVersion,
            int currentDamage);
}
