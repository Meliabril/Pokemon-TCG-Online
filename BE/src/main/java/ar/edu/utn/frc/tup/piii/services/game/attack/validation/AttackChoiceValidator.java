package ar.edu.utn.frc.tup.piii.services.game.attack.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(18)
public class AttackChoiceValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.currentState() == null || context.request() == null) {
            return;
        }

        ResolutionStateDto resolutionState = context.currentState().resolution();
        boolean resolveAttackChoiceAction = GameActionType.RESOLVE_ATTACK_CHOICE.equals(context.request().actionType());
        if (resolutionState == null || !resolutionState.hasPendingAttackChoice()) {
            if (resolveAttackChoiceAction) {
                throw new InvalidGameActionException("No pending attack choice is available");
            }
            return;
        }

        if (!resolveAttackChoiceAction) {
            throw new InvalidGameActionException("A pending attack choice must be resolved before any other action");
        }
        if (!context.actorUserId().equals(resolutionState.pendingChoicePlayerId())) {
            throw new InvalidGameActionException("Only the affected player can resolve the pending attack choice");
        }
    }
}
