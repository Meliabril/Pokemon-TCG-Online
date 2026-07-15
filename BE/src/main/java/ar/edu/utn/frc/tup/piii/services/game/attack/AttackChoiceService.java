package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

public interface AttackChoiceService {

    GameActionExecutionResult resolveAttackChoice(GameActionContext context);

    GameActionExecutionResult resolveAttackChoiceForTimeout(GameActionContext context);
}
