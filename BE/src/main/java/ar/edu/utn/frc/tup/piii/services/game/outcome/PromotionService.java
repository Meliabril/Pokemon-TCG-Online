package ar.edu.utn.frc.tup.piii.services.game.outcome;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

public interface PromotionService {

    GameActionExecutionResult promoteBenchPokemon(GameActionContext context);
}
