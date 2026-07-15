package ar.edu.utn.frc.tup.piii.services.game.evolution;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

public interface EvolutionService {

    GameActionExecutionResult evolve(GameActionContext context);
}
