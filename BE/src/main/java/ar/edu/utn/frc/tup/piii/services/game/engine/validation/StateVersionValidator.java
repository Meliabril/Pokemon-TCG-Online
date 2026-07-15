package ar.edu.utn.frc.tup.piii.services.game.engine.validation;

import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class StateVersionValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        Integer expectedStateVersion = context.request().expectedStateVersion();
        if (expectedStateVersion == null) {
            return;
        }

        if (expectedStateVersion.intValue() != context.currentState().stateVersion()) {
            throw new InvalidGameActionException("State version mismatch. Refresh the game state");
        }
    }
}
