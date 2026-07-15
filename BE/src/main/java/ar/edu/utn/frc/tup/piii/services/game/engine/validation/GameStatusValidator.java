package ar.edu.utn.frc.tup.piii.services.game.engine.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class GameStatusValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.currentState() == null) {
            return;
        }

        GameStatus status = context.currentState().status();
        if (GameStatus.FINISHED.equals(status) || GameStatus.CANCELLED.equals(status)) {
            throw new InvalidGameActionException("Actions are not allowed when the game is no longer playable.");
        }
    }
}
