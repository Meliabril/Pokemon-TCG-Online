package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

import java.util.List;
import java.util.UUID;

public interface AttackService {

    GameActionExecutionResult declareAttack(GameActionContext context);

    GameActionExecutionResult finishAttackTurn(
            GameActionContext context,
            UUID attackerUserId,
            UUID defenderUserId,
            int stateVersion,
            List<GameEventDto> events);
}
