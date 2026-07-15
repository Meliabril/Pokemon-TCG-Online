package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectDefinition;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackTargetResolverService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttackTargetResolverServiceImpl implements AttackTargetResolverService {

    private static final String TARGET_POKEMON_IN_PLAY_ID_KEY = "targetPokemonInPlayId";

    private final GameActionPayloadReader payloadReader;
    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public PokemonInPlay resolveTarget(
            AttackResolutionContext context,
            AttackEffectDefinition definition,
            Map<String, Object> payload) {
        if (payload == null || !payload.containsKey(TARGET_POKEMON_IN_PLAY_ID_KEY)) {
            return context.defenderPokemon();
        }

        if (definition == null || !definition.allowsBenchTarget()) {
            throw new InvalidGameActionException("The selected attack cannot target a Benched Pokemon");
        }

        UUID targetPokemonInPlayId = payloadReader.requiredUuid(payload, TARGET_POKEMON_IN_PLAY_ID_KEY);
        Optional<PokemonInPlay> targetPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                targetPokemonInPlayId,
                context.gameId(),
                context.defenderUserId());
        if (targetPokemon.isEmpty()) {
            throw new InvalidGameActionException("Attack target was not found for the defending player");
        }

        return targetPokemon.get();
    }
}
