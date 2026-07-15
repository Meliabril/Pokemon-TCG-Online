package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReduceDefendingPokemonDamageNextTurnAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "REDUCE_DEFENDING_POKEMON_DAMAGE_NEXT_TURN";

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay defenderPokemon = context.resolutionContext().defenderPokemon();
        int reducedUntilTurn = context.turnNumber() + 1;
        defenderPokemon.setDamageReductionNextTurn(reducedUntilTurn);
        defenderPokemon.setDamageReductionAmount(context.operation().amount());
        pokemonInPlayStateService.save(defenderPokemon);

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "pokemonInPlayId", defenderPokemon.getId().toString(),
                        "reducedUntilTurn", reducedUntilTurn,
                        "damageReductionAmount", context.operation().amount(),
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }
}
