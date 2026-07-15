package ar.edu.utn.frc.tup.piii.services.game.outcome.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(17)
public class PromotionValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.currentState() == null || context.request() == null) {
            return;
        }

        ResolutionStateDto resolutionState = context.currentState().resolution();
        boolean promotionAction = GameActionType.PROMOTE_BENCH_POKEMON.equals(context.request().actionType());
        if (resolutionState == null || !resolutionState.hasPendingPromotion()) {
            if (promotionAction) {
                throw new InvalidGameActionException("No pending promotion is available");
            }
            return;
        }

        if (!promotionAction) {
            throw new InvalidGameActionException("A pending promotion must be resolved before any other action");
        }
        if (!context.actorUserId().equals(resolutionState.playerToPromoteId())) {
            throw new InvalidGameActionException("Only the affected player can promote a Benched Pokemon");
        }
    }
}
