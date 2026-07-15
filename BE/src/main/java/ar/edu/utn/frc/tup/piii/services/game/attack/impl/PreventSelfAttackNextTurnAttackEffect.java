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
public class PreventSelfAttackNextTurnAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "PREVENT_SELF_ATTACK_NEXT_TURN";
    private static final int TURNS_UNTIL_OWN_NEXT_TURN = 2;

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();
        int lockedTurn = context.turnNumber() + TURNS_UNTIL_OWN_NEXT_TURN;
        attackerPokemon.setAttackLockedTurn(lockedTurn);
        pokemonInPlayStateService.save(attackerPokemon);

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "pokemonInPlayId", attackerPokemon.getId().toString(),
                        "lockedUntilTurn", lockedTurn));
        return new AttackEffectResult(0, false, List.of(event));
    }
}
