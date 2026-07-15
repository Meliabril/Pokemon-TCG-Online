package ar.edu.utn.frc.tup.piii.services.game.turn.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Order(4)
public class PlayerTurnValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.currentState() == null) {
            return;
        }

        if (context.request() != null && isSetupFlowAction(context.request().actionType())) {
            return;
        }
        if (context.request() != null && GameActionType.PROMOTE_BENCH_POKEMON.equals(context.request().actionType())) {
            return;
        }
        if (context.request() != null && canResolvePendingAttackChoice(context)) {
            return;
        }

        UUID actorUserId = context.actorUserId();
        UUID activePlayerId = context.currentState().turn().activePlayerId();

        if (actorUserId == null || !actorUserId.equals(activePlayerId)) {
            throw new InvalidGameActionException("It is not this player's turn.");
        }
    }

    private boolean isSetupFlowAction(GameActionType actionType) {
        return GameActionType.START_GAME.equals(actionType)
                || GameActionType.ACK_MULLIGAN_NOTICE.equals(actionType)
                || GameActionType.CHOOSE_INITIAL_POKEMON.equals(actionType);
    }

    private boolean canResolvePendingAttackChoice(GameActionContext context) {
        if (!GameActionType.RESOLVE_ATTACK_CHOICE.equals(context.request().actionType())) {
            return false;
        }

        ResolutionStateDto resolutionState = context.currentState().resolution();
        if (resolutionState == null || !resolutionState.hasPendingAttackChoice()) {
            return false;
        }

        UUID actorUserId = context.actorUserId();
        return actorUserId != null && actorUserId.equals(resolutionState.pendingChoicePlayerId());
    }
}
