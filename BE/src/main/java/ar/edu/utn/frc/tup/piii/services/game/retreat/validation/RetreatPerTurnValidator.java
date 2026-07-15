package ar.edu.utn.frc.tup.piii.services.game.retreat.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(8)
public class RetreatPerTurnValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        GameActionType actionType = context.request().actionType();
        if (GameActionType.RETREAT.equals(actionType)
                && context.currentState().turn().retreatedThisTurn()) {
            throw new InvalidGameActionException("Pokemon already retreated this turn");
        }
    }
}
