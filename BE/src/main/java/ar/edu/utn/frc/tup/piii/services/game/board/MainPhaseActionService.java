package ar.edu.utn.frc.tup.piii.services.game.board;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

public interface MainPhaseActionService {

    GameActionExecutionResult playBasicPokemon(GameActionContext context);

    GameActionExecutionResult attachEnergy(GameActionContext context);
}
