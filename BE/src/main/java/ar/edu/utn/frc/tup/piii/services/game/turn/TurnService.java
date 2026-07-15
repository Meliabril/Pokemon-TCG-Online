package ar.edu.utn.frc.tup.piii.services.game.turn;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

public interface TurnService {

    GameActionExecutionResult drawCard(GameActionContext context);

    GameActionExecutionResult endTurn(GameActionContext context);

    GameActionExecutionResult expireTimedOutTurn(GameActionContext context);
}
