package ar.edu.utn.frc.tup.piii.services.game.energy.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
public class EnergyPerTurnValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        GameActionType actionType = context.request().actionType();
        if (GameActionType.ATTACH_ENERGY.equals(actionType)
                && context.currentState().turn().energyAttachedThisTurn()) {
            throw new InvalidGameActionException("Energy already attached this turn");
        }
    }
}
