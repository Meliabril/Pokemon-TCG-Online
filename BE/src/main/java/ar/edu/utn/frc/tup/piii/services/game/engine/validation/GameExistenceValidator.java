package ar.edu.utn.frc.tup.piii.services.game.engine.validation;

import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(-1)
public class GameExistenceValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.currentState() == null) {
            throw new InvalidGameActionException("Game state is required to validate an action.");
        }
    }
}
