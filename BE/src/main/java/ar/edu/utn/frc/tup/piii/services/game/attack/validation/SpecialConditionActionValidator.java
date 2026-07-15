package ar.edu.utn.frc.tup.piii.services.game.attack.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Order(10)
public class SpecialConditionActionValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        GameActionType actionType = context.request().actionType();
        if (!GameActionType.DECLARE_ATTACK.equals(actionType) && !GameActionType.RETREAT.equals(actionType)) {
            return;
        }

        UUID actorUserId = context.actorUserId();
        if (actorUserId == null) {
            return;
        }

        PlayerStateDto playerState = context.currentState().players().get(actorUserId);
        if (playerState == null) {
            return;
        }

        List<SpecialConditionType> activePokemonConditions = playerState.activePokemonConditions();

        if (hasBlockingCondition(activePokemonConditions)) {
            throw new InvalidGameActionException(
                    "Active Pokemon cannot attack or retreat due to a special condition");
        }
    }

    private boolean hasBlockingCondition(List<SpecialConditionType> conditions) {
        if (conditions == null) {
            return false;
        }

        for (SpecialConditionType condition : conditions) {
            if (SpecialConditionType.ASLEEP.equals(condition) || SpecialConditionType.PARALYZED.equals(condition)) {
                return true;
            }
        }

        return false;
    }
}
