package ar.edu.utn.frc.tup.piii.services.game.retreat.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import ar.edu.utn.frc.tup.piii.services.game.retreat.RetreatLockService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Rejects a RETREAT action when the actor's active Pokemon was locked out of retreating by an
 * opponent's attack effect (e.g. Zoroark's "Corner" / "Acorralar") during the current turn.
 */
@Component
@Order(9)
@RequiredArgsConstructor
public class RetreatLockValidator implements ActionValidator {

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final RetreatLockService retreatLockService;

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        GameActionType actionType = context.request().actionType();
        if (!GameActionType.RETREAT.equals(actionType)) {
            return;
        }

        Optional<PokemonInPlay> activePokemon = pokemonInPlayStateService.findActivePokemon(
                context.gameId(), context.actorUserId());
        if (activePokemon.isEmpty()) {
            return;
        }

        int currentTurnNumber = context.currentState().turn().turnNumber();
        if (retreatLockService.isLocked(activePokemon.get(), currentTurnNumber)) {
            throw new InvalidGameActionException("Active Pokemon cannot retreat this turn");
        }
    }
}
