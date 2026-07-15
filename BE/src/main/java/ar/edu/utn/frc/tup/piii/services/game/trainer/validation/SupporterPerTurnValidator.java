package ar.edu.utn.frc.tup.piii.services.game.trainer.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(7)
public class SupporterPerTurnValidator implements ActionValidator {

    private static final String TRAINER_SUBTYPE_KEY = "trainerSubtype";
    private static final String SUPPORTER_SUBTYPE = "SUPPORTER";

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        GameActionType actionType = context.request().actionType();
        if (!GameActionType.PLAY_TRAINER.equals(actionType)) {
            return;
        }

        Map<String, Object> payload = context.request().payload();
        Object trainerSubtype = payload.get(TRAINER_SUBTYPE_KEY);

        if (SUPPORTER_SUBTYPE.equals(trainerSubtype) && context.currentState().turn().supporterPlayedThisTurn()) {
            throw new InvalidGameActionException("Supporter already played this turn");
        }
    }
}
