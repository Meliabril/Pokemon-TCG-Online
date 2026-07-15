package ar.edu.utn.frc.tup.piii.services.game.setup.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class SetupActionValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.currentState() == null || context.request() == null) {
            return;
        }

        GameActionType actionType = context.request().actionType();
        GameStatus status = context.currentState().status();

        if (GameActionType.START_GAME.equals(actionType) && !GameStatus.WAITING.equals(status)) {
            throw new InvalidGameActionException("Only waiting games can be started");
        }

        if (GameActionType.CHOOSE_INITIAL_POKEMON.equals(actionType) && !GameStatus.SETUP.equals(status)) {
            throw new InvalidGameActionException("Initial Pokemon can only be selected during setup");
        }

        if (GameActionType.ACK_MULLIGAN_NOTICE.equals(actionType) && !GameStatus.SETUP.equals(status)) {
            throw new InvalidGameActionException("Mulligan notices can only be acknowledged during setup");
        }
    }
}
