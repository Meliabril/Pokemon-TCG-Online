package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.retreat.RetreatLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Implements the "Defending Pokemon can't retreat during your opponent's next turn" effect
 * (e.g. Zoroark's "Corner" / "Acorralar"). The lock is bound to the exact Defending Pokemon
 * instance targeted by this attack, so it has no effect if that Pokemon is knocked out and
 * replaced before the lock would otherwise apply.
 */
@Service
@RequiredArgsConstructor
public class PreventOpponentRetreatNextTurnAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "PREVENT_OPPONENT_RETREAT_NEXT_TURN";

    private final RetreatLockService retreatLockService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay defendingPokemon = context.targetPokemon();
        int lockedTurn = context.turnNumber() + 1;
        retreatLockService.lock(defendingPokemon, lockedTurn);

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "pokemonInPlayId", defendingPokemon.getId().toString(),
                        "lockedUntilTurn", lockedTurn));
        return new AttackEffectResult(0, false, List.of(event));
    }
}
