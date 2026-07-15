package ar.edu.utn.frc.tup.piii.services.game.ability;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

public interface AbilityService {

    GameActionExecutionResult useAbility(GameActionContext context);
}
