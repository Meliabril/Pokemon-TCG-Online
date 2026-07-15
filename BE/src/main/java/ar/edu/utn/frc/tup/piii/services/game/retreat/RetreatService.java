package ar.edu.utn.frc.tup.piii.services.game.retreat;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

public interface RetreatService {

    GameActionExecutionResult retreat(GameActionContext context);
}
