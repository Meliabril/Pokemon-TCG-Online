package ar.edu.utn.frc.tup.piii.services.game.evolution.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(12)
public class EvolutionValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        if (!GameActionType.EVOLVE_POKEMON.equals(context.request().actionType())) {
            return;
        }

        int currentTurnNumber = context.currentState().turn().turnNumber();
        if (isFirstTurn(currentTurnNumber)) {
            throw new InvalidGameActionException("Cannot evolve a Pokemon during your first turn");
        }
    }

    private boolean isFirstTurn(int currentTurnNumber) {
        return currentTurnNumber == 1 || currentTurnNumber == 2;
    }
}
