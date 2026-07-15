package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.Map;
import java.util.UUID;

public interface AttackTargetResolverService {

    PokemonInPlay resolveTarget(
            AttackResolutionContext context,
            AttackEffectDefinition definition,
            Map<String, Object> payload);
}
