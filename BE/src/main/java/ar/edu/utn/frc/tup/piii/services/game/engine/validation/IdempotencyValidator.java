package ar.edu.utn.frc.tup.piii.services.game.engine.validation;

import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@Order(0)
public class IdempotencyValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        UUID clientActionId = context.request().clientActionId();
        if (clientActionId == null) {
            return;
        }

        Set<UUID> processedClientActionIds = context.currentState().actions().processedClientActionIds();
        if (processedClientActionIds != null && processedClientActionIds.contains(clientActionId)) {
            throw new InvalidGameActionException("Action already processed");
        }
    }
}
