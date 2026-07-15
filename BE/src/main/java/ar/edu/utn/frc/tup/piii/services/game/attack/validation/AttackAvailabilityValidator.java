package ar.edu.utn.frc.tup.piii.services.game.attack.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Order(11)
public class AttackAvailabilityValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        if (!GameActionType.DECLARE_ATTACK.equals(context.request().actionType())) {
            return;
        }

        int currentTurnNumber = context.currentState().turn().turnNumber();
        UUID playerWhoWentFirstId = context.currentState().turn().playerWhoWentFirstId();
        UUID actorUserId = context.actorUserId();

        if (isFirstPlayerFirstTurn(currentTurnNumber, playerWhoWentFirstId, actorUserId)) {
            throw new InvalidGameActionException(
                    "The player who goes first cannot attack on their first turn");
        }
    }

    private boolean isFirstPlayerFirstTurn(
            int currentTurnNumber,
            UUID playerWhoWentFirstId,
            UUID actorUserId) {
        if (playerWhoWentFirstId == null || actorUserId == null) {
            return false;
        }

        boolean isInitialTurnWindow = currentTurnNumber == 1 || currentTurnNumber == 2;
        return isInitialTurnWindow && actorUserId.equals(playerWhoWentFirstId);
    }
}
