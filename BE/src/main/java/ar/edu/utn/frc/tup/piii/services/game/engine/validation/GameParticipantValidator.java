package ar.edu.utn.frc.tup.piii.services.game.engine.validation;

import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Order(3)
public class GameParticipantValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.currentState() == null) {
            return;
        }

        UUID actorUserId = context.actorUserId();
        List<UUID> playerIds = context.currentState().playerIds();
        boolean participantFound = false;

        for (UUID playerId : playerIds) {
            if (actorUserId != null && actorUserId.equals(playerId)) {
                participantFound = true;
                break;
            }
        }

        if (!participantFound) {
            throw new ForbiddenActionException("The acting user is not a participant of this game.");
        }
    }
}
